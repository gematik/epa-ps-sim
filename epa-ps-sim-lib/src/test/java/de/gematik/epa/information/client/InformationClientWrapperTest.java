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
package de.gematik.epa.information.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import de.gematik.epa.api.information.client.AccountInformationApi;
import de.gematik.epa.client.JaxRsClientWrapper;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.jupiter.api.Test;

class InformationClientWrapperTest {
  private final String serverUrl = "http://localhost:8080";
  private final String userAgent = "ps-sim";
  private final JaxRsClientWrapper<AccountInformationApi> informationClientWrapper =
      new JaxRsClientWrapper(serverUrl, userAgent, AccountInformationApi.class);

  @Test
  void shouldInitializeAccountInformationApi() {
    final AccountInformationApi api = informationClientWrapper.getServiceApi();
    assertThat(api).isNotNull();
  }

  @Test
  void shouldAcceptApplicationJson() {
    final AccountInformationApi api = informationClientWrapper.getServiceApi();
    final Client client = WebClient.client(api);
    assertNotNull(client);
    assertThat(client.getHeaders().getFirst("Accept")).isEqualTo("application/json");
  }

  @Test
  void shouldThrowExceptionWhenServerUrlIsNull() {
    final Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new JaxRsClientWrapper<>(null, userAgent, AccountInformationApi.class));

    var actualMessage = exception.getMessage();
    assertThat(actualMessage).isEqualTo(JaxRsClientWrapper.ERROR_MESSAGE);
  }

  @Test
  void shouldHandleEmptyServerUrl() {
    final JaxRsClientWrapper<AccountInformationApi> wrapper =
        new JaxRsClientWrapper<>("", "", AccountInformationApi.class);
    final AccountInformationApi api = wrapper.getServiceApi();
    assertThat(api).isNotNull();
  }
}
