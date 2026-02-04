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
package de.gematik.epa.ps.medication;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import ca.uhn.fhir.rest.api.MethodOutcome;
import de.gematik.epa.api.testdriver.medication.dto.GetMedicationDispenseListDTO;
import de.gematik.epa.api.testdriver.medication.dto.GetMedicationHistoryResponseDTO;
import de.gematik.epa.api.testdriver.medication.dto.GetMedicationListAsFhirResponseDTO;
import de.gematik.epa.api.testdriver.medication.dto.GetMedicationRequestListDTO;
import de.gematik.epa.api.testdriver.medication.dto.GetMedicationResponseDTO;
import de.gematik.epa.fhir.client.FhirClient;
import de.gematik.epa.ps.endpoint.MedicationApiEndpoint;
import de.gematik.epa.ps.fhir.config.TestFhirClientProvider;
import de.gematik.epa.unit.TestDocumentClientConfiguration;
import de.gematik.epa.unit.TestKonnektorClientConfiguration;
import de.gematik.epa.utils.FhirUtils;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(
    classes = {
      TestKonnektorClientConfiguration.class,
      TestDocumentClientConfiguration.class,
      TestFhirClientProvider.class
    })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@AutoConfigureRestTestClient
@Slf4j
class MedicationApiEndpointIntegrationTest {

  private static final String MEDICATION_STATEMENT = "MedicationStatement";
  private static final String MEDICATION = "Medication";
  private static final String MEDICATION_REQUEST = "MedicationRequest";
  private static final int PORT = 8080;
  private static final DockerImageName fhirDockerImage =
      DockerImageName.parse("hapiproject/hapi:latest");
  private final GenericContainer<?> fhirServer =
      new GenericContainer<>(fhirDockerImage)
          .withExposedPorts(PORT)
          .waitingFor(
              Wait.forHttp("/fhir/metadata")
                  .forStatusCode(200)
                  .withStartupTimeout(Duration.ofMinutes(5L)));

  private final List<String> medicationIds = new ArrayList<>();
  private final List<String> medicationRequestIds = new ArrayList<>();
  private final List<String> medicationDispenseIds = new ArrayList<>();

  @Autowired MedicationApiEndpoint medicationApiEndpoint;
  @Autowired FhirClient fhirClient;
  @Autowired RestTestClient restTestClient;

  @BeforeAll
  void setUp() {
    fhirServer.start();
    var serverUrl =
        "http://" + fhirServer.getHost() + ":" + fhirServer.getMappedPort(PORT) + "/fhir";
    fhirClient.setServerUrl(serverUrl, "PS-SIM");
    FhirUtils.setJsonParser(fhirClient.getContext().newJsonParser());

    registerCustomSearchParameters();
  }

  private void registerCustomSearchParameters() {
    var contextSearchParameter = new SearchParameter();
    contextSearchParameter.setId("context-sp");
    contextSearchParameter.setUrl(
        "https://gematik.de/fhir/epa-medication/SearchParameter/context-sp");
    contextSearchParameter.setVersion("1.2.0");
    contextSearchParameter.setName("ContextSP");
    contextSearchParameter.setStatus(Enumerations.PublicationStatus.ACTIVE);
    contextSearchParameter.setCode("context");
    contextSearchParameter.addBase(MEDICATION_STATEMENT);
    contextSearchParameter.addBase(MEDICATION);
    contextSearchParameter.addBase(MEDICATION_REQUEST);
    contextSearchParameter.setType(Enumerations.SearchParamType.TOKEN);
    contextSearchParameter.setExpression(
        "Resource.extension('https://gematik.de/fhir/epa-medication/StructureDefinition/context-extension').value");
    contextSearchParameter.setDescription("Liefert alle Ressourcen mit diesem Context Code");
    contextSearchParameter.setMultipleOr(false);
    contextSearchParameter.setMultipleAnd(false);

    fhirClient.getClient().create().resource(contextSearchParameter).execute();

    SearchParameter rxPrescriptionSearchParameter = new SearchParameter();
    rxPrescriptionSearchParameter.setId("rx-prescription-process-sp");
    rxPrescriptionSearchParameter.setUrl(
        "https://gematik.de/fhir/epa-medication/SearchParameter/rx-prescription-process-sp");
    rxPrescriptionSearchParameter.setVersion("1.2.0");
    rxPrescriptionSearchParameter.setName("RxPrescriptionProcessParameter");
    rxPrescriptionSearchParameter.setStatus(Enumerations.PublicationStatus.ACTIVE);
    rxPrescriptionSearchParameter.setDate(java.sql.Date.valueOf(LocalDate.parse("2025-09-12")));
    rxPrescriptionSearchParameter.setPublisher("gematik GmbH");
    rxPrescriptionSearchParameter.setDescription(
        "Returns Medications, MedicationDispenses or MedicationStatement with the Rx Prescription Process Identifier.");
    rxPrescriptionSearchParameter.setCode("rx-prescription");
    rxPrescriptionSearchParameter.addBase(MEDICATION);
    rxPrescriptionSearchParameter.addBase("MedicationDispense");
    rxPrescriptionSearchParameter.addBase(MEDICATION_STATEMENT);
    rxPrescriptionSearchParameter.setType(Enumerations.SearchParamType.TOKEN);

    rxPrescriptionSearchParameter.setExpression(
        "extension.where(url = 'https://gematik.de/fhir/epa-medication/StructureDefinition/rx-prescription-process-identifier-extension').value.value");
    rxPrescriptionSearchParameter.setXpathUsage(SearchParameter.XPathUsageType.NORMAL);

    fhirClient.getClient().create().resource(rxPrescriptionSearchParameter).execute();
  }

