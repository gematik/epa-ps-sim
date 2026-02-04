/*-
 * #%L
 * epa-ps-sim-app
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
package de.gematik.epa.ps.information;

import de.gematik.epa.api.information.client.AccountInformationApi;
import de.gematik.epa.api.information.client.ConsentDecisionsApi;
import de.gematik.epa.api.information.client.UserExperienceApi;
import de.gematik.epa.client.JaxRsClientWrapper;
import de.gematik.epa.information.InformationService;
import de.gematik.epa.ps.config.EpaProxyConfiguration;
import de.gematik.epa.ps.information.config.InformationServerConfiguration;
import de.gematik.epa.ps.information.config.InformationServers;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@Accessors(fluent = true)
@EnableConfigurationProperties({InformationServers.class, EpaProxyConfiguration.class})
@RequiredArgsConstructor
public class InformationClientProvider {

  private final InformationServers informationServersConfiguration;
  private final EpaProxyConfiguration epaProxyConfiguration;

  String getServerUrl(final InformationServerConfiguration informationServerConfiguration) {
    return informationServerConfiguration.getProtocol()
        + "://"
        + informationServerConfiguration.getHost()
        + ":"
        + informationServerConfiguration.getPort();
  }

  @Bean
  public InformationService informationService() {
    var accountInformationClientWrapper =
        informationServersConfiguration.getServers().stream()
            .map(server -> this.createInformationClientWrapper(server, AccountInformationApi.class))
            .toList();
    var consentDecisionsClientWrapper =
        informationServersConfiguration.getServers().stream()
            .map(server -> this.createInformationClientWrapper(server, ConsentDecisionsApi.class))
            .toList();
    var userExperienceClientWrapper =
        informationServersConfiguration.getServers().stream()
            .map(server -> this.createInformationClientWrapper(server, UserExperienceApi.class))
            .toList();

    return new InformationService(
        accountInformationClientWrapper,
        consentDecisionsClientWrapper,
        userExperienceClientWrapper);
  }

  private <T> JaxRsClientWrapper<T> createInformationClientWrapper(
      InformationServerConfiguration informationServerConfiguration, Class<T> apiClass) {
    return new JaxRsClientWrapper<>(
            getServerUrl(informationServerConfiguration),
            informationServerConfiguration.getUserAgent(),
            apiClass,
            epaProxyConfiguration.getHost(),
            epaProxyConfiguration.getPort())
        .makeInsecureTlsClient();
  }
}
