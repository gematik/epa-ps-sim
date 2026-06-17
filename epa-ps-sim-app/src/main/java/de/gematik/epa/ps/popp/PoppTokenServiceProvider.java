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
package de.gematik.epa.ps.popp;

import de.gematik.epa.api.poppToken.client.PoppTokenClientApi;
import de.gematik.epa.api.poppToken.client.PoppTokenGeneratorApi;
import de.gematik.epa.client.JaxRsClientWrapper;
import de.gematik.epa.config.AppConfig;
import de.gematik.epa.popp.PoppTokenClientService;
import de.gematik.epa.popp.PoppTokenGeneratorService;
import de.gematik.epa.ps.config.ServerConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@Accessors(fluent = true)
@EnableConfigurationProperties({AppConfig.class})
@RequiredArgsConstructor
public class PoppTokenServiceProvider {

  private final AppConfig appConfiguration;

  @Bean
  @ConfigurationProperties(prefix = "popp-token-generator")
  public ServerConfiguration poppTokenGeneratorConfiguration() {
    return new ServerConfiguration();
  }

  @Bean
  @ConfigurationProperties(prefix = "popp-token-client")
  public ServerConfiguration poppTokenClientConfiguration() {
    return new ServerConfiguration();
  }

  @Bean
  public PoppTokenGeneratorService poppTokenGenerator() {
    var clientWrapper =
        new JaxRsClientWrapper<>(
                poppTokenGeneratorConfiguration().buildUrl(),
                appConfiguration.getUserAgent(),
                PoppTokenGeneratorApi.class)
            .makeInsecureTlsClient();
    return new PoppTokenGeneratorService(clientWrapper);
  }

  @Bean
  public PoppTokenClientService poppTokenClient() {
    var clientWrapper =
        new JaxRsClientWrapper<>(
                poppTokenClientConfiguration().buildUrl(),
                appConfiguration.getUserAgent(),
                PoppTokenClientApi.class)
            .makeInsecureTlsClient();
    return new PoppTokenClientService(clientWrapper);
  }
}
