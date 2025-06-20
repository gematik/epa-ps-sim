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
package de.gematik.epa.entitlement.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import de.gematik.epa.api.entitlement.client.EntitlementsApi;
import de.gematik.epa.api.entitlement.client.UserBlockingApi;
import de.gematik.epa.api.information.client.AccountInformationApi;
import de.gematik.epa.api.testdriver.information.InformationApi;
import de.gematik.epa.client.JaxRsClientWrapper;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.jupiter.api.Test;

class JaxRsClientWrapperTest {

  private final String serverUrl = "http://localhost:8080";
  private final JaxRsClientWrapper<EntitlementsApi> entitlementClientWrapper =
      new JaxRsClientWrapper<>(serverUrl, "UserAgent", EntitlementsApi.class);
  private final JaxRsClientWrapper<AccountInformationApi> informationClientWrapper =
      new JaxRsClientWrapper<>(serverUrl, "UserAgent", AccountInformationApi.class);

  private final JaxRsClientWrapper<UserBlockingApi> blockingClientWrapper =
      new JaxRsClientWrapper<>(serverUrl, "UserAgent", UserBlockingApi.class);

  @Test
  void shouldInitializeAccountInformationApi() {
    assertThat(informationClientWrapper.makeInsecureTlsClient().getServiceApi()).isNotNull();
  }

  @Test
  void shouldInitializeEntitlementsApi() {
    assertThat(entitlementClientWrapper.getServiceApi()).isNotNull();
  }

  @Test
  void shouldInitializeUserBlockingApi() {
    assertThat(blockingClientWrapper.getServiceApi()).isNotNull();
  }

  @Test
  void shouldHandleNullServerUrl() {
    final Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new JaxRsClientWrapper<>(null, "UserAgent", EntitlementsApi.class));

    var actualMessage = exception.getMessage();
    assertThat(actualMessage).isEqualTo(JaxRsClientWrapper.ERROR_MESSAGE);
  }

  @Test
  void shouldHandleEmptyServerUrl() {
    final JaxRsClientWrapper<EntitlementsApi> wrapper =
        new JaxRsClientWrapper<>("", "", EntitlementsApi.class);
    final EntitlementsApi api = wrapper.getServiceApi();
    assertThat(api).isNotNull();
  }

  @Test
  void shouldCreateClientWrapperWithProxy() {
    var proxyHost = "http://proxy.de";
    var port = "8080";
    final JaxRsClientWrapper<InformationApi> wrapper =
        new JaxRsClientWrapper<>(
            "http://epa-1.de", "ps-sim", InformationApi.class, proxyHost, port);
    final InformationApi api = wrapper.getServiceApi();
    assertThat(api).isNotNull();
    assertThat(WebClient.getConfig(api).getHttpConduit().getClient().getProxyServer())
        .isEqualTo(proxyHost);
    assertThat(WebClient.getConfig(api).getHttpConduit().getClient().getProxyServerPort())
        .isEqualTo(Integer.parseInt(port));
  }
}
