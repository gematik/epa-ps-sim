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
package de.gematik.epa.ps.fhir.config;

import de.gematik.epa.config.AppConfig;
import de.gematik.epa.fhir.client.FhirClient;
import de.gematik.epa.ps.config.ServerConfiguration;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@Slf4j
@Accessors(fluent = true)
@Profile("test")
@ComponentScan("de.gematik.epa.fhir")
@EnableConfigurationProperties({AppConfig.class})
@AllArgsConstructor
public class TestFhirClientProvider {

  private final AppConfig appConfiguration;

  @Bean
  public ServerConfiguration fhirServerConfiguration() {
    return new ServerConfiguration();
  }

  @Bean
  public FhirClient fhirClient() {
    return new FhirClient(fhirServerConfiguration().buildUrl(), appConfiguration.getUserAgent());
  }
}
