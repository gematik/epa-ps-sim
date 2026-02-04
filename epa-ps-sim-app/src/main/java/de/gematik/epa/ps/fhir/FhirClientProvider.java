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
package de.gematik.epa.ps.fhir;

import de.gematik.epa.fhir.client.FhirClient;
import de.gematik.epa.ps.fhir.config.AuditServerConfiguration;
import de.gematik.epa.ps.fhir.config.FhirServerConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Slf4j
@Accessors(fluent = true)
@Profile("!test")
@EnableConfigurationProperties({FhirServerConfiguration.class, AuditServerConfiguration.class})
@RequiredArgsConstructor
public class FhirClientProvider {

  private final FhirServerConfiguration fhirServerConfiguration;
  private final AuditServerConfiguration auditServerConfiguration;

  String getFhirServerUrl() {
    return fhirServerConfiguration.getProtocol()
        + "://"
        + fhirServerConfiguration.getHost()
        + ":"
        + fhirServerConfiguration.getPort()
        + "/"
        + fhirServerConfiguration.getPath();
  }

  String getAuditServerUrl() {
    return auditServerConfiguration.getProtocol()
        + "://"
        + auditServerConfiguration.getHost()
        + ":"
        + auditServerConfiguration.getPort()
        + "/"
        + auditServerConfiguration.getPath();
  }

  @Bean
  public FhirClient fhirClient() {
    return new FhirClient(getFhirServerUrl(), fhirServerConfiguration.getUserAgent());
  }

  @Bean
  public FhirClient auditFhirClient() {
    return new FhirClient(getAuditServerUrl(), auditServerConfiguration.getUserAgent());
  }
}