  @AfterAll
  void tearDown() {
    fhirServer.stop();
  }

  @Test
  @Order(0)
  void contextLoads() {
    assertThat(medicationApiEndpoint).isNotNull();
    assertThat(fhirClient).isNotNull();
    assertThat(restTestClient).isNotNull();
  }

  @TestFactory
  @Order(1)
  List<DynamicTest> shouldGetMedicationAsExpected() {
    return List.of(
        dynamicTest(
            "get medication by ID",
            () ->
                testMedicationApiHappyPath(
                    List.of("medication-example-1.json"),
                    "/services/epa/testdriver/api/v1/medication?id=%s",
                    1)),
        dynamicTest(
            "get medications by code",
            () ->
                testMedicationApiHappyPath(
                    List.of(
                        "medication-example-code-11210000-1.json",
                        "medication-example-code-11210000-2.json"),
                    "/services/epa/testdriver/api/v1/medication?code=11210000",
                    2)),
        dynamicTest(
            "get medications by lastUpdated",
            () ->
                testMedicationApiHappyPath(
                    List.of(),
                    "/services/epa/testdriver/api/v1/medication?lastUpdated=ge" + LocalDate.now(),
                    3)),
        dynamicTest(
            "get medications by ingredient code",
            () ->
                testMedicationApiHappyPath(
                    List.of("medication-example-with-context-emp.json"),
                    "/services/epa/testdriver/api/v1/medication?ingredientCode=24421",
                    1)),
        dynamicTest(
            "get medications by rx-prescription",
            () ->
                testMedicationApiHappyPath(
                    List.of("medication-example-with-prescription.json"),
                    "/services/epa/testdriver/api/v1/medication?prescription=160.000.000.000.000.00",
                    1)));
  }

  @TestFactory
  @Order(2)
  List<DynamicTest> shouldNotFailWhenNoMedicationFound() {
    return List.of(
        dynamicTest(
            "get not existing medication by ID ",
            () ->
                testMedicationApiFailurePath(
                    "/services/epa/testdriver/api/v1/medication?id=2345",
                    "No medication found for ID: 2345")),
        dynamicTest(
            "get not existing medications by code",
            () ->
                testMedicationApiFailurePath(
                    "/services/epa/testdriver/api/v1/medication?code=bla",
                    "No medication found for search params")));
  }

  @TestFactory
  @Order(4)
  List<DynamicTest> shouldGetMedicationRequestsAsExpected() {
    return List.of(
        dynamicTest(
            "get medication request by ID",
            () ->
                testMedicationRequestApiHappyPath(
                    List.of("medication-request-1.json"),
                    "/services/epa/testdriver/api/v1/medication-request?id=%s",
                    1)),
        dynamicTest(
            "get medication requests by status",
            () ->
                testMedicationRequestApiHappyPath(
                    List.of("medication-request-2.json"),
                    "/services/epa/testdriver/api/v1/medication-request?status=active",
                    2)),
        dynamicTest(
            "get medication requests by lastUpdatedAt",
            () ->
                testMedicationRequestApiHappyPath(
                    List.of(),
                    "/services/epa/testdriver/api/v1/medication-request?lastUpdated=ge"
                        + LocalDate.now(),
                    2)));
  }

