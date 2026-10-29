/*-
 * #%L
 * epa-ps-sim-lib
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.epa.client;

import static de.gematik.epa.utils.MiscUtils.*;

import de.gematik.epa.utils.HealthRecordProvider;
import de.gematik.epa.utils.InsurantIdHolder;
import de.gematik.epa.utils.TelematikIdHolder;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

@Slf4j
public class JaxRsOutgoingRequestInterceptor extends AbstractPhaseInterceptor<Message> {

  public JaxRsOutgoingRequestInterceptor() {
    super(Phase.PRE_PROTOCOL);
  }

  @Override
  public void handleMessage(Message message) throws Fault {
    // first check if there is an insurantId found inside the request context
    String insurantId = InsurantIdHolder.getInsurantId();
    final Map<String, List<String>> headers =
        CastUtils.cast((Map) message.get(Message.PROTOCOL_HEADERS));

    final String targetEndpoint = (String) message.get(Message.ENDPOINT_ADDRESS);
    final boolean isIgnoredEndpoint =
        ENDPOINTS_TO_IGNORE.stream().anyMatch(targetEndpoint::contains)
            || ENDPOINTS_TO_IGNORE_FOR_FQDN.stream().anyMatch(targetEndpoint::contains);
    final boolean allowsTelematikIdOnly =
        ENDPOINTS_ALLOWING_TELEMATIKID_ONLY.stream().anyMatch(targetEndpoint::contains);

    // if there is no insurantId in the request context check the outgoing request
    if (insurantId == null && headers != null) {
      insurantId = findInsurantId(headers);
    }

    if (headers != null) {
      deduplicateAcceptHeader(headers);
    }

    final var telematikId = TelematikIdHolder.getTelematikId();

    applyRoutingHeader(
        insurantId, headers, targetEndpoint, isIgnoredEndpoint, allowsTelematikIdOnly, telematikId);
    applyActorIdHeader(telematikId, headers, targetEndpoint, isIgnoredEndpoint);
  }

  /**
   * Adds the {@code x-target-fqdn} routing header for the VAU-Proxy, or blocks the request when no
   * routing target can be determined.
   *
   * <p>The endpoint pointing to the information service is excluded from this health record
   * localisation logic entirely (ignored endpoint). Login-flow endpoints
   * (getNonce/send_authorization_request_sc/send_authcode_sc) are called before a health record can
   * be resolved; a telematikId alone is sufficient identification there, so the request is allowed
   * through without {@code x-target-fqdn}. Any other request without a resolvable health record
   * must not be sent, since it could not be routed to the correct VAU-Proxy target.
   */
  private void applyRoutingHeader(
      final String insurantId,
      final Map<String, List<String>> headers,
      final String targetEndpoint,
      final boolean isIgnoredEndpoint,
      final boolean allowsTelematikIdOnly,
      final String telematikId) {

    if (isIgnoredEndpoint) {
      log.debug("Skipping insurantId check for ignored endpoint [{}]", targetEndpoint);
      return;
    }

    final boolean fqdnResolvable =
        insurantId != null && HealthRecordProvider.hasHealthRecord(insurantId);
    if (fqdnResolvable && headers != null) {
      // if fqdn ist resolvable, add it to the request headers so that the VAU-Proxy can route
      // the request
      headers.put(X_TARGET_FQDN, List.of(HealthRecordProvider.getHealthRecordUrl(insurantId)));
      return;
    }

    if (allowsTelematikIdOnly && telematikId != null) {
      log.debug(
          "Request allowed because it is a login-flow"
              + " endpoint and a telematikId is present at targetEndpoint [{}].",
          targetEndpoint);
      return;
    }

    // no insurantId/health record identifies this call, and it is not a login-flow endpoint
    // that would accept a telematikId alone -> there is no routing context, the request must
    // not be sent.
    throw blockUnroutableRequest(targetEndpoint, insurantId);
  }

  private static Fault blockUnroutableRequest(
      final String targetEndpoint, final String insurantId) {
    final String warning =
        "Blocking outbound request to ["
            + targetEndpoint
            + "]: "
            + X_TARGET_FQDN
            + " header is missing (insurantId="
            + insurantId
            + "). The request will NOT be sent.";
    log.warn(warning);
    return new Fault(new IllegalStateException(warning));
  }

  private void applyActorIdHeader(
      final String telematikId,
      final Map<String, List<String>> headers,
      final String targetEndpoint,
      final boolean isIgnoredEndpoint) {
    if (telematikId != null && headers != null) {
      headers.put(X_ACTOR_ID, List.of(telematikId));
    } else if (isIgnoredEndpoint) {
      log.debug("Skipping telematikId warning for ignored endpoint [{}]", targetEndpoint);
    } else {
      log.warn(
          "Outbound request to [{}]: {} header is missing - no telematikId available.",
          targetEndpoint,
          X_ACTOR_ID);
    }
  }

  /**
   * Resets accumulated duplicate entries in the Accept header by modifying the value list in-place.
   *
   * <p>Root cause: {@code AbstractClient.getHeaders()} creates a {@code MetadataMap(copy=false)}
   * whose value lists are shared by reference with {@code state.getRequestHeaders()}. Every call to
   * a proxy method that carries a {@code @HeaderParam("Accept")} parameter therefore mutates the
   * persistent shared state list via {@code headers.addAll("Accept", ...)}, causing unbounded
   * growth across invocations.
   *
   * <p>Calling {@code values.clear()} followed by {@code values.addAll(deduped)} modifies the
   * shared list object in-place, which simultaneously fixes both the outgoing message's {@code
   * PROTOCOL_HEADERS} and the underlying {@code state.getRequestHeaders()}, leaving the state clean
   * for the next invocation.
   */
  private void deduplicateAcceptHeader(Map<String, List<String>> headers) {
    headers.entrySet().stream()
        .filter(e -> "Accept".equalsIgnoreCase(e.getKey()))
        .filter(e -> e.getValue() != null && e.getValue().size() > 1)
        .forEach(
            e -> {
              List<String> values = e.getValue();
              List<String> deduped = values.stream().distinct().toList();
              log.debug(
                  "Resetting Accept header from {} to {} distinct entries",
                  values.size(),
                  deduped.size());
              // In-place modification: clears the shared list and repopulates with distinct values.
              // This fixes both the outgoing message and the persistent WebClient state.
              values.clear();
              values.addAll(deduped);
            });
  }
}
