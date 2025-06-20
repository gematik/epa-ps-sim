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
package de.gematik.epa.api.testdriver.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.epa.api.testdriver.medication.dto.*;
import de.gematik.epa.fhir.client.FhirClient;
import de.gematik.epa.medication.MedicationService;
import de.gematik.epa.medication.MedicationsSearch;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MedicationApiImplTest {

  private final FhirClient fhirClient = mock(FhirClient.class);
  private final FhirContext fhirContext = mock(FhirContext.class);
  private MedicationApiImpl medicationApi;
  private MedicationService medicationService;

  @BeforeEach
  public void setup() {
    when(fhirClient.getContext()).thenReturn(fhirContext);
    medicationService = mock(MedicationService.class);
    medicationApi = new MedicationApiImpl(medicationService);
  }

  @Test
  void shouldGetMedicationById() {
    // given
    final String medication = "{\n" + "  \"resourceType\": \"Medication\"}";
    final GetMedicationResponseDTO response =
        new GetMedicationResponseDTO().medications(List.of(medication)).success(true);
    when(medicationService.executeGetById("id")).thenReturn(response);

    // when
    final GetMedicationResponseDTO expectedResponse =
        medicationApi.getMedicationList(
            null, null, null, 10, 1, null, "id", null, null, null, null, null, null);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedications()).hasSize(1);
    assertThat(response.getStatusMessage()).isBlank();
    assertThat(response).isEqualTo(expectedResponse);
  }

  @Test
  void shouldSearchForMedication() {
    // given
    final String medication = "{\n" + "  \"resourceType\": \"Medication\"}";
    final GetMedicationResponseDTO expectedResponse =
        new GetMedicationResponseDTO().medications(List.of(medication)).success(true);
    when(medicationService.searchMedications(any())).thenReturn(expectedResponse);

    // when
    final GetMedicationResponseDTO response =
        medicationApi.getMedicationList(
            "recordId", null, null, null, null, null, null, null, null, null, null, null, null);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedications()).hasSize(1);
    assertThat(response.getStatusMessage()).isBlank();
    assertThat(response).isEqualTo(expectedResponse);
  }

  @Test
  void shouldGetMedicationListAsPdf() {
    // given
    final byte[] pdf = new byte[0];
    when(medicationService.getEmlAsPdf("recordId"))
        .thenReturn(new GetMedicationListAsPdfResponseDTO().eml(pdf).success(true));

    // when
    var response = medicationApi.getMedicationListAsPdf("recordId");

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getEml()).isEqualTo(pdf);
  }

  @Test
  void shouldReturnNoSuccessForNullPdf() {
    // given
    when(medicationService.getEmlAsPdf("recordId"))
        .thenReturn(new GetMedicationListAsPdfResponseDTO().eml(null).success(false));

    // when
    var response = medicationApi.getMedicationListAsPdf("recordId");

    // then
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getEml()).isNull();
  }

  @Test
  void shouldGetMedicationListAsXhtml() {
    // given
    final String xhtml = "<html><body>test</body></html>";
    when(medicationService.getEmlAsXhtml("recordId"))
        .thenReturn(new GetMedicationListAsXhtmlResponseDTO().success(true).eml(xhtml));

    // when
    var response = medicationApi.getMedicationListAsXhtml("recordId");

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getEml()).isEqualTo(xhtml);
  }

  @Test
  void shouldReturnNoSuccessForNullXhtml() {
    // given
    when(medicationService.getEmlAsXhtml("recordId"))
        .thenReturn(new GetMedicationListAsXhtmlResponseDTO().eml(null).success(false));

    // when
    var response = medicationApi.getMedicationListAsXhtml("recordId");

    // then
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getEml()).isNull();
  }

  @Test
  void shouldReturnGetMedicationRequestById() {
    // given
    final String medicationRequest = "{\n" + "  \"resourceType\": \"MedicationRequest\"}";
    when(medicationService.getMedicationRequestById("123"))
        .thenReturn(
            new GetMedicationRequestListDTO()
                .medicationRequests(List.of(medicationRequest))
                .success(true));

    // when
    final GetMedicationRequestListDTO response =
        medicationApi.getMedicationRequests(
            null, null, null, 1, null, null, "123", null, null, null, null, null, null, null);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedicationRequests()).hasSize(1);
    assertThat(response.getStatusMessage()).isBlank();
  }

  @Test
  void shouldSearchForMedicationRequests() {
    // given
    when(medicationService.searchMedicationRequests(any()))
        .thenReturn(
            new GetMedicationRequestListDTO()
                .medicationRequests(List.of("{\n" + "  \"resourceType\": \"MedicationRequest\"}"))
                .success(true));

    // when
    final GetMedicationRequestListDTO response =
        medicationApi.getMedicationRequests(
            null, null, null, 1, 1, null, null, null, null, null, null, null, "active", null);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedicationRequests()).hasSize(1);
    assertThat(response.getStatusMessage()).isBlank();
  }

  @Test
  void shouldReturnGetMedicationDispenseById() {
    // given
    final String medicationDispense = "{\n" + "  \"resourceType\": \"MedicationDispense\"}";
    when(medicationService.getMedicationDispenseById("123"))
        .thenReturn(
            new GetMedicationDispenseListDTO()
                .medicationDispenses(List.of(medicationDispense))
                .success(true));

    // when
    final GetMedicationDispenseListDTO response =
        medicationApi.getMedicationDispenses(
            null, null, null, 1, null, null, "123", null, null, null, null, null, null, null, null);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedicationDispenses()).hasSize(1);
    assertThat(response.getStatusMessage()).isBlank();
  }

  @Test
  void shouldSearchForMedicationDispenses() {
    // given
    when(medicationService.searchMedicationDispenses(any()))
        .thenReturn(
            new GetMedicationDispenseListDTO()
                .medicationDispenses(List.of("{\n" + "  \"resourceType\": \"MedicationDispense\"}"))
                .success(true));

    // when
    final GetMedicationDispenseListDTO response =
        medicationApi.getMedicationDispenses(
            null,
            null,
            null,
            1,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "completed");

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedicationDispenses()).hasSize(1);
    assertThat(response.getStatusMessage()).isBlank();
  }

  @Test
  void shouldGetMedicationListAsFhir() {
    // given
    final GetMedicationListAsFhirResponseDTO expectedResponse =
        new GetMedicationListAsFhirResponseDTO().success(true);
    when(medicationService.searchForEmlAsFhir(any(MedicationsSearch.class)))
        .thenReturn(expectedResponse);

    // when
    final GetMedicationListAsFhirResponseDTO response =
        medicationApi.getMedicationListAsFhir(
            "insurantId", "useragent", 10, 0, "total", "active", null);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getStatusMessage()).isBlank();
  }

  @Test
  void shouldGetEmlAsFhirSuccessfully() {
    // given
    final GetEmlAsFhirResponseDTO expectedResponse = new GetEmlAsFhirResponseDTO().success(true);
    when(medicationService.getEmlAsFhir(
            any(UUID.class), anyString(), anyString(), anyInt(), anyInt()))
        .thenReturn(expectedResponse);

    // when
    final GetEmlAsFhirResponseDTO response =
        medicationApi.getEmlAsFhir("insurantId", UUID.randomUUID(), "ge2023-01-01", 10, 0);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getStatusMessage()).isBlank();
  }
}
