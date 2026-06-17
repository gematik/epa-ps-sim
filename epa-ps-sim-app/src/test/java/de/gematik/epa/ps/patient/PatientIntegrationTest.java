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
package de.gematik.epa.ps.patient;

import static de.gematik.epa.unit.util.TestDataFactory.KVNR;
import static de.gematik.epa.unit.util.TestDataFactory.X_INSURANTID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.gematik.epa.api.testdriver.patient.dto.PatientInformationDTO;
import de.gematik.epa.api.testdriver.patient.dto.PatientInformationResponseDTO;
import de.gematik.epa.fhir.client.FhirClient;
import de.gematik.epa.ps.fhir.config.TestFhirClientProvider;
import de.gematik.epa.unit.TestDocumentClientConfiguration;
import de.gematik.epa.unit.TestKonnektorClientConfiguration;
import de.gematik.epa.utils.FhirUtils;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@AutoConfigureRestTestClient
class PatientIntegrationTest {

  @Autowired FhirClient fhirClient;
  @Autowired RestTestClient restTestClient;

  private static final int PORT = 8080;
  private static final DockerImageName fhirDockerImage =
      DockerImageName.parse("hapiproject/hapi:latest");
  private static final GenericContainer<?> fhirServer =
      new GenericContainer<>(fhirDockerImage)
          .withExposedPorts(PORT)
          .waitingFor(
              Wait.forHttp("/fhir/metadata")
                  .forStatusCode(200)
                  .withStartupTimeout(Duration.ofMinutes(5L)));
  private static final String PATIENT_JSON =
      """
    {
      "resourceType": "Patient",
      "meta": {"profile":["https://gematik.de/fhir/epa/StructureDefinition/epa-patient|1.3.0"]},
      "identifier": [{"system": "http://fhir.de/sid/gkv/kvid-10", "value": "%s"}],
      "name": [{"use":"official", "family": "NachnameA", "given": ["VornameA"]}],
      "birthDate": "1964-08-12"
    }
    """
          .formatted(KVNR);

  @BeforeAll
  static void setUpAll() {
    fhirServer.start();
  }

  @BeforeEach
  void setUp() {
    var serverUrl =
        "http://" + fhirServer.getHost() + ":" + fhirServer.getMappedPort(PORT) + "/fhir";
    fhirClient.setServerUrl(serverUrl, "PS-SIM");
    FhirUtils.setJsonParser(fhirClient.getContext().newJsonParser());

    assertThat(restTestClient).isNotNull();
  }

  @Test
  void testCreateOrUpdatePatient() {
    PatientInformationDTO patientInformationDTO = new PatientInformationDTO();
    patientInformationDTO.setPatient(PATIENT_JSON);
    var response =
        restTestClient
            .put()
            .uri("/services/epa/testdriver/api/v1/patient/fhir/Patient/")
            .contentType(MediaType.APPLICATION_JSON)
            .header(X_INSURANTID, KVNR)
            .body(patientInformationDTO)
            .exchange()
            .returnResult(PatientInformationResponseDTO.class);

    assertEquals(HttpStatus.OK, response.getStatus());
  }
}