  @TestFactory
  @Order(6)
  List<DynamicTest> shouldGetMedicationDispensesAsExpected() {
    return List.of(
        dynamicTest(
            "get medication dispense by ID",
            () ->
                testMedicationDispenseApiHappyPath(
                    List.of("medication-dispense-1.json"),
                    "/services/epa/testdriver/api/v1/medication-dispense?id=%s",
                    1)),
        dynamicTest(
            "get medication dispense by status",
            () ->
                testMedicationDispenseApiHappyPath(
                    List.of("medication-dispense-2.json"),
                    "/services/epa/testdriver/api/v1/medication-dispense?status=completed",
                    2)),
        dynamicTest(
            "get medication dispense by whenHandedOver",
            () ->
                testMedicationDispenseApiHappyPath(
                    List.of(),
                    "/services/epa/testdriver/api/v1/medication-dispense?whenhandedover=gt2024-01-13",
                    2)));
  }

  @Test
  @Order(7)
  void shouldReturnEmlAsFhir() {
    createFhirTransactionResource(List.of("eml-fhir-transaction.json"));
    var response =
        restTestClient
            .get()
            .uri(
                "/services/epa/testdriver/api/v1/medication/eml/fhir?lastUpdated=le"
                    + LocalDate.now())
            .exchange()
            .returnResult(GetMedicationListAsFhirResponseDTO.class);
    assertThat(response.getStatus().value()).isEqualTo(200);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody().getSuccess()).isTrue();
    assertThat(response.getResponseBody().getStatusMessage()).isBlank();
    assertThat(response.getResponseBody().getEml()).isNotBlank();

