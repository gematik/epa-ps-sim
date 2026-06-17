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
import de.gematik.epa.api.testdriver.medication.dto.CancelEmlEntryInput.FormatEnum;
import de.gematik.epa.medication.MedicationStatementSearch;
import de.gematik.epa.medication.MedicationStatementService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MedicationStatementApiImplTest {

  private MedicationStatementService medicationStatementService;
  private MedicationStatementApiImpl medicationStatementApi;

  @BeforeEach
  void setUp() {
    medicationStatementService = mock(MedicationStatementService.class);
    medicationStatementApi = new MedicationStatementApiImpl(medicationStatementService);
  }

  @Test
  void shouldAddEmlEntrySuccessfully() {
    UUID requestId = UUID.randomUUID();
    var addEmlEntryInput = new AddEmlEntryInput();
    addEmlEntryInput.setMedicationStatement("{\"resourceType\":\"MedicationStatement\"}");
    addEmlEntryInput.setMedication("{\"resourceType\":\"Medication\"}");
    addEmlEntryInput.setOrganization("{\"resourceType\":\"Organization\"}");

    var expectedResponse = new AddEmlEntryResponseDTO();
    expectedResponse.setSuccess(true);
    expectedResponse.setParameters("success response");

    when(medicationStatementService.addEmlEntry(KVNR, requestId, USER_AGENT, addEmlEntryInput))
        .thenReturn(expectedResponse);

    var result = medicationStatementApi.addEmlEntry(KVNR, requestId, USER_AGENT, addEmlEntryInput);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getParameters()).isEqualTo("success response");
    verify(medicationStatementService).addEmlEntry(KVNR, requestId, USER_AGENT, addEmlEntryInput);
  }

  @Test
  void shouldHandleAddEmlEntryFailure() {
    UUID requestId = UUID.randomUUID();
    var addEmlEntryInput = new AddEmlEntryInput();

    var expectedResponse = new AddEmlEntryResponseDTO();
    expectedResponse.setSuccess(false);
    expectedResponse.setStatusMessage("Bad Request");

    when(medicationStatementService.addEmlEntry(KVNR, requestId, USER_AGENT, addEmlEntryInput))
        .thenReturn(expectedResponse);

    var result = medicationStatementApi.addEmlEntry(KVNR, requestId, USER_AGENT, addEmlEntryInput);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).isEqualTo("Bad Request");
    verify(medicationStatementService).addEmlEntry(KVNR, requestId, USER_AGENT, addEmlEntryInput);
  }

  @ParameterizedTest
  @EnumSource(FormatEnum.class)
  void shouldCancelEmlEntrySuccessfully(FormatEnum format) {
    UUID requestId = UUID.randomUUID();

    var cancelEmlEntryInput = new CancelEmlEntryInput();
    cancelEmlEntryInput.setFormat(format);
    cancelEmlEntryInput.setOrganization("{\"resourceType\":\"Organization\"}");

    var expectedResponse = new CancelEmlEntryResponseDTO();
    expectedResponse.setSuccess(true);
    expectedResponse.setParameters("success response");

    when(medicationStatementService.cancelEmlEntry(
            KVNR, requestId, "1", USER_AGENT, cancelEmlEntryInput))
        .thenReturn(expectedResponse);

    var result =
        medicationStatementApi.cancelEmlEntry(
            KVNR, requestId, "1", USER_AGENT, cancelEmlEntryInput);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getParameters()).isEqualTo("success response");
    verify(medicationStatementService)
        .cancelEmlEntry(KVNR, requestId, "1", USER_AGENT, cancelEmlEntryInput);
  }

  @ParameterizedTest
  @EnumSource(FormatEnum.class)
  void shouldHandleCancelEmlEntryFailure() {
    UUID requestId = UUID.randomUUID();
    var cancelEmlEntryInput = new CancelEmlEntryInput();

    var expectedResponse = new CancelEmlEntryResponseDTO();
    expectedResponse.setSuccess(false);
    expectedResponse.setStatusMessage("Bad Request");

    when(medicationStatementService.cancelEmlEntry(
            KVNR, requestId, "1", USER_AGENT, cancelEmlEntryInput))
        .thenReturn(expectedResponse);

    var result =
        medicationStatementApi.cancelEmlEntry(
            KVNR, requestId, "1", USER_AGENT, cancelEmlEntryInput);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).isEqualTo("Bad Request");
    verify(medicationStatementService)
        .cancelEmlEntry(KVNR, requestId, "1", USER_AGENT, cancelEmlEntryInput);
  }

  @Test
  void shouldLinkEmpSuccessfully() {
    var requestId = UUID.randomUUID();
    var medicationPlanId = "160.000.000.012.345.67";

    var linkEmpInput = new LinkEmpInput();
    linkEmpInput.setMedicationPlanId(medicationPlanId);
    linkEmpInput.setOrganization("{\"resourceType\":\"Organization\"}");

    var expectedResponse = new LinkEmpResponseDTO();
    expectedResponse.setSuccess(true);
    expectedResponse.setParameters("success response");

    when(medicationStatementService.linkEmp(KVNR, requestId, "1", USER_AGENT, linkEmpInput))
        .thenReturn(expectedResponse);

    var result = medicationStatementApi.linkEmp(KVNR, requestId, "1", USER_AGENT, linkEmpInput);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getParameters()).isEqualTo("success response");
    verify(medicationStatementService).linkEmp(KVNR, requestId, "1", USER_AGENT, linkEmpInput);
  }

  @Test
  void shouldLinkEmpThenUnlinkSuccessfully() {
    var requestId = UUID.randomUUID();
    var medicationPlanId = "160.000.000.012.345.67";

    var linkEmpInput = new LinkEmpInput();
    linkEmpInput.setMedicationPlanId(medicationPlanId);
    linkEmpInput.setOrganization("{\"resourceType\":\"Organization\"}");

    var expectedResponse = new LinkEmpResponseDTO();
    expectedResponse.setSuccess(true);
    expectedResponse.setParameters("success response");

    when(medicationStatementService.linkEmp(KVNR, requestId, "1", USER_AGENT, linkEmpInput))
        .thenReturn(expectedResponse);

    var result = medicationStatementApi.linkEmp(KVNR, requestId, "1", USER_AGENT, linkEmpInput);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getParameters()).isEqualTo("success response");
    verify(medicationStatementService).linkEmp(KVNR, requestId, "1", USER_AGENT, linkEmpInput);

    // unlink
    var unlinkEmpInput = new UnlinkEmpInput();
    unlinkEmpInput.setMedicationPlanId(medicationPlanId);
    unlinkEmpInput.setOrganization("{\"resourceType\":\"Organization\"}");

    var unlinkRespDTO = new UnlinkEmpResponseDTO();
    unlinkRespDTO.setSuccess(true);
    unlinkRespDTO.setParameters("Success response");

    when(medicationStatementService.unlinkEmp(KVNR, requestId, "1", USER_AGENT, unlinkEmpInput))
        .thenReturn(unlinkRespDTO);

    var unlinkedResult =
        medicationStatementApi.unlinkEmp(KVNR, requestId, "1", USER_AGENT, unlinkEmpInput);

    assertThat(unlinkedResult).isNotNull();
    assertThat(unlinkedResult.getSuccess()).isNotNull();
    assertThat(unlinkedResult.getSuccess()).isTrue();
    assertThat(unlinkedResult.getParameters()).isNotNull();
    assertThat(unlinkedResult.getParameters()).isEqualTo("Success response");
  }

  @Test
  void shouldGetMedicationStatementHistoryByIdAndVersionSuccessfully() {
    var requestId = UUID.randomUUID();
    var id = "23";
    var versionId = "2";
    var format = "application/fhir+json";

    var expectedSearch =
        new MedicationStatementSearch()
            .id(id)
            .insurantId(KVNR)
            .requestId(requestId)
            .useragent(USER_AGENT)
            .versionId(versionId)
            .format(format);

    var expectedResponse = new GetMedicationStatementHistoryByIdAndVersionResponseDTO();
    expectedResponse.setSuccess(true);
    expectedResponse.setMedicationStatement("{\"resourceType\":\"MedicationStatement\"}");

    when(medicationStatementService.getMedicationStatementHistoryById(expectedSearch))
        .thenReturn(expectedResponse);

    var result =
        medicationStatementApi.getMedicationStatementHistoryByIdAndVersion(
            KVNR, requestId, id, versionId, USER_AGENT, format);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getMedicationStatement())
        .isEqualTo("{\"resourceType\":\"MedicationStatement\"}");
    verify(medicationStatementService).getMedicationStatementHistoryById(expectedSearch);
  }

  @Test
  void shouldHandleGetMedicationStatementHistoryByIdAndVersionNotFound() {
    var requestId = UUID.randomUUID();
    var id = "unknown-id";
    var versionId = "1";
    var format = "application/fhir+json";

    var expectedSearch =
        new MedicationStatementSearch()
            .id(id)
            .insurantId(KVNR)
            .requestId(requestId)
            .useragent(USER_AGENT)
            .versionId(versionId)
            .format(format);

    var expectedResponse = new GetMedicationStatementHistoryByIdAndVersionResponseDTO();
    expectedResponse.setSuccess(true);
    expectedResponse.setStatusMessage(
        "No medication statement history found for ID: " + id + " and version: " + versionId);

    when(medicationStatementService.getMedicationStatementHistoryById(expectedSearch))
        .thenReturn(expectedResponse);

    var result =
        medicationStatementApi.getMedicationStatementHistoryByIdAndVersion(
            KVNR, requestId, id, versionId, USER_AGENT, format);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getStatusMessage()).contains("No medication statement history");
    verify(medicationStatementService).getMedicationStatementHistoryById(expectedSearch);
  }

  @Test
  void shouldGetMedicationStatementHistoryListSuccessfully() {
    var requestId = UUID.randomUUID();
    var id = "23";
    var format = "application/fhir+json";

    var expectedSearch =
        new MedicationStatementSearch()
            .id(id)
            .insurantId(KVNR)
            .requestId(requestId)
            .useragent(USER_AGENT)
            .format(format);

    var expectedResponse = new GetMedicationStatementHistoryResponseDTO();
    expectedResponse.setSuccess(true);
    expectedResponse.setMedicationStatements(List.of("{\"resourceType\":\"MedicationStatement\"}"));

    when(medicationStatementService.searchMedicationStatementHistory(expectedSearch))
        .thenReturn(expectedResponse);

    var result =
        medicationStatementApi.getMedicationStatementHistoryList(
            KVNR, requestId, id, USER_AGENT, format);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getMedicationStatements()).hasSize(1);
    verify(medicationStatementService).searchMedicationStatementHistory(expectedSearch);
  }

  @Test
  void shouldHandleGetMedicationStatementHistoryListNotFound() {
    var requestId = UUID.randomUUID();
    var id = "unknown-id";
    var format = "application/fhir+json";

    var expectedSearch =
        new MedicationStatementSearch()
            .id(id)
            .insurantId(KVNR)
            .requestId(requestId)
            .useragent(USER_AGENT)
            .format(format);

    var expectedResponse = new GetMedicationStatementHistoryResponseDTO();
    expectedResponse.setSuccess(true);
    expectedResponse.setStatusMessage("No medication statement historyBundle found for ID: " + id);

    when(medicationStatementService.searchMedicationStatementHistory(expectedSearch))
        .thenReturn(expectedResponse);

    var result =
        medicationStatementApi.getMedicationStatementHistoryList(
            KVNR, requestId, id, USER_AGENT, format);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getStatusMessage()).contains("No medication statement historyBundle");
    verify(medicationStatementService).searchMedicationStatementHistory(expectedSearch);
  }

  @Test
  void shouldGetMedicationStatementsCallsSearchWithDefaultsWhenIdIsNull() {
    var requestId = UUID.randomUUID();

    // DEFAULT_COUNT = 10, DEFAULT_OFFSET = 0 are applied when count/offset params are null
    var expectedSearch =
        new MedicationStatementSearch()
            .insurantId(KVNR)
            .requestId(requestId)
            .useragent(USER_AGENT)
            .count(10)
            .offset(0);

    var expectedResponse = new GetMedicationStatementListDTO();
    expectedResponse.setSuccess(true);
    expectedResponse.setMedicationStatements(List.of("{\"resourceType\":\"MedicationStatement\"}"));

    when(medicationStatementService.searchMedicationStatements(expectedSearch))
        .thenReturn(expectedResponse);

    var result =
        medicationStatementApi.getMedicationStatements(
            KVNR,
            requestId,
            USER_AGENT,
            null,
            null,
            null, // count, offset, total → defaults applied
            null, // id = null → triggers search
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
            null);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getMedicationStatements()).hasSize(1);
    verify(medicationStatementService).searchMedicationStatements(expectedSearch);
  }

  @Test
  void shouldGetMedicationStatementsCallsSearchWithExplicitParams() {
    var requestId = UUID.randomUUID();
    var status = "active";
    var context = "EMP";
    var count = 5;
    var offset = 10;

    var expectedSearch =
        new MedicationStatementSearch()
            .insurantId(KVNR)
            .requestId(requestId)
            .useragent(USER_AGENT)
            .count(count)
            .offset(offset)
            .status(status)
            .context(context);

    var expectedResponse = new GetMedicationStatementListDTO();
    expectedResponse.setSuccess(true);

    when(medicationStatementService.searchMedicationStatements(expectedSearch))
        .thenReturn(expectedResponse);

    var result =
        medicationStatementApi.getMedicationStatements(
            KVNR,
            requestId,
            USER_AGENT,
            count,
            offset,
            null, // count, offset, total
            null, // id = null → triggers search
            null,
            null,
            null,
            null,
            null,
            status,
            null,
            null,
            null,
            context,
            null);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isTrue();
    verify(medicationStatementService).searchMedicationStatements(expectedSearch);
  }

  @Test
  void shouldGetMedicationStatementsCallsGetByIdWhenIdIsNotNull() {
    var requestId = UUID.randomUUID();
    var id = "23";

    var expectedResponse = new GetMedicationStatementListDTO();
    expectedResponse.setSuccess(true);
    expectedResponse.setMedicationStatements(List.of("{\"resourceType\":\"MedicationStatement\"}"));

    when(medicationStatementService.executeGetById(id)).thenReturn(expectedResponse);

    var result =
        medicationStatementApi.getMedicationStatements(
            KVNR,
            requestId,
            USER_AGENT,
            null,
            null,
            null, // count, offset, total — ignored when id is set
            id, // id != null → triggers getById
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
            null);

    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getMedicationStatements()).hasSize(1);
    verify(medicationStatementService).executeGetById(id);
  }
}
