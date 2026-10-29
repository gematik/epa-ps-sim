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

import static de.gematik.epa.utils.MiscUtils.ENDPOINTS_TO_IGNORE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import de.gematik.epa.api.information.client.AccountInformationApi;
import de.gematik.epa.utils.HealthRecordProvider;
import de.gematik.epa.utils.InsurantIdHolder;
import de.gematik.epa.utils.MiscUtils;
import de.gematik.epa.utils.TelematikIdHolder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JaxRsOutgoingRequestInterceptorTest {

  private static final String VAU_STATUS_ENDPOINT = "http://vau-proxy:8080/VAU-Status";
  private static final String LOGIN_FLOW_ENDPOINT = "http://vau-proxy:8080/epa/authz/v1/getNonce";

  private final Message message = mock(Message.class);
  private final JaxRsClientWrapper client =
      new JaxRsClientWrapper<>("http://localhost:8080", "PS-SIM", AccountInformationApi.class);

  private final JaxRsOutgoingRequestInterceptor interceptor = new JaxRsOutgoingRequestInterceptor();

  @BeforeEach
  void setUp() {
    InsurantIdHolder.clear();
    TelematikIdHolder.clearTelematikId();
  }

  @AfterEach
  void tearDown() {
    InsurantIdHolder.clear();
    TelematikIdHolder.clearTelematikId();
    HealthRecordProvider.getAllHealthRecords()
        .forEach((key, value) -> HealthRecordProvider.clearHealthRecord(key));
  }

  @Test
  void shouldAddInsurantIdToHeadersWhenPresent() {
    final Map<String, List<String>> headers = new HashMap<>();
    when(message.get(Message.PROTOCOL_HEADERS)).thenReturn(headers);
    when(message.get(Message.ENDPOINT_ADDRESS)).thenReturn("someEndpoint");

    var insurantId = "12345";
    InsurantIdHolder.setInsurantId(insurantId);
    HealthRecordProvider.addHealthRecord(insurantId, client.getUrl());
    interceptor.handleMessage(message);

    assertThat(headers).containsKey(MiscUtils.X_TARGET_FQDN);
  }

  @Test
  void shouldNotAddActorIdHeaderWhenTelematikIdIsMissingOnNonIgnoredEndpoint() {
    // covers the warning branch of applyActorIdHeader: routable request (insurantId/health
    // record resolvable), but no telematikId available -> x-actorId must not be set
    final Map<String, List<String>> headers = new HashMap<>();
    when(message.get(Message.PROTOCOL_HEADERS)).thenReturn(headers);
    when(message.get(Message.ENDPOINT_ADDRESS)).thenReturn("someEndpoint");

    var insurantId = "12345";
    InsurantIdHolder.setInsurantId(insurantId);
    HealthRecordProvider.addHealthRecord(insurantId, client.getUrl());
    TelematikIdHolder.setTelematikId(null);

    interceptor.handleMessage(message);

    assertThat(headers).containsKey(MiscUtils.X_TARGET_FQDN);
    assertThat(headers).doesNotContainKey("x-actorId");
  }

  @Test
  void shouldBlockVauStatusRequestWhenNeitherInsurantIdNorTelematikIdIsPresent() {
    final Map<String, List<String>> headers = new HashMap<>();
    when(message.get(Message.PROTOCOL_HEADERS)).thenReturn(headers);
    when(message.get(Message.ENDPOINT_ADDRESS)).thenReturn(VAU_STATUS_ENDPOINT);

    InsurantIdHolder.setInsurantId(null);

    assertThatThrownBy(() -> interceptor.handleMessage(message))
        .isInstanceOf(Fault.class)
        .cause()
        .hasMessageContaining(MiscUtils.X_TARGET_FQDN)
        .hasMessageContaining(VAU_STATUS_ENDPOINT);
    assertThat(headers.containsKey(MiscUtils.X_TARGET_FQDN)).isFalse();
  }

  @Test
  void shouldBlockVauStatusRequestWithoutInsurantIdEvenWhenTelematikIdIsPresent() {
    final Map<String, List<String>> headers = new HashMap<>();
    when(message.get(Message.PROTOCOL_HEADERS)).thenReturn(headers);
    when(message.get(Message.ENDPOINT_ADDRESS)).thenReturn(VAU_STATUS_ENDPOINT);

    InsurantIdHolder.setInsurantId(null);
    TelematikIdHolder.setTelematikId("1-SMC-B-Testkarte--8831100001639442");

    assertThatThrownBy(() -> interceptor.handleMessage(message)).isInstanceOf(Fault.class);
    assertThat(headers.containsKey(MiscUtils.X_TARGET_FQDN)).isFalse();
  }

  @Test
  void shouldAddRoutingHeaderToVauStatusRequestWhenHealthRecordIsResolvable() {
    final Map<String, List<String>> headers = new HashMap<>();
    when(message.get(Message.PROTOCOL_HEADERS)).thenReturn(headers);
    when(message.get(Message.ENDPOINT_ADDRESS)).thenReturn(VAU_STATUS_ENDPOINT);

    var insurantId = "12345";
    InsurantIdHolder.setInsurantId(insurantId);
    HealthRecordProvider.addHealthRecord(insurantId, client.getUrl());

    interceptor.handleMessage(message);

    assertThat(headers).containsEntry(MiscUtils.X_TARGET_FQDN, List.of(client.getUrl()));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        VAU_STATUS_ENDPOINT,
        "http://vau-proxy:8080/epa/basic/api/v1/ps/entitlements",
        "http://vau-proxy:8080/epa/basic/api/v2/ps/entitlements",
        "http://vau-proxy:8080/epa/basic/api/v1/blockedusers/1-123",
        "http://vau-proxy:8080/epa/basic/api/v1/consents",
        "http://vau-proxy:8080/epa/basic/api/v1/emailaddress",
        "http://vau-proxy:8080/epa/mhd/api/v1/fhir/DocumentReference/_search",
        "http://vau-proxy:8080/epa/mhd/retrieve/v1/content/abc.pdf",
        "http://vau-proxy:8080/epa/medication/api/v1/fhir/$medication-list",
        "http://vau-proxy:8080/epa/medication/render/v1/eml/pdf",
        "http://vau-proxy:8080/epa/audit/api/v1/fhir/AuditEvent",
        "http://vau-proxy:8080/epa/audit/render/v1/pdf"
      })
  void shouldBlockHealthRecordRequestWithoutResolvableHealthRecord(final String endpoint) {
    final Map<String, List<String>> headers = new HashMap<>();
    headers.put(MiscUtils.X_INSURANT_ID, List.of("99999"));
    when(message.get(Message.PROTOCOL_HEADERS)).thenReturn(headers);
    when(message.get(Message.ENDPOINT_ADDRESS)).thenReturn(endpoint);

    assertThatThrownBy(() -> interceptor.handleMessage(message))
        .isInstanceOf(Fault.class)
        .cause()
        .hasMessageContaining(endpoint)
        .hasMessageContaining("99999");
    assertThat(headers.containsKey(MiscUtils.X_TARGET_FQDN)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://vau-proxy:8080/epa/authz/v1/freshness",
        "http://vau-proxy:8080/epa/vzd/v1/vzdToken",
        "http://vau-proxy:8080/destroy",
        "http://vau-proxy:8080/epa/mhd/setup",
        "http://vau-proxy:8080/epa/mhd/cleanup"
      })
  void shouldBlockNonIgnoredRequestWithoutResolvableHealthRecord(final String endpoint) {
    final Map<String, List<String>> headers = new HashMap<>();
    when(message.get(Message.PROTOCOL_HEADERS)).thenReturn(headers);
    when(message.get(Message.ENDPOINT_ADDRESS)).thenReturn(endpoint);

    assertThatThrownBy(() -> interceptor.handleMessage(message))
        .isInstanceOf(Fault.class)
        .cause()
        .hasMessageContaining(endpoint)
        .hasMessageContaining(MiscUtils.X_TARGET_FQDN);
    assertThat(headers).doesNotContainKey(MiscUtils.X_TARGET_FQDN);
  }

  @Test
  void shouldNotBlockPoppTokenClientRequestWithoutInsurantId() {
    final Map<String, List<String>> headers = new HashMap<>();
    when(message.get(Message.PROTOCOL_HEADERS)).thenReturn(headers);
    when(message.get(Message.ENDPOINT_ADDRESS)).thenReturn("http://popp-token-client:9091/token");

    InsurantIdHolder.setInsurantId(null);
    TelematikIdHolder.setTelematikId("1-SMC-B-Testkarte--8831100001639442");

    interceptor.handleMessage(message);

    assertThat(headers.containsKey(MiscUtils.X_TARGET_FQDN)).isFalse();
    assertThat(headers).containsKey("x-actorId");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://vau-proxy:8080/epa/authz/v1/getNonce",
        "http://vau-proxy:8080/epa/authz/v1/send_authorization_request_sc",
        "http://vau-proxy:8080/epa/authz/v1/send_authcode_sc"
      })
  void shouldNotBlockLoginFlowRequestWithoutInsurantId(final String endpoint) {
    final Map<String, List<String>> headers = new HashMap<>();
    when(message.get(Message.PROTOCOL_HEADERS)).thenReturn(headers);
    when(message.get(Message.ENDPOINT_ADDRESS)).thenReturn(endpoint);

    InsurantIdHolder.setInsurantId(null);
    TelematikIdHolder.setTelematikId("1-SMC-B-Testkarte--8831100001639442");

    interceptor.handleMessage(message);

    assertThat(headers)
        .doesNotContainKey(MiscUtils.X_TARGET_FQDN)
        .containsKey(MiscUtils.X_ACTOR_ID);
  }

  @Test
  void shouldBlockLoginFlowRequestWithoutInsurantIdOrTelematikId() {
    final Map<String, List<String>> headers = new HashMap<>();
    when(message.get(Message.PROTOCOL_HEADERS)).thenReturn(headers);
    when(message.get(Message.ENDPOINT_ADDRESS)).thenReturn(LOGIN_FLOW_ENDPOINT);

    InsurantIdHolder.setInsurantId(null);
    TelematikIdHolder.setTelematikId(null);

    assertThatThrownBy(() -> interceptor.handleMessage(message))
        .isInstanceOf(Fault.class)
        .cause()
        .hasMessageContaining(LOGIN_FLOW_ENDPOINT);
    assertThat(headers).doesNotContainKey(MiscUtils.X_TARGET_FQDN);
  }

  @Test
  void shouldBlockNonIgnoredRequestWhenNoHealthRecordIsRegisteredForInsurantId() {
    final Map<String, List<String>> headers = new HashMap<>();
    when(message.get(Message.PROTOCOL_HEADERS)).thenReturn(headers);
    when(message.get(Message.ENDPOINT_ADDRESS)).thenReturn("someEndpoint");

    InsurantIdHolder.setInsurantId("99999");

    assertThatThrownBy(() -> interceptor.handleMessage(message))
        .isInstanceOf(Fault.class)
        .cause()
        .hasMessageContaining("99999")
        .hasMessageContaining(MiscUtils.X_TARGET_FQDN);

    assertThat(headers).doesNotContainKey(MiscUtils.X_TARGET_FQDN);
  }

  @Test
  void shouldNotAddInsurantIdToHeadersWhenEndpointIsIgnored() {
    final Map<String, List<String>> headers = new HashMap<>();
    when(message.get(Message.PROTOCOL_HEADERS)).thenReturn(headers);
    when(message.get(Message.ENDPOINT_ADDRESS)).thenReturn(ENDPOINTS_TO_IGNORE.getFirst());

    var insurantId = "12345";
    InsurantIdHolder.setInsurantId(insurantId);
    HealthRecordProvider.addHealthRecord(insurantId, client.getUrl());
    interceptor.handleMessage(message);

    assertThat(headers.containsKey(MiscUtils.X_TARGET_FQDN)).isFalse();
  }

  @Test
  void shouldNotBlockIgnoredEndpointWhenInsurantIdIsMissing() {
    final Map<String, List<String>> headers = new HashMap<>();
    when(message.get(Message.PROTOCOL_HEADERS)).thenReturn(headers);
    when(message.get(Message.ENDPOINT_ADDRESS)).thenReturn(ENDPOINTS_TO_IGNORE.getFirst());

    InsurantIdHolder.setInsurantId(null);

    interceptor.handleMessage(message);

    assertThat(headers.containsKey(MiscUtils.X_TARGET_FQDN)).isFalse();
  }

  @Test
  void shouldAddTelematikIdToHeadersWhenPresent() {
    final Map<String, List<String>> headers = new HashMap<>();
    when(message.get(Message.PROTOCOL_HEADERS)).thenReturn(headers);
    when(message.get(Message.ENDPOINT_ADDRESS)).thenReturn(LOGIN_FLOW_ENDPOINT);

    TelematikIdHolder.setTelematikId("1-SMC-B-Testkarte--8831100001639442");
    interceptor.handleMessage(message);
    assertThat(headers).containsKey("x-actorId");
  }
}
