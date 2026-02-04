/*-
 * #%L
 * epa-ps-sim-lib
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
package de.gematik.epa.fhir.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FhirClientTest {

  private final FhirContext fhirContext = mock(FhirContext.class);
  private final IGenericClient iGenericClient = mock(IGenericClient.class);

  @BeforeEach
  void setUp() {
    when(fhirContext.newRestfulGenericClient(anyString())).thenReturn(iGenericClient);
  }

  @Test
  void shouldCreateClientWithServerUrl() {
    // given + when
    var fhirClient = new FhirClient("http://localhost:8080/fhir", "PS-SIM");

    // then
    assertThat(fhirClient.getServerUrl()).isEqualTo("http://localhost:8080/fhir");
  }

  @Test
  void shouldUpdateServerUrlAfterCreation() {
    // given
    var fhirClient = new FhirClient("http://localhost:8080/fhir", "PS-SIM");

    // when
    var updatedServerUrl = "http://test:8080/fhir";
    var userAgent = "PS-SIM";
    fhirClient.setServerUrl(updatedServerUrl, userAgent);

    // then
    assertThat(updatedServerUrl).isEqualTo(fhirClient.getServerUrl());
  }
}
