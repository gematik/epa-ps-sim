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

import de.gematik.epa.config.AppConfig;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JaxRsOutgoingXDSRequestInterceptor extends AbstractPhaseInterceptor<Message> {
  private final AppConfig appConfig;

  @Autowired
  public JaxRsOutgoingXDSRequestInterceptor(AppConfig appConfig) {
    super(Phase.PRE_PROTOCOL);
    this.appConfig = appConfig;
  }

  @Override
  public void handleMessage(Message message) throws Fault {
    final Map<String, List<String>> headers =
        CastUtils.cast((Map) message.get(Message.PROTOCOL_HEADERS));
    if (headers != null) {
      String insurantId = InsurantIdHolder.getInsurantId();
      String userAgent = appConfig.getUserAgent();
      String telematikId = TelematikIdHolder.getTelematikId();

      if (insurantId != null) {
        headers.put(X_INSURANT_ID, List.of(insurantId));
      } else {
        log.warn(
            "No insurantId found in message exchange for: {}",
            message.get(Message.ENDPOINT_ADDRESS));
      }
      headers.put(X_USER_AGENT, List.of(userAgent));

      if (telematikId != null) {
        headers.put(X_ACTOR_ID, List.of(telematikId));
      } else {
        log.warn(
            "No telematikId found in message exchange for: {}",
            message.get(Message.ENDPOINT_ADDRESS));
      }
    }
  }
}
