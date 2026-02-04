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

import de.gematik.epa.api.testdriver.medication.dto.AddEmlEntryInput;
import de.gematik.epa.api.testdriver.medication.dto.AddEmlEntryResponseDTO;
import de.gematik.epa.api.testdriver.medication.dto.CancelEmlEntryInput;
import de.gematik.epa.api.testdriver.medication.dto.CancelEmlEntryInput.FormatEnum;
import de.gematik.epa.api.testdriver.medication.dto.CancelEmlEntryResponseDTO;
import de.gematik.epa.api.testdriver.medication.dto.LinkEmpInput;
import de.gematik.epa.api.testdriver.medication.dto.LinkEmpResponseDTO;
import de.gematik.epa.medication.MedicationStatementService;
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
}
