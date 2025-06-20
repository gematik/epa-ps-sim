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
package de.gematik.epa.ps.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import ca.uhn.fhir.rest.api.MethodOutcome;
import de.gematik.epa.api.testdriver.medication.dto.GetMedicationDispenseListDTO;
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
import java.util.List;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
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
class MedicationApiEndpointIntegrationTest {

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
  @Autowired MedicationApiEndpoint medicationApiEndpoint;
  @Autowired FhirClient fhirClient;
  @Autowired TestRestTemplate testRestTemplate;

  @BeforeAll
  void setUp() {
    fhirServer.start();
    var serverUrl =
        "http://" + fhirServer.getHost() + ":" + fhirServer.getMappedPort(PORT) + "/fhir";
    fhirClient.setServerUrl(serverUrl, "PS-SIM");
    FhirUtils.setJsonParser(fhirClient.getContext().newJsonParser());
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
    assertThat(testRestTemplate).isNotNull();
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
                    "/services/epa/testdriver/api/v1/medication?id=1",
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
                    3)));
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
                    "/services/epa/testdriver/api/v1/medication-request?id=4",
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
                    "/services/epa/testdriver/api/v1/medication-dispense?id=6",
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
        testRestTemplate.getForEntity(
            "/services/epa/testdriver/api/v1/medication/eml/fhir?lastUpdated=le" + LocalDate.now(),
            GetMedicationListAsFhirResponseDTO.class);
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getSuccess()).isTrue();
    assertThat(response.getBody().getStatusMessage()).isBlank();
    assertThat(response.getBody().getEml()).isNotBlank();

    var fhirResource = FhirUtils.fromString(response.getBody().getEml());
    assertThat(fhirResource).isNotNull();
    assertThat(((Bundle) fhirResource).getEntry()).isNotEmpty();
  }

  @Test
  @Order(10)
  void getMedicationListAsFhirReturnsProperResponseWhenNoMedicationExists() {
    var response =
        testRestTemplate.getForEntity(
            "/services/epa/testdriver/api/v1/medication/eml/fhir?status=inactive",
            GetMedicationListAsFhirResponseDTO.class);
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getSuccess()).isTrue();
    assertThat(response.getBody().getStatusMessage()).isNotBlank();
  }

  private void testMedicationApiFailurePath(String url, String expectedStatusMessage) {
    var response = testRestTemplate.getForEntity(url, GetMedicationResponseDTO.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getSuccess()).isTrue();
    assertThat(response.getBody().getStatusMessage()).isNotBlank();
    assertThat(response.getBody().getStatusMessage()).contains(expectedStatusMessage);
    assertThat(response.getBody().getMedications()).isEmpty();
  }

  private void testMedicationApiHappyPath(List<String> samples, String url, int expectedCount) {
    createFhirResource(samples);
    var response = testRestTemplate.getForEntity(url, GetMedicationResponseDTO.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getSuccess()).isTrue();
    assertThat(response.getBody().getStatusMessage()).isBlank();
    assertThat(response.getBody().getMedications()).hasSize(expectedCount);
  }

  private void testMedicationRequestApiHappyPath(
      List<String> samples, String url, int expectedCount) {
    createFhirResource(samples);
    var response = testRestTemplate.getForEntity(url, GetMedicationRequestListDTO.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getSuccess()).isTrue();
    assertThat(response.getBody().getStatusMessage()).isBlank();
    assertThat(response.getBody().getMedicationRequests()).hasSize(expectedCount);
  }

  private void testMedicationDispenseApiHappyPath(
      List<String> samples, String url, int expectedCount) {
    createFhirResource(samples);
    var response = testRestTemplate.getForEntity(url, GetMedicationDispenseListDTO.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getSuccess()).isTrue();
    assertThat(response.getBody().getStatusMessage()).isBlank();
    assertThat(response.getBody().getMedicationDispenses()).hasSize(expectedCount);
  }

  @SneakyThrows
  private void createFhirResource(final List<String> fileNames) {
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
