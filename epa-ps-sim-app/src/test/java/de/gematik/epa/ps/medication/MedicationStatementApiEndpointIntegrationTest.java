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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static de.gematik.epa.unit.util.TestDataFactory.*;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.epa.api.testdriver.medication.dto.AddEmlEntryInput;
import de.gematik.epa.api.testdriver.medication.dto.CancelEmlEntryInput;
import de.gematik.epa.api.testdriver.medication.dto.CancelEmlEntryInput.FormatEnum;
import de.gematik.epa.api.testdriver.medication.dto.LinkEmpInput;
import de.gematik.epa.ps.endpoint.MedicationStatementApiEndpoint;
import de.gematik.epa.ps.utils.AbstractIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MedicationStatementApiEndpointIntegrationTest extends AbstractIntegrationTest {

  private final String encodedOrganization = "eyJyZXNvdXJjZVR5cGUiOiJPcmdhbml6YXRpb24ifQ==";
  @Autowired private MedicationStatementApiEndpoint medicationStatementApiEndpoint;

  @Test
  void contextLoads() {
    assertThat(medicationStatementApiEndpoint).isNotNull();
  }

  @BeforeEach
  void setup() {
    mockEmlRender.resetAll();
    mockEmlRender.resetRequests();
  }

  @SneakyThrows
  @Test
  void shouldAddEmlEntrySuccessfully() {
    final AddEmlEntryInput addEmlEntryInput = new AddEmlEntryInput();
    addEmlEntryInput.setMedicationStatement("{\"resourceType\":\"MedicationStatement\"}");
    addEmlEntryInput.setMedication("{\"resourceType\":\"Medication\"}");
    addEmlEntryInput.setOrganization("{\"resourceType\":\"Organization\"}");
    addEmlEntryInput.setFormat(AddEmlEntryInput.FormatEnum.APPLICATION_FHIR_JSON);

    var bodyAsJson =
        FileUtils.readFileToString(
            FileUtils.getFile("src/test/resources/medication/add-eml-input.json"),
            StandardCharsets.UTF_8);
    mockEmlRender.stubFor(
        post(urlEqualTo(
                "/epa/medication/api/v1/fhir/MedicationStatement/$add-eml-entry?_format=application/fhir%2Bjson"))
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
        medicationStatementApiEndpoint.addEmlEntry(
            KVNR, UUID.randomUUID(), USER_AGENT, addEmlEntryInput);

    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getParameters()).isNotEmpty();
  }

  @SneakyThrows
  @Test
  void addEmlEntryHandles400Response() {
    final AddEmlEntryInput addEmlEntryInput = new AddEmlEntryInput();
    addEmlEntryInput.setMedicationStatement("{\"resourceType\":\"MedicationStatement\"}");
    addEmlEntryInput.setMedication("{\"resourceType\":\"Medication\"}");
    addEmlEntryInput.setOrganization("{\"resourceType\":\"Organization\"}");
    addEmlEntryInput.setFormat(AddEmlEntryInput.FormatEnum.APPLICATION_FHIR_JSON);

    var bodyAsJson =
        FileUtils.readFileToString(
            FileUtils.getFile("src/test/resources/medication/add-eml-input.json"),
            StandardCharsets.UTF_8);
    String errorOutcome =
"""
{"resourceType" : "OperationOutcome", "id" : "d38e9660-eafb-453f-b105-d1bc21be44f1",  "issue" : ["Error parsing resource (Unknown Content)" ]}
""";
    mockEmlRender.stubFor(
        post(urlEqualTo(
                "/epa/medication/api/v1/fhir/MedicationStatement/$add-eml-entry?_format=application/fhir%2Bjson"))
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
        medicationStatementApiEndpoint.addEmlEntry(
            KVNR, UUID.randomUUID(), USER_AGENT, addEmlEntryInput);

    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getStatusMessage()).isNotEmpty().contains("Unknown Content");
  }

  @SneakyThrows
  @Test
  void shouldCancelEmlEntrySuccessfully() {
    final CancelEmlEntryInput cancelEmlEntryInput = new CancelEmlEntryInput();
    cancelEmlEntryInput.setOrganization("{\"resourceType\":\"Organization\"}");
    cancelEmlEntryInput.setFormat(FormatEnum.APPLICATION_FHIR_JSON);
    mockEmlRender.stubFor(
        post(urlEqualTo(
                "/epa/medication/api/v1/fhir/MedicationStatement/1/$cancel-eml-entry?_format=application/fhir%2Bjson"))
            .withHeader(X_INSURANTID, equalTo(KVNR))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .withHeader(X_REQUESTING_ORGANIZATION, equalTo(encodedOrganization))
            .withHeader(ACCEPT_HEADER, equalTo(ACCEPT_FHIR_JSON))
            .withHeader(CONTENT_TYPE_HEADER, equalTo(ACCEPT_FHIR_JSON))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE_HEADER, ACCEPT_FHIR_JSON)
                    .withHeader(ACCEPT_HEADER, ACCEPT_FHIR_JSON)
                    .withBody("{\"httpStatusCode\":200,\"empResponse\":\"success response\"}")));

    var response =
        medicationStatementApiEndpoint.cancelEmlEntry(
            KVNR, UUID.randomUUID(), "1", USER_AGENT, cancelEmlEntryInput);

    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getParameters()).isNotEmpty();
  }

  @SneakyThrows
  @Test
  void cancelEmlEntryHandles404Response() {
    final CancelEmlEntryInput cancelEmlEntryInput = new CancelEmlEntryInput();
    cancelEmlEntryInput.setOrganization("{\"resourceType\":\"Organization\"}");
    cancelEmlEntryInput.setFormat(CancelEmlEntryInput.FormatEnum.APPLICATION_FHIR_JSON);

    String errorOutcome =
        """
            {
                "errorCode": "not-found",
                "errorDetail": "MSG_RESOURCE_ID_FAIL"
            }""";
    mockEmlRender.stubFor(
        post(urlEqualTo(
                "/epa/medication/api/v1/fhir/MedicationStatement/5/$cancel-eml-entry?_format=application/fhir%2Bjson"))
            .withHeader(X_INSURANTID, equalTo(KVNR))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .withHeader(X_REQUESTING_ORGANIZATION, equalTo(encodedOrganization))
            .withHeader(ACCEPT_HEADER, equalTo(ACCEPT_FHIR_JSON))
            .withHeader(CONTENT_TYPE_HEADER, equalTo(ACCEPT_FHIR_JSON))
            .willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader(CONTENT_TYPE_HEADER, ACCEPT_FHIR_JSON)
                    .withHeader(ACCEPT_HEADER, ACCEPT_FHIR_JSON)
                    .withBody(errorOutcome)));

    var response =
        medicationStatementApiEndpoint.cancelEmlEntry(
            KVNR, UUID.randomUUID(), "5", USER_AGENT, cancelEmlEntryInput);

    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getStatusMessage()).isNotEmpty().contains("MSG_RESOURCE_ID_FAIL");
  }

  @Test
  void shouldLinkEmpSuccessfully() {
    var bodyAsJson =
        """
    {
                 "resourceType" : "Parameters",
                 "id" : "${json-unit.any-string}",
                 "meta" : {
                   "profile" : ["https://gematik.de/fhir/epa-medication/StructureDefinition/epa-op-link-emp-entry-parameters"
                   ]
                 },
                 "parameter" : [
                   {
                     "name" : "medicationPlanIdentifier",
                     "valueIdentifier" : {
                       "system" : "https://gematik.de/fhir/sid/emp-identifier",
                       "value" : "881f3c6d-20e6-443e-b7dc-580a40fa3d14"
                     }
                   }
                 ]
               }
    """;
    mockEmlRender.stubFor(
        post(urlEqualTo(
                "/epa/medication/api/v1/fhir/MedicationStatement/1/$link-emp?_format=application/fhir%2Bjson"))
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
                    .withBody("{\"resourceType\":Parameters}")));

    LinkEmpInput input = new LinkEmpInput();
    input.setOrganization("{\"resourceType\":\"Organization\"}");
    input.setFormat(LinkEmpInput.FormatEnum.APPLICATION_FHIR_JSON);
    input.setMedicationPlanId("881f3c6d-20e6-443e-b7dc-580a40fa3d14");
    var response =
        medicationStatementApiEndpoint.linkEmp(KVNR, UUID.randomUUID(), "1", USER_AGENT, input);

    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getParameters()).isNotEmpty();
  }
}
