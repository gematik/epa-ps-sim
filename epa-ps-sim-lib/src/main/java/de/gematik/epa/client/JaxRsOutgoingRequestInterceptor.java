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

    // if there is no insurantId in the request context check the outgoing request
    if (insurantId == null && headers != null) {
      insurantId = findInsurantId(headers);
    }

    // the endpoint pointing to the information service should be excluded from health record
    // localisation logic
    final String targetEndpoint = (String) message.get(Message.ENDPOINT_ADDRESS);
    if (insurantId != null && ENDPOINTS_TO_IGNORE.stream().noneMatch(targetEndpoint::contains)) {
      headers.put(X_TARGET_FQDN, List.of(HealthRecordProvider.getHealthRecordUrl(insurantId)));
    } else {
      log.warn("No insurantId found in message exchange for: {}", targetEndpoint);
    }

    var telematikId = TelematikIdHolder.getTelematikId();
    if (telematikId != null && headers != null) {
      headers.put(X_ACTOR_ID, List.of(telematikId));
    } else {
      log.warn("No telematikId found in message exchange for: {}", targetEndpoint);
    }
  }
}
