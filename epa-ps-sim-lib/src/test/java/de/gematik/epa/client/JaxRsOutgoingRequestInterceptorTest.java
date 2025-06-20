/*-
 * #%L
 * epa-ps-sim-lib
 * %%
 * Copyright (C) 2025 gematik GmbH
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */
package de.gematik.epa.client;

import static de.gematik.epa.utils.MiscUtils.ENDPOINTS_TO_IGNORE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import de.gematik.epa.api.information.client.AccountInformationApi;
import de.gematik.epa.utils.HealthRecordProvider;
import de.gematik.epa.utils.InsurantIdHolder;
import de.gematik.epa.utils.MiscUtils;
import de.gematik.epa.utils.TelematikIdHolder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.cxf.message.Message;
import org.junit.jupiter.api.Test;

class JaxRsOutgoingRequestInterceptorTest {

  private final Message message = mock(Message.class);
  private final JaxRsClientWrapper client =
      new JaxRsClientWrapper<>("http://localhost:8080", "PS-SIM", AccountInformationApi.class);

  private final JaxRsOutgoingRequestInterceptor interceptor = new JaxRsOutgoingRequestInterceptor();

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
  void shouldNotAddInsurantIdToHeadersWhenNotPresent() {
    final Map<String, List<String>> headers = new HashMap<>();
    when(message.get(Message.PROTOCOL_HEADERS)).thenReturn(headers);
    when(message.get(Message.ENDPOINT_ADDRESS)).thenReturn("someEndpoint");

    InsurantIdHolder.setInsurantId(null);
    interceptor.handleMessage(message);

    assertThat(headers.containsKey(MiscUtils.X_TARGET_FQDN)).isFalse();
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
    interceptor.handleMessage(message);

    assertThat(headers.containsKey(MiscUtils.X_TARGET_FQDN)).isFalse();
  }

  @Test
  void shouldAddTelematikIdToHeadersWhenPresent() {
    final Map<String, List<String>> headers = new HashMap<>();
    when(message.get(Message.PROTOCOL_HEADERS)).thenReturn(headers);
    when(message.get(Message.ENDPOINT_ADDRESS)).thenReturn("someEndpoint");

    TelematikIdHolder.setTelematikId("1-SMC-B-Testkarte--8831100001639442");
    interceptor.handleMessage(message);
    assertThat(headers).containsKey("x-actorId");
    TelematikIdHolder.clearTelematikId();
  }
}
