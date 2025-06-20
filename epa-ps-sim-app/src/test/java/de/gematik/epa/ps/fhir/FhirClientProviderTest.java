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
package de.gematik.epa.ps.fhir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.epa.ps.fhir.config.AuditServerConfiguration;
import de.gematik.epa.ps.fhir.config.FhirServerConfiguration;
import org.junit.jupiter.api.Test;

class FhirClientProviderTest {

  public static final String EXPECTED_URL = "http://localhost:8080/fhir";
  private final FhirServerConfiguration fhirServerConfiguration =
      mock(FhirServerConfiguration.class);
  private final AuditServerConfiguration auditServerConfiguration =
      mock(AuditServerConfiguration.class);
  private final FhirClientProvider fhirClientProvider =
      new FhirClientProvider(fhirServerConfiguration, auditServerConfiguration);

  @Test
  void shouldCreateFhirServerUrl() {
    mockFhirServerConfigAccess();

    assertThat(fhirClientProvider.getFhirServerUrl()).isEqualTo(EXPECTED_URL);
  }

  @Test
  void shouldCreateAuditServerUrl() {
    mockAuditServerConfigAccess();

    assertThat(fhirClientProvider.getAuditServerUrl()).isEqualTo(EXPECTED_URL);
  }

  @Test
  void shouldCreateFhirClientWithServerUrl() {
    mockFhirServerConfigAccess();

    final var fhirClient = fhirClientProvider.fhirClient();
    assertThat(fhirClient.getServerUrl()).isEqualTo(EXPECTED_URL);
  }

  @Test
  void shouldCreateAuditClientWithServerUrl() {
    mockAuditServerConfigAccess();

    final var auditFhirClient = fhirClientProvider.auditFhirClient();
    assertThat(auditFhirClient.getServerUrl()).isEqualTo(EXPECTED_URL);
  }

  private void mockFhirServerConfigAccess() {
    when(fhirServerConfiguration.getProtocol()).thenReturn("http");
    when(fhirServerConfiguration.getHost()).thenReturn("localhost");
    when(fhirServerConfiguration.getPort()).thenReturn("8080");
    when(fhirServerConfiguration.getPath()).thenReturn("fhir");
  }

  private void mockAuditServerConfigAccess() {
    when(auditServerConfiguration.getProtocol()).thenReturn("http");
    when(auditServerConfiguration.getHost()).thenReturn("localhost");
    when(auditServerConfiguration.getPort()).thenReturn("8080");
    when(auditServerConfiguration.getPath()).thenReturn("fhir");
  }
}
