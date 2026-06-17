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
package de.gematik.epa.api.testdriver.impl;

import static de.gematik.epa.unit.util.TestDataFactory.KVNR;
import static de.gematik.epa.unit.util.TestDataFactory.USER_AGENT;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.epa.api.testdriver.medication.dto.*;
import de.gematik.epa.medication.EmpService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmpApiImplTest {

  private EmpService empService;
  private EmpApiImpl empApi;

  @BeforeEach
  void setUp() {
    empService = mock(EmpService.class);
    empApi = new EmpApiImpl(empService);
  }

  @Test
  void shouldAddEmpEntrySuccessfully() {
    UUID requestId = UUID.randomUUID();
    var addEmpEntryInput = new AddEmpEntryInput();
    addEmpEntryInput.setMedicationRequest("{\"resourceType\":\"MedicationRequest\"}");
    addEmpEntryInput.setMedication("{\"resourceType\":\"Medication\"}");
    addEmpEntryInput.setOrganization("{\"resourceType\":\"Organization\"}");

    var expectedResponse = new AddEmpEntryResponseDTO();
    expectedResponse.setSuccess(true);
    expectedResponse.setParameters("success response");

    when(empService.addEmpEntry(KVNR, requestId, USER_AGENT, addEmpEntryInput))
        .thenReturn(expectedResponse);

    var result = empApi.addEmpEntry(KVNR, requestId, USER_AGENT, addEmpEntryInput);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getParameters()).isEqualTo("success response");
    verify(empService).addEmpEntry(KVNR, requestId, USER_AGENT, addEmpEntryInput);
  }

  @Test
  void shouldHandleAddEmpEntryFailure() {
    UUID requestId = UUID.randomUUID();
    var addEmpEntryInput = new AddEmpEntryInput();

    var expectedResponse = new AddEmpEntryResponseDTO();
    expectedResponse.setSuccess(false);
    expectedResponse.setStatusMessage("Bad Request");

    when(empService.addEmpEntry(KVNR, requestId, USER_AGENT, addEmpEntryInput))
        .thenReturn(expectedResponse);

    var result = empApi.addEmpEntry(KVNR, requestId, USER_AGENT, addEmpEntryInput);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).isEqualTo("Bad Request");
    verify(empService).addEmpEntry(KVNR, requestId, USER_AGENT, addEmpEntryInput);
  }

  @Test
  void shouldUpdateEmpEntrySuccessfully() {
    UUID requestId = UUID.randomUUID();
    var updateEmpEntryInput = new UpdateEmpEntryInput();
    updateEmpEntryInput.setMedicationRequest("{\"resourceType\":\"MedicationRequest\"}");
    updateEmpEntryInput.setMedicationPlanId("881f3c6d-20e6-443e-b7dc-580a40fa3d14");
    updateEmpEntryInput.setChronologyId("221f3c6d-20e6-443e-b7dc-580a40fa3d14");
    updateEmpEntryInput.setOrganization("{\"resourceType\":\"Organization\"}");

    var expectedResponse = new UpdateEmpEntryResponseDTO();
    expectedResponse.setSuccess(true);
    expectedResponse.setParameters("success response");

    when(empService.updateEmpEntry(KVNR, requestId, USER_AGENT, updateEmpEntryInput))
        .thenReturn(expectedResponse);

    var result = empApi.updateEmpEntry(KVNR, requestId, USER_AGENT, updateEmpEntryInput);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getParameters()).isEqualTo("success response");
    verify(empService).updateEmpEntry(KVNR, requestId, USER_AGENT, updateEmpEntryInput);
  }

  @Test
  void shouldHandleUpdateEmpEntryFailure() {
    UUID requestId = UUID.randomUUID();
    var updateEmpEntryInput = new UpdateEmpEntryInput();

    var expectedResponse = new UpdateEmpEntryResponseDTO();
    expectedResponse.setSuccess(false);
    expectedResponse.setStatusMessage("Bad Request");

    when(empService.updateEmpEntry(KVNR, requestId, USER_AGENT, updateEmpEntryInput))
        .thenReturn(expectedResponse);

    var result = empApi.updateEmpEntry(KVNR, requestId, USER_AGENT, updateEmpEntryInput);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).isEqualTo("Bad Request");
    verify(empService).updateEmpEntry(KVNR, requestId, USER_AGENT, updateEmpEntryInput);
  }

  @Test
  void shouldBatchEmpSuccessfully() {
    UUID requestId = UUID.randomUUID();
    var batchEmpInput = new BatchEmpInput();
    batchEmpInput.setEmpAdds(List.of(new AddEmpEntryInput()));
    batchEmpInput.setEmpUpdates(List.of(new UpdateEmpEntryInput()));
    batchEmpInput.setEmpCommit(new EmpCommitInput().chronologyId("provenance-1"));
    batchEmpInput.setOrganization("{\"resourceType\":\"Organization\"}");

    var expectedResponse = new BatchEmpResponseDTO();
    expectedResponse.setSuccess(true);
    expectedResponse.setBundle("success response");

    when(empService.batchEmp(KVNR, requestId, USER_AGENT, batchEmpInput))
        .thenReturn(expectedResponse);

    var result = empApi.batchEmp(KVNR, requestId, USER_AGENT, batchEmpInput);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getBundle()).isEqualTo("success response");
    verify(empService).batchEmp(KVNR, requestId, USER_AGENT, batchEmpInput);
  }

  @Test
  void shouldHandleBatchEmpFailure() {
    UUID requestId = UUID.randomUUID();
    var batchEmpInput = new BatchEmpInput();

    var expectedResponse = new BatchEmpResponseDTO();
    expectedResponse.setSuccess(false);
    expectedResponse.setStatusMessage("Bad Request");

    when(empService.batchEmp(KVNR, requestId, USER_AGENT, batchEmpInput))
        .thenReturn(expectedResponse);

    var result = empApi.batchEmp(KVNR, requestId, USER_AGENT, batchEmpInput);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).isEqualTo("Bad Request");
    verify(empService).batchEmp(KVNR, requestId, USER_AGENT, batchEmpInput);
  }

  @Test
  void shouldGetMedicationPlanLogsSuccessfully() {
    UUID requestId = UUID.randomUUID();
    Integer count = 10;
    Integer offset = 0;
    String format = "application/fhir+json";

    var expectedResponse = new GetMedicationPlanLogsResponseDTO();
    expectedResponse.setSuccess(true);
    expectedResponse.setMedicationPlanLogs("{\"resourceType\":\"Bundle\",\"type\":\"searchset\"}");

    when(empService.getMedicationPlanLogs(KVNR, requestId, count, offset, format))
        .thenReturn(expectedResponse);

    var result = empApi.getMedicationPlanLogs(KVNR, requestId, count, offset, format);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getMedicationPlanLogs())
        .isEqualTo("{\"resourceType\":\"Bundle\",\"type\":\"searchset\"}");
    verify(empService).getMedicationPlanLogs(KVNR, requestId, count, offset, format);
  }

  @Test
  void shouldHandleGetMedicationPlanLogsFailure() {
    UUID requestId = UUID.randomUUID();
    Integer count = 10;
    Integer offset = 0;
    String format = "application/fhir+json";

    var expectedResponse = new GetMedicationPlanLogsResponseDTO();
    expectedResponse.setSuccess(false);
    expectedResponse.setStatusMessage("Bad Request");

    when(empService.getMedicationPlanLogs(KVNR, requestId, count, offset, format))
        .thenReturn(expectedResponse);

    var result = empApi.getMedicationPlanLogs(KVNR, requestId, count, offset, format);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).isEqualTo("Bad Request");
    verify(empService).getMedicationPlanLogs(KVNR, requestId, count, offset, format);
  }

  @Test
  void shouldGetMedicationPlanSuccessfully() {
    UUID requestId = UUID.randomUUID();
    String format = "application/fhir+json";

    var expectedResponse = new GetMedicationPlanResponseDTO();
    expectedResponse.setSuccess(true);
    expectedResponse.setMedicationPlan("{\"resourceType\":\"Bundle\",\"type\":\"searchset\"}");

    when(empService.getEmp(format, KVNR, requestId, null)).thenReturn(expectedResponse);

    var result = empApi.getMedicationPlan(KVNR, requestId, format, null);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getMedicationPlan())
        .isEqualTo("{\"resourceType\":\"Bundle\",\"type\":\"searchset\"}");
    verify(empService).getEmp(format, KVNR, requestId, null);
  }

  @Test
  void shouldHandleGetMedicationPlanFailure() {
    UUID requestId = UUID.randomUUID();
    String format = "application/fhir+json";

    var expectedResponse = new GetMedicationPlanResponseDTO();
    expectedResponse.setSuccess(false);
    expectedResponse.setStatusMessage("Bad Request");

    when(empService.getEmp(format, KVNR, requestId, null)).thenReturn(expectedResponse);

    var result = empApi.getMedicationPlan(KVNR, requestId, format, null);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).isEqualTo("Bad Request");
    verify(empService).getEmp(format, KVNR, requestId, null);
  }
}
