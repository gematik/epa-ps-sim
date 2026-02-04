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
package de.gematik.epa.medication;

import static de.gematik.epa.medication.MedicationService.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.SearchTotalModeEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.InvalidResponseException;
import ca.uhn.fhir.rest.gclient.*;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.epa.api.testdriver.medication.dto.*;
import de.gematik.epa.fhir.client.FhirClient;
import de.gematik.epa.medication.client.EmlRenderClient;
import de.gematik.epa.medication.client.RenderResponse;
import de.gematik.epa.unit.util.ResourceLoader;
import de.gematik.epa.utils.FhirUtils;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MedicationServiceTest {

  private static String medicationAsString;
  private static String medicationRequestAsString;
  private static String medicationDispenseAsString;
  private static String medicationListAsString;
  private final FhirClient fhirClient = mock(FhirClient.class);
  private final FhirContext context = mock(FhirContext.class);
  private final IParser jsonParser = mock(IParser.class);
  private final EmlRenderClient emlRenderClient = mock(EmlRenderClient.class);
  private final ObjectMapper objectMapper =
      new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
  private MedicationService medicationService;

  @SneakyThrows
  @BeforeAll
  static void setupOnce() {
    medicationAsString =
        ResourceLoader.readFileContentFromResource("src/test/resources/response/medication.json");

    medicationRequestAsString =
        ResourceLoader.readFileContentFromResource(
            "src/test/resources/response/medication-request.json");

    medicationDispenseAsString =
        ResourceLoader.readFileContentFromResource(
            "src/test/resources/response/medication-dispense.json");

    medicationListAsString =
        ResourceLoader.readFileContentFromResource(
            "src/test/resources/response/medication-list-fhir.json");
  }

  private static Medication getMedication() {
    final Medication medication = new Medication();
    medication.setId("123");
    return medication;
  }

  @BeforeEach
  void setup() {
    when(fhirClient.getContext()).thenReturn(context);
    when(context.newJsonParser()).thenReturn(jsonParser);
    FhirUtils.setJsonParser(jsonParser);
    medicationService = new MedicationService(fhirClient, emlRenderClient);
  }

  @Test
  void shouldGetMedicationById() {
    // given
    final Medication medication = getMedication();

    var id = "123";
    var iReadExecutable = mockFhirGetResourceById(Medication.class, id);
    when(iReadExecutable.execute()).thenReturn(medication);

    when(jsonParser.encodeResourceToString(medication)).thenReturn(medicationAsString);

    // when
    final GetMedicationResponseDTO response = medicationService.executeGetById(id);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedications()).hasSize(1);
    assertThat(response.getStatusMessage()).isBlank();
  }

  @Test
  void getByIdShouldReturnSuccessAndStatusMessageWhenNoResourceFound() {
    // given
    var id = "123";
    final var iReadExecutable = mockFhirGetResourceById(Medication.class, id);
    when(iReadExecutable.execute()).thenThrow(ResourceNotFoundException.class);

    // when
    final GetMedicationResponseDTO response = medicationService.executeGetById(id);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedications()).isEmpty();
    assertThat(response.getStatusMessage()).isNotBlank();
  }

  @Test
  void shouldSearchMedication() {
    // given
    Integer count = 10;
    Integer offset = 0;
    String identifier = "ABC123";
    String code = "MED123";
    String status = "active";
    String total = "accurate";
    String lastUpdated = "gt2021-01-01";

    final var executable = mockSearchMedications(Medication.class);
    final Bundle resultBundle = new Bundle();
    final Bundle.BundleEntryComponent bc = new Bundle.BundleEntryComponent();
    final Medication medication = getMedication();
    bc.setResource(medication);
    resultBundle.addEntry(bc);
    when(executable.execute()).thenReturn(resultBundle);

    when(jsonParser.encodeResourceToString(medication)).thenReturn(medicationAsString);

    // when
    final GetMedicationResponseDTO response =
        medicationService.searchMedications(
            new MedicationsSearch()
                .code(code)
                .status(status)
                .total(total)
                .lastUpdated(lastUpdated)
                .identifier(identifier)
                .count(count)
                .offset(offset));

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedications()).hasSize(1);
    assertThat(response.getStatusMessage()).isBlank();
  }

  @Test
  void searchShouldReturnNoSuccessWhenFailure() {
    // given
    Integer count = 10;
    Integer offset = 0;
    String identifier = "ABC123";
    String code = "MED123";
    String status = "active";

    final var executable = mockSearchMedications(Medication.class);
    when(executable.execute()).thenThrow(InvalidResponseException.class);

    // when
    final GetMedicationResponseDTO response =
        medicationService.searchMedications(
            new MedicationsSearch()
                .code(code)
                .status(status)
                .identifier(identifier)
                .count(count)
                .offset(offset));

    // then
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getMedications()).isEmpty();
    assertThat(response.getStatusMessage()).isNotBlank();
  }

  @Test
  void searchShouldReturnSuccessAndStatusMessageWhenNoResourceFound() {
    // given
    Integer count = 10;
    Integer offset = 0;
    String identifier = "ABC123";
    String code = "MED123";
    String status = "active";

    final var executable = mockSearchMedications(Medication.class);
    final var resultBundle = new Bundle();
    when(executable.execute()).thenReturn(resultBundle);

    // when
    final MedicationsSearch request =
        new MedicationsSearch()
            .code(code)
            .status(status)
            .identifier(identifier)
            .count(count)
            .offset(offset);
    final GetMedicationResponseDTO response = medicationService.searchMedications(request);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedications()).isEmpty();
    assertThat(response.getStatusMessage()).isNotBlank();
    assertThat(response.getStatusMessage()).contains("No medication found for search params");
  }

  @Test
  void medicationResponseShouldBeValidJson() throws Exception {
    // given
    final GetMedicationResponseDTO medicationResponse =
        new GetMedicationResponseDTO().success(true).medications(List.of(medicationAsString));

    // when
    var json =
        objectMapper.readValue(medicationResponse.getMedications().getFirst(), JsonNode.class);

    // then
    assertThat(json).isNotNull();
  }

  @Test
  void shouldGetEmlAsPdf() {
    // given
    final String recordId = "123";
    final byte[] pdf = new byte[] {1, 2, 3};
    when(emlRenderClient.getEmlAsPdf(recordId))
        .thenReturn(new RenderResponse().pdf(pdf).httpStatusCode(200));

    // when
    var result = medicationService.getEmlAsPdf(recordId);

    // then
    assertThat(result.getEml()).isEqualTo(pdf);
    assertThat(result.getSuccess()).isTrue();
  }

  @Test
  void getEmlAsPdfReturnStatusMessageWhenPdfHasFailure() {
    // given
    when(emlRenderClient.getEmlAsPdf("insurantId"))
        .thenReturn(new RenderResponse().errorMessage("error").httpStatusCode(500));

    // when
    var result = medicationService.getEmlAsPdf("insurantId");

    // then
    assertThat(result.getStatusMessage()).isNotBlank();
    assertThat(result.getSuccess()).isFalse();
  }

  @Test
  void shouldGetEmlAsXhtml() {
    // given
    final String recordId = "123";
    final String xhtml = "<html><body>test</body></html>";
    when(emlRenderClient.getEmlAsXhtml(recordId))
        .thenReturn(new RenderResponse().xhtml(xhtml).httpStatusCode(200));

    // when
    var result = medicationService.getEmlAsXhtml(recordId);

    // then
    assertThat(result.getEml()).isEqualTo(xhtml);
    assertThat(result.getSuccess()).isTrue();
  }

  @Test
  void getEmlAsPdfReturnStatusMessageWhenXhtmlHasFailure() {
    // given
    when(emlRenderClient.getEmlAsXhtml("insurantId"))
        .thenReturn(new RenderResponse().errorMessage("error").httpStatusCode(500));

    // when
    var result = medicationService.getEmlAsXhtml("insurantId");

    // then
    assertThat(result.getStatusMessage()).isNotBlank();
    assertThat(result.getSuccess()).isFalse();
  }

  @Test
  void shouldGetEmlAsFhirSuccessfully() {
    var requestId = UUID.randomUUID();
    var insurantId = "insurantId";
    var date = "2023-01-01";
    var count = 10;
    var offset = 0;
    var bundle =
        """
            {
              "resourceType": "Bundle",
              "type": "searchset",
              "total": 16
                }""";
    var format = "application/fhir+json";
    var renderResponse = new RenderResponse().emlAsFhir(bundle).httpStatusCode(200);

    when(emlRenderClient.getMedicationList(
            insurantId, requestId.toString(), date, count, offset, format))
        .thenReturn(renderResponse);

    var response =
        medicationService.getEmlAsFhir(requestId, insurantId, date, count, offset, format);

    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getEml()).isEqualTo(bundle);
    assertThat(response.getStatusMessage()).isBlank();
  }

  @Test
  void getEmlAsFhirShouldHandleFailureResponse() {
    var requestId = UUID.randomUUID();
    var insurantId = "insurantId";
    var date = "invalid-date";
    var count = 10;
    var offset = 0;
    var errorMessage = "Invalid date format";
    var format = "application/fhir+json";
    var renderResponse = new RenderResponse().errorMessage(errorMessage).httpStatusCode(400);
    when(emlRenderClient.getMedicationList(
            insurantId, requestId.toString(), date, count, offset, format))
        .thenReturn(renderResponse);

    var response =
        medicationService.getEmlAsFhir(requestId, insurantId, date, count, offset, format);

    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getStatusMessage()).isEqualTo(errorMessage);
  }

  @Test
  void shouldGetMedicationRequestById() {
    // given
    var id = "123";
    final MedicationRequest medicationRequest = new MedicationRequest();
    medicationRequest.setId(id);

    var iReadExecutable = mockFhirGetResourceById(MedicationRequest.class, id);
    when(iReadExecutable.execute()).thenReturn(medicationRequest);

    when(jsonParser.encodeResourceToString(medicationRequest))
        .thenReturn(medicationRequestAsString);

    // when
    final GetMedicationRequestListDTO response = medicationService.getMedicationRequestById(id);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedicationRequests()).hasSize(1);
    assertThat(response.getStatusMessage()).isBlank();
  }

  @Test
  void shouldSearchMedicationRequest() {
    // given
    final var executable = mockSearchMedications(MedicationRequest.class);
    final Bundle resultBundle = new Bundle();
    final Bundle.BundleEntryComponent bc = new Bundle.BundleEntryComponent();
    final MedicationRequest medicationRequest = new MedicationRequest();
    bc.setResource(medicationRequest);
    resultBundle.addEntry(bc);

    when(executable.execute()).thenReturn(resultBundle);
    when(jsonParser.encodeResourceToString(medicationRequest))
        .thenReturn(medicationRequestAsString);

    // when
    final GetMedicationRequestListDTO response =
        medicationService.searchMedicationRequests(
            new MedicationsSearch().status("active").count(10).offset(0));

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedicationRequests()).hasSize(1);
    assertThat(response.getStatusMessage()).isBlank();
  }

  @Test
  void getMedicationRequestByIdShouldReturnSuccessAndStatusMessageWhenNoResourceFound() {
    // given
    var id = "123";
    final var iReadExecutable = mockFhirGetResourceById(MedicationRequest.class, id);
    when(iReadExecutable.execute()).thenThrow(ResourceNotFoundException.class);

    // when
    final GetMedicationRequestListDTO response = medicationService.getMedicationRequestById(id);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedicationRequests()).isEmpty();
    assertThat(response.getStatusMessage()).isNotBlank();
  }

  @Test
  void searchMedicationRequestsShouldReturnSuccessAndStatusMessageWhenNoResourceFound() {
    // given
    final var executable = mockSearchMedications(MedicationRequest.class);
    final var resultBundle = new Bundle();
    when(executable.execute()).thenReturn(resultBundle);

    // when
    MedicationsSearch request = new MedicationsSearch().status("active").count(10).offset(0);
    final GetMedicationRequestListDTO response =
        medicationService.searchMedicationRequests(request);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedicationRequests()).isEmpty();
    assertThat(response.getStatusMessage()).isNotBlank();
  }

  @Test
  void shouldGetMedicationDispenseById() {
    // given
    var id = "123";
    final MedicationDispense medicationDispense = new MedicationDispense();
    medicationDispense.setId(id);

    var iReadExecutable = mockFhirGetResourceById(MedicationDispense.class, id);
    when(iReadExecutable.execute()).thenReturn(medicationDispense);

    when(jsonParser.encodeResourceToString(medicationDispense))
        .thenReturn(medicationDispenseAsString);

    // when
    final GetMedicationDispenseListDTO response = medicationService.getMedicationDispenseById(id);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedicationDispenses()).hasSize(1);
    assertThat(response.getStatusMessage()).isBlank();
  }

  @Test
  void shouldSearchMedicationDispense() {
    // given
    final var executable = mockSearchMedications(MedicationDispense.class);
    final Bundle resultBundle = new Bundle();
    final Bundle.BundleEntryComponent bc = new Bundle.BundleEntryComponent();
    final MedicationDispense medicationDispense = new MedicationDispense();
    bc.setResource(medicationDispense);
    resultBundle.addEntry(bc);

    when(executable.execute()).thenReturn(resultBundle);
    when(jsonParser.encodeResourceToString(medicationDispense))
        .thenReturn(medicationDispenseAsString);

    // when
    final GetMedicationDispenseListDTO response =
        medicationService.searchMedicationDispenses(
            new MedicationsSearch().status("completed").count(10).offset(0));

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedicationDispenses()).hasSize(1);
    assertThat(response.getStatusMessage()).isBlank();
  }

  @Test
  void getMedicationDispenseByIdShouldReturnSuccessAndStatusMessageWhenNoResourceFound() {
    // given
    var id = "123";
    final var iReadExecutable = mockFhirGetResourceById(MedicationDispense.class, id);
    when(iReadExecutable.execute()).thenThrow(ResourceNotFoundException.class);

    // when
    final GetMedicationDispenseListDTO response = medicationService.getMedicationDispenseById(id);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedicationDispenses()).isEmpty();
    assertThat(response.getStatusMessage()).isNotBlank();
  }

  @Test
  void searchMedicationDispensesShouldReturnSuccessAndStatusMessageWhenNoResourceFound() {
    // given
    final var executable = mockSearchMedications(MedicationDispense.class);
    final var resultBundle = new Bundle();
    when(executable.execute()).thenReturn(resultBundle);

    // when
    MedicationsSearch request = new MedicationsSearch().status("completed").count(10).offset(0);
    final GetMedicationDispenseListDTO response =
        medicationService.searchMedicationDispenses(request);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedicationDispenses()).isEmpty();
    assertThat(response.getStatusMessage()).isNotBlank();
  }

  @Test
  void shouldReturnEmlAsFhir() {
    final MedicationsSearch searchRequest =
        new MedicationsSearch().count(10).offset(0).status("active").lastUpdated("gt2021-01-01");
    final Bundle medicationsBundle = new Bundle();
    final Bundle.BundleEntryComponent bc = new Bundle.BundleEntryComponent();
    final Medication medication = new Medication();
    bc.setResource(medication);
    medicationsBundle.addEntry(bc);
    final Bundle practitionersBundle = new Bundle();
    final Bundle organizationsBundle = new Bundle();

    IGenericClient client = mock(IGenericClient.class);
    when(fhirClient.getClient()).thenReturn(client);

    IUntypedQuery<IBaseBundle> searchMedication = mock(IUntypedQuery.class);
    IQuery<IBaseBundle> iBaseBundleIQuery = mock(IQuery.class);
    IQuery<Bundle> bundleQuery = mock(IQuery.class);

    IUntypedQuery<IBaseBundle> searchPractitioner = mock(IUntypedQuery.class);
    IUntypedQuery<IBaseBundle> searchOrg = mock(IUntypedQuery.class);

    when(client.search())
        .thenReturn(searchMedication)
        .thenReturn(searchPractitioner)
        .thenReturn(searchOrg);

    when(searchMedication.forResource(Medication.class)).thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.and(any(ICriterion.class))).thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.count(searchRequest.count())).thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.offset(searchRequest.offset())).thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.revInclude(new Include(MEDICATION_REQUEST_MEDICATION)))
        .thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.revInclude(new Include(MEDICATION_DISPENSE_MEDICATION)))
        .thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.totalMode(FhirUtils.calculateTotalMode(searchRequest.total())))
        .thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.lastUpdated(any(DateRangeParam.class))).thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.returnBundle(Bundle.class)).thenReturn(bundleQuery);
    when(bundleQuery.execute()).thenReturn(medicationsBundle);

    IQuery<IBaseBundle> practitionerQuery = mock(IQuery.class);
    IQuery<Bundle> practitionerBundleQuery = mock(IQuery.class);

    when(searchPractitioner.forResource(PractitionerRole.class)).thenReturn(practitionerQuery);
    when(practitionerQuery.include(new Include(PRACTITIONER_ROLE_PRACTITIONER)))
        .thenReturn(practitionerQuery);
    when(practitionerQuery.returnBundle(Bundle.class)).thenReturn(practitionerBundleQuery);
    when(practitionerBundleQuery.execute()).thenReturn(practitionersBundle);

    // Mocking searchOrg
    IQuery<IBaseBundle> orgQuery = mock(IQuery.class);
    IQuery<Bundle> orgBundleQuery = mock(IQuery.class);
    when(searchOrg.forResource(Organization.class)).thenReturn(orgQuery);
    when(orgQuery.returnBundle(Bundle.class)).thenReturn(orgBundleQuery);
    when(orgBundleQuery.execute()).thenReturn(organizationsBundle);

    when(jsonParser.encodeResourceToString(any(Bundle.class))).thenReturn(medicationListAsString);

    final GetMedicationListAsFhirResponseDTO response =
        medicationService.searchForEmlAsFhir(searchRequest);

    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getEml()).isNotBlank();
    assertThat(response.getEml())
        .contains("Medication")
        .contains("PractitionerRole")
        .contains("Organization");
  }

  @Test
  void searchForEmlAsFhirShouldNotExecuteUnnecessaryCalls() {
    // given
    final MedicationsSearch searchRequest = new MedicationsSearch().count(10).offset(0);
    final Bundle medicationsBundle = new Bundle();

    var client = mock(IGenericClient.class);
    when(fhirClient.getClient()).thenReturn(client);

    IUntypedQuery<IBaseBundle> searchMedication = mock(IUntypedQuery.class);
    IQuery<IBaseBundle> iBaseBundleIQuery = mock(IQuery.class);
    IQuery<Bundle> bundleQuery = mock(IQuery.class);

    when(client.search()).thenReturn(searchMedication);

    when(searchMedication.forResource(Medication.class)).thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.and(any(ICriterion.class))).thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.count(searchRequest.count())).thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.offset(searchRequest.offset())).thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.revInclude(new Include(MEDICATION_REQUEST_MEDICATION)))
        .thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.revInclude(new Include(MEDICATION_DISPENSE_MEDICATION)))
        .thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.totalMode(FhirUtils.calculateTotalMode(searchRequest.total())))
        .thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.returnBundle(Bundle.class)).thenReturn(bundleQuery);
    when(bundleQuery.execute()).thenReturn(medicationsBundle);

    when(jsonParser.encodeResourceToString(any(Bundle.class))).thenReturn(medicationListAsString);

    // when
    final GetMedicationListAsFhirResponseDTO response =
        medicationService.searchForEmlAsFhir(searchRequest);

    verify(bundleQuery, times(1)).execute();

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getEml()).isNotBlank();
  }

  @Test
  void searchForEmlAsFhirReturnsExpectedResponseForException() {
    // given
    final MedicationsSearch searchRequest = new MedicationsSearch().count(10).offset(0);

    var client = mock(IGenericClient.class);
    when(fhirClient.getClient()).thenReturn(client);

    IUntypedQuery<IBaseBundle> searchMedication = mock(IUntypedQuery.class);
    IQuery<IBaseBundle> iBaseBundleIQuery = mock(IQuery.class);
    IQuery<Bundle> bundleQuery = mock(IQuery.class);

    when(client.search()).thenReturn(searchMedication);
    when(searchMedication.forResource(Medication.class)).thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.and(any(ICriterion.class))).thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.count(searchRequest.count())).thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.offset(searchRequest.offset())).thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.revInclude(new Include(MEDICATION_REQUEST_MEDICATION)))
        .thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.revInclude(new Include(MEDICATION_DISPENSE_MEDICATION)))
        .thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.totalMode(FhirUtils.calculateTotalMode(searchRequest.total())))
        .thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.returnBundle(Bundle.class)).thenReturn(bundleQuery);
    when(bundleQuery.execute()).thenThrow(InvalidResponseException.class);

    // when
    final GetMedicationListAsFhirResponseDTO response =
        medicationService.searchForEmlAsFhir(searchRequest);

    // then
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getStatusMessage()).isNotBlank();
  }

  @Test
  void searchForEmlAsFhirReturnsEmptyBundleWhenNoMedicationWasFound() {
    // given
    final MedicationsSearch searchRequest = new MedicationsSearch().count(10).offset(0);
    final Bundle medicationsBundle = new Bundle();
    medicationsBundle.addEntry(null);

    IGenericClient client = mock(IGenericClient.class);
    when(fhirClient.getClient()).thenReturn(client);

    IUntypedQuery<IBaseBundle> searchMedication = mock(IUntypedQuery.class);
    IQuery<IBaseBundle> iBaseBundleIQuery = mock(IQuery.class);
    IQuery<Bundle> bundleQuery = mock(IQuery.class);

    when(client.search()).thenReturn(searchMedication);
    when(searchMedication.forResource(Medication.class)).thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.and(any(ICriterion.class))).thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.count(searchRequest.count())).thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.offset(searchRequest.offset())).thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.revInclude(new Include(MEDICATION_REQUEST_MEDICATION)))
        .thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.revInclude(new Include(MEDICATION_DISPENSE_MEDICATION)))
        .thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.totalMode(FhirUtils.calculateTotalMode(searchRequest.total())))
        .thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.returnBundle(Bundle.class)).thenReturn(bundleQuery);
    when(bundleQuery.execute()).thenReturn(medicationsBundle);

    // when
    final GetMedicationListAsFhirResponseDTO response =
        medicationService.searchForEmlAsFhir(searchRequest);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getStatusMessage()).isNotBlank();
    assertThat(response.getEml()).isBlank();
  }

  @Test
  void searchForEmlAsFhirReturnsExpectedResponseForNullMedication() {
    // given
    final MedicationsSearch searchRequest = new MedicationsSearch().count(10).offset(0);

    IGenericClient client = mock(IGenericClient.class);
    when(fhirClient.getClient()).thenReturn(client);

    IUntypedQuery<IBaseBundle> searchMedication = mock(IUntypedQuery.class);
    IQuery<IBaseBundle> iBaseBundleIQuery = mock(IQuery.class);
    IQuery<Bundle> bundleQuery = mock(IQuery.class);

    when(client.search()).thenReturn(searchMedication);
    when(searchMedication.forResource(Medication.class)).thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.and(any(ICriterion.class))).thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.count(searchRequest.count())).thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.offset(searchRequest.offset())).thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.revInclude(new Include(MEDICATION_REQUEST_MEDICATION)))
        .thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.revInclude(new Include(MEDICATION_DISPENSE_MEDICATION)))
        .thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.totalMode(FhirUtils.calculateTotalMode(searchRequest.total())))
        .thenReturn(iBaseBundleIQuery);
    when(iBaseBundleIQuery.returnBundle(Bundle.class)).thenReturn(bundleQuery);
    when(bundleQuery.execute()).thenReturn(null);

    // when
    final GetMedicationListAsFhirResponseDTO response =
        medicationService.searchForEmlAsFhir(searchRequest);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getStatusMessage()).isNotBlank();
    assertThat(response.getEml()).isBlank();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"le2023-01-01", "ge2023-01-01", "lt2023-01-01", "gt2023-01-01", "eq2023-01-01"})
  void filterByWhenHandedOverReturnsCorrectQuery(String whenHandedOver) {
    IQuery<IBaseBundle> query = mock(IQuery.class);
    IQuery<IBaseBundle> expectedQuery = mock(IQuery.class);
    when(query.and(any())).thenReturn(expectedQuery);

    IQuery<IBaseBundle> result = MedicationService.filterByWhenHandedOver(whenHandedOver, query);

    assertThat(result).isEqualTo(expectedQuery);
    verify(query).and(any(DateClientParam.IDateCriterion.class));
  }

  @Test
  void filterByWhenHandedOverReturnsOriginalQueryWhenPrefixIsInvalid() {
    String whenHandedOver = "xx2023-01-01";
    IQuery<IBaseBundle> query = mock(IQuery.class);

    IQuery<IBaseBundle> result = MedicationService.filterByWhenHandedOver(whenHandedOver, query);

    assertThat(result).isEqualTo(query);
    verify(query, never()).and(any());
  }

  @Test
  void filterByWhenHandedOverReturnsOriginalQueryForEmptyInput() {
    String whenHandedOver = "";
    IQuery<IBaseBundle> query = mock(IQuery.class);

    IQuery<IBaseBundle> result = MedicationService.filterByWhenHandedOver(whenHandedOver, query);

    assertThat(result).isEqualTo(query);
    verify(query, never()).and(any());
  }

  @Test
  void getEmlAsFhirShouldHandleInvalidFormat() {
    var requestId = UUID.randomUUID();
    var insurantId = "insurantId";
    var date = "2023-01-01";
    var count = 10;
    var offset = 0;
    var format = "invalid/format";

    var response =
        medicationService.getEmlAsFhir(requestId, insurantId, date, count, offset, format);
    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getStatusMessage()).isNotBlank();
  }

  @Test
  void shouldSearchMedicationWithContextFilter() {
    // given
    Integer count = 10;
    Integer offset = 0;
    String status = "active";
    String contextQuery = "EMP";

    final var executable = mockSearchMedications(Medication.class);
    final Bundle resultBundle = new Bundle();
    final Bundle.BundleEntryComponent bc = new Bundle.BundleEntryComponent();
    final Medication medication = getMedication();
    bc.setResource(medication);
    resultBundle.addEntry(bc);
    when(executable.execute()).thenReturn(resultBundle);

    when(jsonParser.encodeResourceToString(medication)).thenReturn(medicationAsString);

    // when
    final GetMedicationResponseDTO response =
        medicationService.searchMedications(
            new MedicationsSearch()
                .status(status)
                .context(contextQuery)
                .count(count)
                .offset(offset));

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedications()).hasSize(1);
    assertThat(response.getStatusMessage()).isBlank();
  }

  @Test
  void shouldSearchMedicationWithPrescription() {
    // given
    Integer count = 10;
    Integer offset = 0;
    String status = "active";
    String prescription = "160.000.000.000.000.00";

    final var executable = mockSearchMedications(Medication.class);
    final Bundle resultBundle = new Bundle();
    final Bundle.BundleEntryComponent bc = new Bundle.BundleEntryComponent();
    final Medication medication = getMedication();
    bc.setResource(medication);
    resultBundle.addEntry(bc);
    when(executable.execute()).thenReturn(resultBundle);

    when(jsonParser.encodeResourceToString(medication)).thenReturn(medicationAsString);

    // when
    final GetMedicationResponseDTO response =
        medicationService.searchMedications(
            new MedicationsSearch()
                .status(status)
                .prescription(prescription)
                .count(count)
                .offset(offset));

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedications()).hasSize(1);
    assertThat(response.getStatusMessage()).isBlank();
  }

  @Test
  void shouldSearchMedicationWithPrescriptionSystemAndValue() {
    // given
    Integer count = 10;
    Integer offset = 0;
    String status = "active";
    String prescription = "https://gematik.de/fhir/sid/erp-prescription-id|160.000.000.000.000.00";

    final var executable = mockSearchMedications(Medication.class);
    final Bundle resultBundle = new Bundle();
    final Bundle.BundleEntryComponent bc = new Bundle.BundleEntryComponent();
    final Medication medication = getMedication();
    bc.setResource(medication);
    resultBundle.addEntry(bc);
    when(executable.execute()).thenReturn(resultBundle);

    when(jsonParser.encodeResourceToString(medication)).thenReturn(medicationAsString);

    // when
    final GetMedicationResponseDTO response =
        medicationService.searchMedications(
            new MedicationsSearch()
                .status(status)
                .prescription(prescription)
                .count(count)
                .offset(offset));

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedications()).hasSize(1);
    assertThat(response.getStatusMessage()).isBlank();
  }

  @Test
  void shouldSearchMedicationWithIngredientCode() {
    // given
    Integer count = 10;
    Integer offset = 0;
    String ingredientCode = "24421";

    final var executable = mockSearchMedications(Medication.class);
    final Bundle resultBundle = new Bundle();
    final Bundle.BundleEntryComponent bc = new Bundle.BundleEntryComponent();
    final Medication medication = getMedication();
    bc.setResource(medication);
    resultBundle.addEntry(bc);
    when(executable.execute()).thenReturn(resultBundle);

    when(jsonParser.encodeResourceToString(medication)).thenReturn(medicationAsString);

    // when
    final GetMedicationResponseDTO response =
        medicationService.searchMedications(
            new MedicationsSearch().ingredientCode(ingredientCode).count(count).offset(offset));

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedications()).hasSize(1);
    assertThat(response.getStatusMessage()).isBlank();
  }

  @Test
  void shouldSearchMedicationWithIngredientCodeSystemAndValue() {
    // given
    Integer count = 10;
    Integer offset = 0;
    String ingredientCode = "http://fhir.de/CodeSystem/ask|24421";

    final var executable = mockSearchMedications(Medication.class);
    final Bundle resultBundle = new Bundle();
    final Bundle.BundleEntryComponent bc = new Bundle.BundleEntryComponent();
    final Medication medication = getMedication();
    bc.setResource(medication);
    resultBundle.addEntry(bc);
    when(executable.execute()).thenReturn(resultBundle);

    when(jsonParser.encodeResourceToString(medication)).thenReturn(medicationAsString);

    // when
    final GetMedicationResponseDTO response =
        medicationService.searchMedications(
            new MedicationsSearch().ingredientCode(ingredientCode).count(count).offset(offset));

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedications()).hasSize(1);
    assertThat(response.getStatusMessage()).isBlank();
  }

  private <T extends IBaseResource> IClientExecutable mockSearchMedications(
      final Class<T> resourceClass) {
    final IGenericClient client = mock(IGenericClient.class);
    when(fhirClient.getClient()).thenReturn(client);

    final IUntypedQuery<IBaseBundle> search = mock(IUntypedQuery.class);
    when(client.search()).thenReturn(search);

    final IQuery<IBaseBundle> bundleIQuery = mock(IQuery.class);
    when(search.forResource(resourceClass)).thenReturn(bundleIQuery);

    final IQuery<IBaseBundle> where = mock(IQuery.class);
    when(bundleIQuery.where(any(ICriterion.class))).thenReturn(where);
    when(where.and(any(ICriterion.class))).thenReturn(where);
    when(where.count(anyInt())).thenReturn(where);
    when(where.offset(anyInt())).thenReturn(where);
    when(where.totalMode(any(SearchTotalModeEnum.class))).thenReturn(where);
    when(where.lastUpdated(any(DateRangeParam.class))).thenReturn(where);
    when(where.revInclude(any(Include.class))).thenReturn(where);
    when(where.include(any(Include.class))).thenReturn(where);

    final IQuery<Bundle> bundle = mock(IQuery.class);
    when(where.returnBundle(Bundle.class)).thenReturn(bundle);
    when(bundle.encodedJson()).thenReturn(bundle);
    return bundle;
  }

  private <T extends IBaseResource> IReadExecutable<T> mockFhirGetResourceById(
      final Class<T> resourceClass, final String id) {
    final IGenericClient client = mock(IGenericClient.class);
    when(fhirClient.getClient()).thenReturn(client);

    final IRead read = mock(IRead.class);
    when(client.read()).thenReturn(read);

    final IReadTyped<T> resource = mock(IReadTyped.class);
    when(read.resource(resourceClass)).thenReturn(resource);

    final IReadExecutable<T> resourceIReadExecutable = mock(IReadExecutable.class);
    when(resource.withId(id)).thenReturn(resourceIReadExecutable);
    when(resourceIReadExecutable.encodedJson()).thenReturn(resourceIReadExecutable);
    return resourceIReadExecutable;
  }

  @Test
  void shouldGetEmpAsPdf() {
    /*
    Uses a mock of the EmlRenderClient.
    The method from the mock is configurated to return a RenderResponse which is configured
    to contain the given byte array and the 200 HTTP status code.

    This way the method actually returns the ResponseRender with the preconfigured values.
    This can then be tested.

    The MedicationService.getEmpAsPdf calls upon the EmlRenderClient.getEmpAsPdf method.
     */

    // given
    final String insurantId = "12345";
    final byte[] pdf = new byte[] {1, 2, 3};
    when(emlRenderClient.getEmpAsPdf(insurantId))
        .thenReturn(new RenderResponse().pdf(pdf).httpStatusCode(200));

    // when
    var result = medicationService.getEmpAsPdf(insurantId);

    // then
    assertThat(result.getEmp()).isEqualTo(pdf);
    assertThat(result.getSuccess()).isTrue();

    final byte[] wrongBytes = new byte[] {4, 5, 6};

    assertThat(result.getEmp()).isNotEqualTo(wrongBytes);
  }

  @Test
  void shouldSearchMedicationsHistoryWithJsonFormat() {
    // given
    var id = "123";
    var format = "application/fhir+json";
    var searchRequest = new MedicationsHistorySearch().id(id).format(format);

    var client = mock(IGenericClient.class);
    when(fhirClient.getClient()).thenReturn(client);

    var history = mock(IHistory.class);
    when(client.history()).thenReturn(history);

    var historyUntyped = mock(IHistoryUntyped.class);
    when(history.onInstance(any(IdType.class))).thenReturn(historyUntyped);

    var historyTyped = mock(IHistoryTyped.class);
    when(historyUntyped.returnBundle(Bundle.class)).thenReturn(historyTyped);

    var historyBundle = new Bundle();
    var entry = new Bundle.BundleEntryComponent();
    var medication = new Medication();
    medication.setId(id);
    entry.setResource(medication);
    historyBundle.addEntry(entry);

    when(historyTyped.execute()).thenReturn(historyBundle);
    when(jsonParser.encodeResourceToString(medication)).thenReturn(medicationAsString);

    // when
    var response = medicationService.searchMedicationsHistory(searchRequest);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedications()).hasSize(1);
    assertThat(response.getMedications().getFirst()).isEqualTo(medicationAsString);
    assertThat(response.getStatusMessage()).isBlank();
  }

  @Test
  void shouldSearchMedicationsHistoryWithXmlFormat() {
    // given
    var id = "123";
    var format = "application/fhir+xml";
    var searchRequest = new MedicationsHistorySearch().id(id).format(format);

    var client = mock(IGenericClient.class);
    when(fhirClient.getClient()).thenReturn(client);

    var history = mock(IHistory.class);
    when(client.history()).thenReturn(history);

    var historyUntyped = mock(IHistoryUntyped.class);
    when(history.onInstance(any(IdType.class))).thenReturn(historyUntyped);

    var historyTyped = mock(IHistoryTyped.class);
    when(historyUntyped.returnBundle(Bundle.class)).thenReturn(historyTyped);

    var historyBundle = new Bundle();
    var entry = new Bundle.BundleEntryComponent();
    var medication = new Medication();
    medication.setId(id);
    entry.setResource(medication);
    historyBundle.addEntry(entry);

    when(historyTyped.execute()).thenReturn(historyBundle);

    var xmlParser = mock(IParser.class);
    when(context.newXmlParser()).thenReturn(xmlParser);
    FhirUtils.setXmlParser(xmlParser);
    when(xmlParser.encodeResourceToString(medication)).thenReturn("<Medication/>");

    // when
    var response = medicationService.searchMedicationsHistory(searchRequest);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedications()).hasSize(1);
    assertThat(response.getMedications().getFirst()).isEqualTo("<Medication/>");
    assertThat(response.getStatusMessage()).isBlank();
  }

  @Test
  void searchMedicationsHistoryShouldReturnSuccessAndStatusMessageWhenNoResourceFound() {
    // given
    var id = "123";
    var format = "application/fhir+json";
    var searchRequest = new MedicationsHistorySearch().id(id).format(format);

    var client = mock(IGenericClient.class);
    when(fhirClient.getClient()).thenReturn(client);

    var history = mock(IHistory.class);
    when(client.history()).thenReturn(history);

    var historyUntyped = mock(IHistoryUntyped.class);
    when(history.onInstance(any(IdType.class))).thenReturn(historyUntyped);

    var historyTyped = mock(IHistoryTyped.class);
    when(historyUntyped.returnBundle(Bundle.class)).thenReturn(historyTyped);

    var historyBundle = new Bundle();
    when(historyTyped.execute()).thenReturn(historyBundle);

    // when
    var response = medicationService.searchMedicationsHistory(searchRequest);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedications()).isEmpty();
    assertThat(response.getStatusMessage()).isNotBlank();
    assertThat(response.getStatusMessage()).contains("No medication historyBundle found for ID");
  }

  @Test
  void searchMedicationsHistoryShouldHandleException() {
    // given
    var id = "123";
    var format = "application/fhir+json";
    var searchRequest = new MedicationsHistorySearch().id(id).format(format);

    var client = mock(IGenericClient.class);
    when(fhirClient.getClient()).thenReturn(client);

    var history = mock(IHistory.class);
    when(client.history()).thenReturn(history);

    var historyUntyped = mock(IHistoryUntyped.class);
    when(history.onInstance(any(IdType.class))).thenReturn(historyUntyped);

    var historyTyped = mock(IHistoryTyped.class);
    when(historyUntyped.returnBundle(Bundle.class)).thenReturn(historyTyped);

    when(historyTyped.execute()).thenThrow(InvalidResponseException.class);

    // when
    var response = medicationService.searchMedicationsHistory(searchRequest);

    // then
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getMedications()).isEmpty();
    assertThat(response.getStatusMessage()).isNotBlank();
  }

  @Test
  void searchMedicationsHistoryShouldHandleResourceNotFoundException() {
    // given
    var id = "123";
    var format = "application/fhir+json";
    var searchRequest = new MedicationsHistorySearch().id(id).format(format);

    var client = mock(IGenericClient.class);
    when(fhirClient.getClient()).thenReturn(client);

    var history = mock(IHistory.class);
    when(client.history()).thenReturn(history);

    var historyUntyped = mock(IHistoryUntyped.class);
    when(history.onInstance(any(IdType.class))).thenReturn(historyUntyped);

    var historyTyped = mock(IHistoryTyped.class);
    when(historyUntyped.returnBundle(Bundle.class)).thenReturn(historyTyped);

    when(historyTyped.execute()).thenThrow(ResourceNotFoundException.class);

    // when
    var response = medicationService.searchMedicationsHistory(searchRequest);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedications()).isEmpty();
    assertThat(response.getStatusMessage()).isNotBlank();
    assertThat(response.getStatusMessage()).contains("No medication historyBundle found for ID");
  }
}
