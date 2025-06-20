/*-
 * #%L
 * epa-ps-sim-app
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
package de.gematik.epa.ps.idm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.epa.ps.config.EpaProxyConfiguration;
import de.gematik.epa.ps.idm.config.AuthServerConfiguration;
import de.gematik.epa.ps.idm.config.IdpServerConfiguration;
import de.gematik.epa.ps.kob.config.VauProxyConfiguration;
import kong.unirest.core.UnirestInstance;
import org.junit.jupiter.api.Test;

class AuthenticationClientProviderTest {

  private final IdpServerConfiguration idpServerConfiguration = mock(IdpServerConfiguration.class);
  private final EpaProxyConfiguration epaProxyConfiguration = mock(EpaProxyConfiguration.class);
  private final VauProxyConfiguration vauProxyConfiguration = mock(VauProxyConfiguration.class);
  private final AuthServerConfiguration authServerConfiguration =
      mock(AuthServerConfiguration.class);
  private final AuthenticationClientProvider authenticationClientProvider =
      new AuthenticationClientProvider(
          authServerConfiguration,
          idpServerConfiguration,
          epaProxyConfiguration,
          vauProxyConfiguration);

  @Test
  void shouldReturnCorrectAuthServerUrl() {
    when(authServerConfiguration.getProtocol()).thenReturn("https");
    when(authServerConfiguration.getHost()).thenReturn("auth.example.com");
    when(authServerConfiguration.getPort()).thenReturn("443");

    var url = authenticationClientProvider.getAuthServerUrl();
    assertThat(url).isEqualTo("https://auth.example.com:443");
  }

  @Test
  void shouldReturnCorrectIdmServerUrl() {
    when(idpServerConfiguration.getProtocol()).thenReturn("https");
    when(idpServerConfiguration.getHost()).thenReturn("idm.example.com");
    when(idpServerConfiguration.getPort()).thenReturn("443");
    when(idpServerConfiguration.getPath()).thenReturn("idm");

    var url = authenticationClientProvider.getIdmServerUrl();
    assertThat(url).isEqualTo("https://idm.example.com:443/idm");
  }

  @Test
  void shouldConfigureUnirestInstanceWithProxy() {
    when(epaProxyConfiguration.getHost()).thenReturn("proxy.example.com");
    when(epaProxyConfiguration.getPort()).thenReturn("8080");
    when(idpServerConfiguration.isVerifySsl()).thenReturn(true);
    when(idpServerConfiguration.isFollowRedirects()).thenReturn(true);

    UnirestInstance unirestInstance = authenticationClientProvider.getIdpUnirestInstance();

    assertThat(unirestInstance.config().getProxy()).isNotNull();
    assertThat(unirestInstance.config().getProxy().getHost()).isEqualTo("proxy.example.com");
    assertThat(unirestInstance.config().getProxy().getPort()).isEqualTo(8080);
  }

  @Test
  void shouldConfigureUnirestInstanceWithoutProxy() {
    when(epaProxyConfiguration.getHost()).thenReturn("");
    when(epaProxyConfiguration.getPort()).thenReturn("");
    when(idpServerConfiguration.isVerifySsl()).thenReturn(true);
    when(idpServerConfiguration.isFollowRedirects()).thenReturn(true);

    UnirestInstance unirestInstance = authenticationClientProvider.getIdpUnirestInstance();

    assertThat(unirestInstance.config().getProxy()).isNull();
  }

  @Test
  void shouldDisableSslVerification() {
    when(idpServerConfiguration.isVerifySsl()).thenReturn(false);
    when(idpServerConfiguration.isFollowRedirects()).thenReturn(true);

    UnirestInstance unirestInstance = authenticationClientProvider.getIdpUnirestInstance();

    assertThat(unirestInstance.config().isVerifySsl()).isFalse();
  }

  @Test
  void shouldEnableSslVerification() {
    when(idpServerConfiguration.isVerifySsl()).thenReturn(true);
    when(idpServerConfiguration.isFollowRedirects()).thenReturn(true);

    UnirestInstance unirestInstance = authenticationClientProvider.getIdpUnirestInstance();

    assertThat(unirestInstance.config().isVerifySsl()).isTrue();
  }
}
