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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.gematik.epa.api.testdriver.medication.dto.AddEmpEntryInput;
import de.gematik.epa.api.testdriver.medication.dto.BatchEmpInput;
import de.gematik.epa.api.testdriver.medication.dto.EmpCommitInput;
import de.gematik.epa.api.testdriver.medication.dto.UpdateEmpEntryInput;
import de.gematik.epa.fhir.client.FhirClient;
import de.gematik.epa.medication.client.EmlRenderClient;
import de.gematik.epa.medication.client.RenderResponse;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmpServiceTest {
  private static final String INSURANT_ID = "X110435031";
  private FhirClient fhirClient;
  private EmlRenderClient emlRenderClient;
  private IParser jsonParser;
  private EmpService empService;

  @BeforeEach
  void setup() {
    fhirClient = mock(FhirClient.class);
    emlRenderClient = mock(EmlRenderClient.class);
    final FhirContext fhirContext = FhirContext.forR4();
    jsonParser = fhirContext.newJsonParser();
    when(fhirClient.getContext()).thenReturn(fhirContext);
    empService = new EmpService(fhirClient, emlRenderClient);
  }

  @Test
  void shouldAddEmpEntrySuccessfullyWithJsonFormat() {
    var requestId = UUID.randomUUID();
    var userAgent = "test-agent";

    var medicationRequest = new MedicationRequest();
    medicationRequest.setId("med-request-1");
    var medication = new Medication();
    medication.setId("medication-1");
    var organization = "{\"resourceType\":\"Organization\",\"id\":\"org-1\"}";
    var provenanceId = "provenance-1";

    var addEmpEntryInput = new AddEmpEntryInput();
    addEmpEntryInput.setMedicationRequest(jsonParser.encodeResourceToString(medicationRequest));
    addEmpEntryInput.setMedication(jsonParser.encodeResourceToString(medication));
    addEmpEntryInput.setOrganization(organization);
    addEmpEntryInput.setChronologyId(provenanceId);
    addEmpEntryInput.setFormat(AddEmpEntryInput.FormatEnum.APPLICATION_FHIR_JSON);

    var renderResponse = new RenderResponse().httpStatusCode(200).empResponse("success response");
    when(emlRenderClient.addEmpEntry(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(renderResponse);

    var result = empService.addEmpEntry(INSURANT_ID, requestId, userAgent, addEmpEntryInput);

    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getParameters()).isEqualTo("success response");
  }

  @Test
  void shouldAddEmpEntrySuccessfullyWithXmlFormat() {
    var requestId = UUID.randomUUID();
    var userAgent = "test-agent";

    var medicationRequest = new MedicationRequest();
    medicationRequest.setId("med-request-1");
    var medication = new Medication();
    medication.setId("medication-1");
    var organization = "{\"resourceType\":\"Organization\",\"id\":\"org-1\"}";
    var provenanceId = "provenance-1";

    var addEmpEntryInput = new AddEmpEntryInput();
    addEmpEntryInput.setMedicationRequest(jsonParser.encodeResourceToString(medicationRequest));
    addEmpEntryInput.setMedication(jsonParser.encodeResourceToString(medication));
    addEmpEntryInput.setOrganization(organization);
    addEmpEntryInput.setChronologyId(provenanceId);
    addEmpEntryInput.setFormat(AddEmpEntryInput.FormatEnum.APPLICATION_FHIR_XML);

    var renderResponse = new RenderResponse().httpStatusCode(200).empResponse("success response");
    when(emlRenderClient.addEmpEntry(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(renderResponse);

    var result = empService.addEmpEntry(INSURANT_ID, requestId, userAgent, addEmpEntryInput);

    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getParameters()).isEqualTo("success response");
  }

  @Test
  void shouldUpdateEmpEntrySuccessfullyWithJsonFormat() {
    var requestId = UUID.randomUUID();
    var userAgent = "test-agent";

    var medicationRequest = new MedicationRequest();
    medicationRequest.setId("med-request-1");
    var organization = "{\"resourceType\":\"Organization\",\"id\":\"org-1\"}";
    var provenanceId = "provenance-1";
    var medicationPlanId = "medication-plan-123";

    var updateEmpEntryInput = new UpdateEmpEntryInput();
    updateEmpEntryInput.setMedicationRequest(jsonParser.encodeResourceToString(medicationRequest));
    updateEmpEntryInput.setOrganization(organization);
    updateEmpEntryInput.setChronologyId(provenanceId);

    updateEmpEntryInput.setMedicationPlanId(medicationPlanId);
    updateEmpEntryInput.setFormat(UpdateEmpEntryInput.FormatEnum.APPLICATION_FHIR_JSON);

    var renderResponse = new RenderResponse().httpStatusCode(200).empResponse("success response");
    when(emlRenderClient.updateEmpEntry(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(renderResponse);

    var result = empService.updateEmpEntry(INSURANT_ID, requestId, userAgent, updateEmpEntryInput);

    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getParameters()).isEqualTo("success response");
  }

  @Test
  void shouldGetMedicationPlanLogsSuccessfully() {
    var requestId = UUID.randomUUID();
    var count = 10;
    var offset = 0;
    var format = "application/fhir+json";

    var expectedResponse = "{\"resourceType\":\"Bundle\",\"type\":\"searchset\"}";
    var renderResponse =
        new RenderResponse().httpStatusCode(200).medicationPlanLogs(expectedResponse);
    when(emlRenderClient.getMedicationPlanLogs(
            anyString(), anyString(), anyInt(), anyInt(), anyString()))
        .thenReturn(renderResponse);

    var result = empService.getMedicationPlanLogs(INSURANT_ID, requestId, count, offset, format);

    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getMedicationPlanLogs()).isEqualTo(expectedResponse);
    assertThat(result.getStatusMessage()).isNull();
  }

  @Test
  void shouldHandleErrorWhenGetMedicationPlanLogsFails() {
    var requestId = UUID.randomUUID();
    var count = 10;
    var offset = 0;
    var format = "application/fhir+json";

    var renderResponse = new RenderResponse().httpStatusCode(400).errorMessage("Bad request error");
    when(emlRenderClient.getMedicationPlanLogs(
            anyString(), anyString(), anyInt(), anyInt(), anyString()))
        .thenReturn(renderResponse);

    var result = empService.getMedicationPlanLogs(INSURANT_ID, requestId, count, offset, format);

    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).isEqualTo("Bad request error");
  }

  @Test
  void shouldHandleIllegalArgumentExceptionWhenGetMedicationPlanLogs() {
    var requestId = UUID.randomUUID();
    var count = 10;
    var offset = 0;
    var format = "invalid-format";

    when(emlRenderClient.getMedicationPlanLogs(
            anyString(), anyString(), anyInt(), anyInt(), anyString()))
        .thenThrow(new IllegalArgumentException("Invalid format"));

    var result = empService.getMedicationPlanLogs(INSURANT_ID, requestId, count, offset, format);

    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).contains("Unsupported format");
  }

  @Test
  void shouldGetMedicationPlanSuccessfully() {
    var requestId = UUID.randomUUID();
    var format = "application/fhir+json";

    var expectedResponse = "{\"resourceType\":\"Bundle\",\"type\":\"searchset\"}";
    var renderResponse = new RenderResponse().httpStatusCode(200).medicationPlan(expectedResponse);
    when(emlRenderClient.getEmp(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(renderResponse);

    var result = empService.getEmp(format, INSURANT_ID, requestId, "1234");

    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getMedicationPlan()).isEqualTo(expectedResponse);
    assertThat(result.getStatusMessage()).isNull();
  }

  @Test
  void shouldHandleErrorWhenGetMedicationPlanFails() {
    var requestId = UUID.randomUUID();
    var format = "application/fhir+json";

    var renderResponse = new RenderResponse().httpStatusCode(400).errorMessage("Bad request error");
    when(emlRenderClient.getEmp(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(renderResponse);

    var result = empService.getEmp(format, INSURANT_ID, requestId, "123");

    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).isEqualTo("Bad request error");
  }

  @Test
  void shouldHandleIllegalArgumentExceptionWhenGetMedicationPlan() {
    var requestId = UUID.randomUUID();
    var format = "invalid-format";

    when(emlRenderClient.getEmp(anyString(), anyString(), anyString(), anyString()))
        .thenThrow(new IllegalArgumentException("Invalid format"));

    var result = empService.getEmp(format, INSURANT_ID, requestId, null);

    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).contains("Unsupported format");
  }

  @Test
  void shouldGetEmpWithXmlFormat() {
    var requestId = UUID.randomUUID();
    var format = "application/fhir+xml";

    var expectedResponse = "<Bundle xmlns=\"http://hl7.org/fhir\"/>";
    var renderResponse = new RenderResponse().httpStatusCode(200).medicationPlan(expectedResponse);
    when(emlRenderClient.getEmp(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(renderResponse);

    var result = empService.getEmp(format, INSURANT_ID, requestId, "123");

    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getMedicationPlan()).isEqualTo(expectedResponse);
    assertThat(result.getStatusMessage()).isNull();
  }

  @Test
  void shouldBatchEmpSuccessfullyWithJsonFormat() {
    var requestId = UUID.randomUUID();
    var userAgent = "test-agent";

    var medicationForAdd = new Medication();
    medicationForAdd.setId("medication-1");
    var medicationRequestForAdd = new MedicationRequest();
    medicationRequestForAdd.setId("med-request-add-1");
    var medicationRequestForUpdate = new MedicationRequest();
    medicationRequestForUpdate.setId("med-request-update-1");
    var organization = "{\"resourceType\":\"Organization\",\"id\":\"org-1\"}";
    var chronologyId = "provenance-1";
    var medicationPlanId = "medication-plan-123";

    var batchEmpInput = new BatchEmpInput();
    AddEmpEntryInput addEmpEntryInput = new AddEmpEntryInput();
    addEmpEntryInput.setMedication(jsonParser.encodeResourceToString(medicationForAdd));
    addEmpEntryInput.setMedicationRequest(
        jsonParser.encodeResourceToString(medicationRequestForAdd));
    batchEmpInput.setEmpAdds(List.of(addEmpEntryInput));

    UpdateEmpEntryInput updateEmpEntryInput = new UpdateEmpEntryInput();
    updateEmpEntryInput.setMedicationRequest(
        jsonParser.encodeResourceToString(medicationRequestForUpdate));
    updateEmpEntryInput.setMedicationPlanId(medicationPlanId);
    batchEmpInput.setEmpUpdates(List.of(updateEmpEntryInput));

    batchEmpInput.setEmpCommit(new EmpCommitInput().chronologyId(chronologyId));
    batchEmpInput.setOrganization(organization);
    batchEmpInput.setFormat(BatchEmpInput.FormatEnum.APPLICATION_FHIR_JSON);

    var renderResponse = new RenderResponse().httpStatusCode(200).empResponse("success response");
    when(emlRenderClient.batchEmp(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(renderResponse);

    var result = empService.batchEmp(INSURANT_ID, requestId, userAgent, batchEmpInput);

    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getBundle()).isEqualTo("success response");
  }

  @Test
  void shouldHandleServerErrorWhenGetEmpFails() {
    var requestId = UUID.randomUUID();
    var format = "application/fhir+json";

    var renderResponse =
        new RenderResponse().httpStatusCode(500).errorMessage("Internal Server Error");
    when(emlRenderClient.getEmp(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(renderResponse);

    var result = empService.getEmp(format, INSURANT_ID, requestId, "123");

    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).isEqualTo("Internal Server Error");
    assertThat(result.getMedicationPlan()).isNull();
  }
}