    var fhirResource = FhirUtils.fromString(response.getResponseBody().getEml());
    assertThat(fhirResource).isNotNull();
    assertThat(((Bundle) fhirResource).getEntry()).isNotEmpty();
  }

  @Test
  @Order(10)
  void getMedicationListAsFhirReturnsProperResponseWhenNoMedicationExists() {
    var response =
        restTestClient
            .get()
            .uri("/services/epa/testdriver/api/v1/medication/eml/fhir?status=inactive")
            .exchange()
            .returnResult(GetMedicationListAsFhirResponseDTO.class);
    assertThat(response.getStatus().value()).isEqualTo(200);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody().getSuccess()).isTrue();
    assertThat(response.getResponseBody().getStatusMessage()).isNotBlank();
  }

  @Test
  @Order(11)
  void shouldGetMedicationHistoryList() {
    // given
    var medicationId = medicationIds.getLast();

    // when - get medication history
    var response =
        restTestClient
            .get()
            .uri("/services/epa/testdriver/api/v1/medication/" + medicationId + "/history")
            .exchange()
            .returnResult(GetMedicationHistoryResponseDTO.class);

    // then
    assertThat(response.getStatus().value()).isEqualTo(200);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody().getSuccess()).isTrue();
    assertThat(response.getResponseBody().getStatusMessage()).isBlank();
    assertThat(response.getResponseBody().getMedications()).isNotEmpty();
  }

  @Test
  @Order(12)
  void shouldGetMedicationHistoryListWithXmlFormat() {
    // given - use an existing medication ID
    var medicationId = medicationIds.getFirst();

    // when - get medication history with XML format
    var response =
        restTestClient
            .get()
            .uri(
                "/services/epa/testdriver/api/v1/medication/"
                    + medicationId
                    + "/history?format=application/fhir+xml")
            .exchange()
            .returnResult(GetMedicationHistoryResponseDTO.class);

    // then
    assertThat(response.getStatus().value()).isEqualTo(200);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody().getSuccess()).isTrue();
    assertThat(response.getResponseBody().getMedications()).isNotEmpty();
    assertThat(response.getResponseBody().getMedications().getFirst()).contains("<Medication");
  }

  @Test
  @Order(13)
  void shouldReturnProperResponseWhenMedicationHistoryNotFound() {
    // given - non-existent medication ID
    var nonExistentId = "non-existent-id-999";

    // when
    var response =
        restTestClient
            .get()
            .uri("/services/epa/testdriver/api/v1/medication/" + nonExistentId + "/history")
            .exchange()
            .returnResult(GetMedicationHistoryResponseDTO.class);

    // then
    assertThat(response.getStatus().value()).isEqualTo(200);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody().getSuccess()).isTrue();
    assertThat(response.getResponseBody().getStatusMessage()).isNotBlank();
    assertThat(response.getResponseBody().getStatusMessage())
        .contains("No medication historyBundle found for ID");
    assertThat(response.getResponseBody().getMedications()).isEmpty();
  }

  private void testMedicationApiFailurePath(String url, String expectedStatusMessage) {
    var response =
        restTestClient.get().uri(url).exchange().returnResult(GetMedicationResponseDTO.class);

    assertThat(response.getStatus().value()).isEqualTo(200);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody().getSuccess()).isTrue();
    assertThat(response.getResponseBody().getStatusMessage()).isNotBlank();
    assertThat(response.getResponseBody().getStatusMessage()).contains(expectedStatusMessage);
    assertThat(response.getResponseBody().getMedications()).isEmpty();
  }

  private void testMedicationApiHappyPath(List<String> samples, String url, int expectedCount) {
    createFhirResource(samples, medicationIds);

    var id = medicationIds.getFirst();
    var response =
        restTestClient
            .get()
            .uri(url.formatted(id))
            .exchange()
            .returnResult(GetMedicationResponseDTO.class);

    assertThat(response.getStatus().value()).isEqualTo(200);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody().getSuccess()).isTrue();
    assertThat(response.getResponseBody().getStatusMessage()).isBlank();
    assertThat(response.getResponseBody().getMedications()).hasSize(expectedCount);
  }

  private void testMedicationRequestApiHappyPath(
      List<String> samples, String url, int expectedCount) {
    createFhirResource(samples, medicationRequestIds);

    var id = medicationRequestIds.getFirst();
    var response =
        restTestClient
            .get()
            .uri(url.formatted(id))
            .exchange()
            .returnResult(GetMedicationRequestListDTO.class);

    assertThat(response.getStatus().value()).isEqualTo(200);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody().getSuccess()).isTrue();
    assertThat(response.getResponseBody().getStatusMessage()).isBlank();
    assertThat(response.getResponseBody().getMedicationRequests()).hasSize(expectedCount);
  }

  private void testMedicationDispenseApiHappyPath(
      List<String> samples, String url, int expectedCount) {
    createFhirResource(samples, medicationDispenseIds);

    var id = medicationDispenseIds.getFirst();
    var response =
        restTestClient
            .get()
            .uri(url.formatted(id))
            .exchange()
            .returnResult(GetMedicationDispenseListDTO.class);

    assertThat(response.getStatus().value()).isEqualTo(200);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody().getSuccess()).isTrue();
    assertThat(response.getResponseBody().getStatusMessage()).isBlank();
    assertThat(response.getResponseBody().getMedicationDispenses()).hasSize(expectedCount);
  }

  @SneakyThrows
  private void createFhirResource(final List<String> fileNames, List<String> ids) {
    fhirClient.customizeSocketTimeout(30000);
    fileNames.forEach(
        fileName -> {
          try {
            var medicationAsString =
                FileUtils.readFileToString(
                    FileUtils.getFile("src/test/resources/medication/" + fileName),
                    StandardCharsets.UTF_8);
            MethodOutcome outcome =
                fhirClient.getClient().create().resource(medicationAsString).execute();
            assertThat(outcome.getCreated()).isTrue();
            assertThat(outcome.getId()).isNotNull();
            String resourceId = outcome.getId().getIdPart();
            ids.add(resourceId);
          } catch (Exception e) {
            fail(e.getMessage());
          }
        });
  }

  @SneakyThrows
  private void createFhirTransactionResource(final List<String> fileNames) {
    fhirClient.customizeSocketTimeout(30000);
    fileNames.forEach(
        fileName -> {
          try {
            var resourceAsString =
                FileUtils.readFileToString(
                    FileUtils.getFile("src/test/resources/medication/" + fileName),
                    StandardCharsets.UTF_8);
            final String response =
                fhirClient.getClient().transaction().withBundle(resourceAsString).execute();
            assertThat(response).isNotNull();
          } catch (Exception e) {
            fail(e.getMessage());
          }
        });
  }
}
