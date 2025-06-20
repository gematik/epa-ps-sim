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

import static de.gematik.epa.unit.util.TestDataFactory.USER_AGENT;
import static de.gematik.epa.unit.util.TestDataFactory.X_INSURANTID;
import static de.gematik.epa.unit.util.TestDataFactory.X_USERAGENT;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.epa.config.AppConfig;
import de.gematik.epa.utils.InsurantIdHolder;
import de.gematik.epa.utils.TelematikIdHolder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.cxf.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class JaxRsOutgoingXDSRequestInterceptorTest {

  private final Message message = mock(Message.class);

  @Mock private AppConfig appConfig;

  private JaxRsOutgoingXDSRequestInterceptor interceptor;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(appConfig.getUserAgent()).thenReturn(USER_AGENT);
    interceptor = new JaxRsOutgoingXDSRequestInterceptor(appConfig);
  }

  @Test
  void shouldAddUserAgentAndInsurantIdToHeadersWhenPresent() {

    final Map<String, List<String>> headers = new HashMap<>();
    when(message.get(Message.PROTOCOL_HEADERS)).thenReturn(headers);

    InsurantIdHolder.setInsurantId("X12345");
    interceptor.handleMessage(message);
    assertThat(headers).containsKey(X_INSURANTID);
    assertThat(headers).containsKey(X_USERAGENT);
  }

  @Test
  void shouldNotAddInsurantIdToHeadersWhenNotPresent() {
    final Map<String, List<String>> headers = new HashMap<>();
    when(message.get(Message.PROTOCOL_HEADERS)).thenReturn(headers);

    InsurantIdHolder.setInsurantId(null);
    interceptor.handleMessage(message);
    assertThat(headers).containsKey(X_USERAGENT);
    assertThat(headers.containsKey(X_INSURANTID)).isFalse();
  }

  @Test
  void shouldAddTelematikIdToHeadersWhenPresent() {

    final Map<String, List<String>> headers = new HashMap<>();
    when(message.get(Message.PROTOCOL_HEADERS)).thenReturn(headers);

    TelematikIdHolder.setTelematikId("1-SMC-B-Testkarte--883110000163972");
    interceptor.handleMessage(message);
    assertThat(headers).containsKey("x-actorId");
    TelematikIdHolder.clearTelematikId();
  }
}
