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
package de.gematik.epa.ps.endpoint;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static de.gematik.epa.unit.util.TestDataFactory.*;
import static de.gematik.epa.unit.util.TestDataFactory.USER_AGENT;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.epa.api.testdriver.medication.dto.AddEmpEntryInput;
import de.gematik.epa.api.testdriver.medication.dto.UpdateEmpEntryInput;
import de.gematik.epa.ps.utils.AbstractIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EmpApiEndpointIntegrationTest extends AbstractIntegrationTest {

  private final String encodedOrganization = "eyJyZXNvdXJjZVR5cGUiOiJPcmdhbml6YXRpb24ifQ==";
  @Autowired private EmpApiEndpoint empApiEndpoint;

  @Test
  void contextLoads() {
    assertThat(empApiEndpoint).isNotNull();
  }

  @BeforeEach
  void setup() {
    mockEmlRender.resetAll();
    mockEmlRender.resetRequests();
  }

  @SneakyThrows
  @Test
  void shouldAddEmpEntrySuccessfully() {
    final AddEmpEntryInput addEmpEntryInput = new AddEmpEntryInput();
    addEmpEntryInput.setMedicationRequest("{\"resourceType\":\"MedicationRequest\"}");
    addEmpEntryInput.setMedication("{\"resourceType\":\"Medication\"}");
    addEmpEntryInput.setOrganization("{\"resourceType\":\"Organization\"}");
    addEmpEntryInput.setFormat(AddEmpEntryInput.FormatEnum.APPLICATION_FHIR_JSON);

    var bodyAsJson =
        FileUtils.readFileToString(
            FileUtils.getFile("src/test/resources/medication/add-emp-input.json"),
            StandardCharsets.UTF_8);
    mockEmlRender.stubFor(
        post(urlEqualTo(
                "/epa/medication/api/v1/fhir/$add-emp-entry?_format=application/fhir%2Bjson"))
            .withHeader(X_INSURANTID, equalTo(KVNR))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .withHeader(X_REQUESTING_ORGANIZATION, equalTo(encodedOrganization))
            .withHeader(ACCEPT_HEADER, equalTo(ACCEPT_FHIR_JSON))
            .withHeader(CONTENT_TYPE_HEADER, equalTo(ACCEPT_FHIR_JSON))
            .withRequestBody(equalToJson(bodyAsJson))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE_HEADER, ACCEPT_FHIR_JSON)
                    .withHeader(ACCEPT_HEADER, ACCEPT_FHIR_JSON)
                    .withBody("{\"httpStatusCode\":200,\"empResponse\":\"success response\"}")));

    var response =
        empApiEndpoint.addEmpEntry(KVNR, UUID.randomUUID(), USER_AGENT, addEmpEntryInput);

    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getParameters()).isNotEmpty();
  }

  @SneakyThrows
  @Test
  void addEmpEntryHandles400Response() {
    final AddEmpEntryInput addEmpEntryInput = new AddEmpEntryInput();
    addEmpEntryInput.setMedicationRequest("{\"resourceType\":\"MedicationRequest\"}");
    addEmpEntryInput.setMedication("{\"resourceType\":\"Medication\"}");
    addEmpEntryInput.setOrganization("{\"resourceType\":\"Organization\"}");
    addEmpEntryInput.setFormat(AddEmpEntryInput.FormatEnum.APPLICATION_FHIR_JSON);

    var bodyAsJson =
        FileUtils.readFileToString(
            FileUtils.getFile("src/test/resources/medication/add-emp-input.json"),
            StandardCharsets.UTF_8);

    String errorOutcome =
        """
        {"resourceType" : "OperationOutcome", "id" : "d38e9660-eafb-453f-b105-d1bc21be44f1",  "issue" : ["Error parsing resource (Unknown Content)" ]}
        """;
    mockEmlRender.stubFor(
        post(urlEqualTo(
                "/epa/medication/api/v1/fhir/$add-emp-entry?_format=application/fhir%2Bjson"))
            .withHeader(X_INSURANTID, equalTo(KVNR))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .withHeader(X_REQUESTING_ORGANIZATION, equalTo(encodedOrganization))
            .withHeader(ACCEPT_HEADER, equalTo(ACCEPT_FHIR_JSON))
            .withHeader(CONTENT_TYPE_HEADER, equalTo(ACCEPT_FHIR_JSON))
            .withRequestBody(equalToJson(bodyAsJson))
            .willReturn(
                aResponse()
                    .withStatus(400)
                    .withHeader(CONTENT_TYPE_HEADER, ACCEPT_FHIR_JSON)
                    .withHeader(ACCEPT_HEADER, ACCEPT_FHIR_JSON)
                    .withBody(errorOutcome)));

    var response =
        empApiEndpoint.addEmpEntry(KVNR, UUID.randomUUID(), USER_AGENT, addEmpEntryInput);
    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getStatusMessage()).isNotEmpty().contains("Unknown Content");
  }

  @SneakyThrows
  @Test
  void shouldUpdateEmpEntrySuccessfully() {
    final UpdateEmpEntryInput updateEmpEntryInput = new UpdateEmpEntryInput();
    updateEmpEntryInput.setMedicationRequest("{\"resourceType\":\"MedicationRequest\"}");
    updateEmpEntryInput.setMedicationPlanId("881f3c6d-20e6-443e-b7dc-580a40fa3d14");
    updateEmpEntryInput.setChronologyId("221f3c6d-20e6-443e-b7dc-580a40fa3d14");
    updateEmpEntryInput.setOrganization("{\"resourceType\":\"Organization\"}");
    updateEmpEntryInput.setFormat(UpdateEmpEntryInput.FormatEnum.APPLICATION_FHIR_JSON);

    var bodyAsJson =
        FileUtils.readFileToString(
            FileUtils.getFile("src/test/resources/medication/update-emp-input.json"),
            StandardCharsets.UTF_8);
    mockEmlRender.stubFor(
        post(urlEqualTo(
                "/epa/medication/api/v1/fhir/$update-emp-entry?_format=application/fhir%2Bjson"))
            .withHeader(X_INSURANTID, equalTo(KVNR))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .withHeader(X_REQUESTING_ORGANIZATION, equalTo(encodedOrganization))
            .withHeader(ACCEPT_HEADER, equalTo(ACCEPT_FHIR_JSON))
            .withHeader(CONTENT_TYPE_HEADER, equalTo(ACCEPT_FHIR_JSON))
            .withRequestBody(equalToJson(bodyAsJson))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE_HEADER, ACCEPT_FHIR_JSON)
                    .withHeader(ACCEPT_HEADER, ACCEPT_FHIR_JSON)
                    .withBody("{\"httpStatusCode\":200,\"empResponse\":\"success response\"}")));

    var response =
        empApiEndpoint.updateEmpEntry(KVNR, UUID.randomUUID(), USER_AGENT, updateEmpEntryInput);

    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getParameters()).isNotEmpty();
  }

  @SneakyThrows
  @Test
  void updateEmpEntryHandles400Response() {
    final UpdateEmpEntryInput updateEmpEntryInput = new UpdateEmpEntryInput();
    updateEmpEntryInput.setMedicationRequest("{\"resourceType\":\"MedicationRequest\"}");
    updateEmpEntryInput.setMedicationPlanId("881f3c6d-20e6-443e-b7dc-580a40fa3d14");
    updateEmpEntryInput.setChronologyId("221f3c6d-20e6-443e-b7dc-580a40fa3d14");
    updateEmpEntryInput.setOrganization("{\"resourceType\":\"Organization\"}");
    updateEmpEntryInput.setFormat(UpdateEmpEntryInput.FormatEnum.APPLICATION_FHIR_JSON);

    var bodyAsJson =
        FileUtils.readFileToString(
            FileUtils.getFile("src/test/resources/medication/update-emp-input.json"),
            StandardCharsets.UTF_8);

    String errorOutcome =
        """
                {"resourceType" : "OperationOutcome", "id" : "d38e9660-eafb-453f-b105-d1bc21be44f1",  "issue" : ["Error parsing resource (Unknown Content)" ]}
                """;
    mockEmlRender.stubFor(
        post(urlEqualTo(
                "/epa/medication/api/v1/fhir/$update-emp-entry?_format=application/fhir%2Bjson"))
            .withHeader(X_INSURANTID, equalTo(KVNR))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .withHeader(X_REQUESTING_ORGANIZATION, equalTo(encodedOrganization))
            .withHeader(ACCEPT_HEADER, equalTo(ACCEPT_FHIR_JSON))
            .withHeader(CONTENT_TYPE_HEADER, equalTo(ACCEPT_FHIR_JSON))
            .withRequestBody(equalToJson(bodyAsJson))
            .willReturn(
                aResponse()
                    .withStatus(400)
                    .withHeader(CONTENT_TYPE_HEADER, ACCEPT_FHIR_JSON)
                    .withHeader(ACCEPT_HEADER, ACCEPT_FHIR_JSON)
                    .withBody(errorOutcome)));

    var response =
        empApiEndpoint.updateEmpEntry(KVNR, UUID.randomUUID(), USER_AGENT, updateEmpEntryInput);
    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getStatusMessage()).isNotEmpty().contains("Unknown Content");
  }

  @SneakyThrows
  @Test
  void shouldGetMedicationPlanLogsSuccessfully() {
    var insurantId = KVNR;
    var requestId = UUID.randomUUID();
    var format = "application/fhir+json";

    var bodyAsJson =
        FileUtils.readFileToString(
            FileUtils.getFile("src/test/resources/medication/medication-plan-logs.json"),
            StandardCharsets.UTF_8);
    mockEmlRender.stubFor(
        get(urlEqualTo(
                "/epa/medication/api/v1/fhir/$medication-plan-log?_format=application/fhir%2Bjson"))
            .withHeader(X_INSURANTID, equalTo(insurantId))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .withHeader(ACCEPT_HEADER, equalTo(ACCEPT_FHIR_JSON))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE_HEADER, ACCEPT_FHIR_JSON)
                    .withHeader(ACCEPT_HEADER, ACCEPT_FHIR_JSON)
                    .withBody(bodyAsJson)));

    var response = empApiEndpoint.getMedicationPlanLogs(insurantId, requestId, null, null, format);

    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getMedicationPlanLogs()).isNotEmpty().contains("Bundle");
  }

  @Test
  void getMedicationPlanLogsHandles400Response() {
    var insurantId = KVNR;
    var requestId = UUID.randomUUID();
    var format = "application/fhir+json";

    String errorOutcome =
        """
        {"errorCode" : "ivalidQueryParam", "errorDetail" : "Invalid query param!" }
        """;
    mockEmlRender.stubFor(
        get(urlEqualTo(
                "/epa/medication/api/v1/fhir/$medication-plan-log?_format=application/fhir%2Bjson"))
            .withHeader(X_INSURANTID, equalTo(insurantId))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .withHeader(ACCEPT_HEADER, equalTo(ACCEPT_FHIR_JSON))
            .willReturn(
                aResponse()
                    .withStatus(400)
                    .withHeader(CONTENT_TYPE_HEADER, ACCEPT_FHIR_JSON)
                    .withHeader(ACCEPT_HEADER, ACCEPT_FHIR_JSON)
                    .withBody(errorOutcome)));

    var response = empApiEndpoint.getMedicationPlanLogs(insurantId, requestId, null, null, format);
    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getStatusMessage()).isNotEmpty().contains("ivalidQueryParam");
  }
}
