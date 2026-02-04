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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.gematik.epa.api.testdriver.medication.dto.AddEmlEntryInput;
import de.gematik.epa.api.testdriver.medication.dto.CancelEmlEntryInput;
import de.gematik.epa.api.testdriver.medication.dto.CancelEmlEntryInput.FormatEnum;
import de.gematik.epa.api.testdriver.medication.dto.LinkEmpInput;
import de.gematik.epa.fhir.client.FhirClient;
import de.gematik.epa.medication.client.EmlRenderClient;
import de.gematik.epa.medication.client.RenderResponse;
import java.util.UUID;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MedicationStatementServiceTest {

  private FhirClient fhirClient;
  private EmlRenderClient emlRenderClient;
  private MedicationStatementService medicationStatementService;
  private IParser jsonParser;

  @BeforeEach
  void setup() {
    fhirClient = mock(FhirClient.class);
    emlRenderClient = mock(EmlRenderClient.class);
    final FhirContext fhirContext = FhirContext.forR4();
    jsonParser = fhirContext.newJsonParser();
    when(fhirClient.getContext()).thenReturn(fhirContext);
    medicationStatementService = new MedicationStatementService(fhirClient, emlRenderClient);
  }

  @Test
  void shouldAddEmlEntrySuccessfullyWithJsonFormat() {
    var insurantId = "X110435031";
    var requestId = UUID.randomUUID();
    var userAgent = "test-agent";

    var medicationStatement = new MedicationStatement();
    medicationStatement.setId("med-statement-1");
    var medication = new Medication();
    medication.setId("medication-1");
    var organization = "{\"resourceType\":\"Organization\",\"id\":\"org-1\"}";

    var addEmlEntryInput = new AddEmlEntryInput();
    addEmlEntryInput.setMedicationStatement(jsonParser.encodeResourceToString(medicationStatement));
    addEmlEntryInput.setMedication(jsonParser.encodeResourceToString(medication));
    addEmlEntryInput.setOrganization(organization);
    addEmlEntryInput.setFormat(AddEmlEntryInput.FormatEnum.APPLICATION_FHIR_JSON);

    var renderResponse = new RenderResponse().httpStatusCode(200).empResponse("success response");
    when(emlRenderClient.addEmlEntry(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(renderResponse);

    var result =
        medicationStatementService.addEmlEntry(insurantId, requestId, userAgent, addEmlEntryInput);

    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getParameters()).isEqualTo("success response");
  }

  @Test
  void shouldAddEmlEntrySuccessfullyWithXmlFormat() {
    var insurantId = "X110435031";
    var requestId = UUID.randomUUID();
    var userAgent = "test-agent";

    var medicationStatement = new MedicationStatement();
    medicationStatement.setId("med-statement-1");
    var medication = new Medication();
    medication.setId("medication-1");
    var organization = "{\"resourceType\":\"Organization\",\"id\":\"org-1\"}";

    var addEmlEntryInput = new AddEmlEntryInput();
    addEmlEntryInput.setMedicationStatement(jsonParser.encodeResourceToString(medicationStatement));
    addEmlEntryInput.setMedication(jsonParser.encodeResourceToString(medication));
    addEmlEntryInput.setOrganization(organization);
    addEmlEntryInput.setFormat(AddEmlEntryInput.FormatEnum.APPLICATION_FHIR_XML);

    var renderResponse = new RenderResponse().httpStatusCode(200).empResponse("success response");
    when(emlRenderClient.addEmlEntry(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(renderResponse);

    var result =
        medicationStatementService.addEmlEntry(insurantId, requestId, userAgent, addEmlEntryInput);

    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getParameters()).isEqualTo("success response");
  }

  @Test
  void shouldCancelEmlEntrySuccessfullyWithJsonFormat() {
    String id = "5";
    String insurantId = "X110435031";
    UUID requestId = UUID.randomUUID();
    String userAgent = "test-agent";

    String organization = "{\"resourceType\":\"Organization\",\"id\":\"org-1\"}";

    CancelEmlEntryInput cancelEmlEntryInput = new CancelEmlEntryInput();
    cancelEmlEntryInput.setOrganization(organization);
    cancelEmlEntryInput.setFormat(CancelEmlEntryInput.FormatEnum.APPLICATION_FHIR_JSON);

    var renderResponse = new RenderResponse().httpStatusCode(200).empResponse("success response");
    when(emlRenderClient.cancelEmlEntry(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(renderResponse);

    var cancelResult =
        medicationStatementService.cancelEmlEntry(
            insurantId, requestId, id, userAgent, cancelEmlEntryInput);

    assertThat(cancelResult.getSuccess()).isTrue();
    assertThat(cancelResult.getParameters()).isEqualTo("success response");
  }

  @Test
  void shouldCancelEmlEntrySuccessfullyWithXmlFormat() {
    String id = "5";
    String insurantId = "X110435031";
    UUID requestId = UUID.randomUUID();
    String userAgent = "test-agent";

    String organization = "{\"resourceType\":\"Organization\",\"id\":\"org-1\"}";

    CancelEmlEntryInput cancelEmlEntryInput = new CancelEmlEntryInput();
    cancelEmlEntryInput.setOrganization(organization);
    cancelEmlEntryInput.setFormat(FormatEnum.APPLICATION_FHIR_XML);

    var renderResponse = new RenderResponse().httpStatusCode(200).empResponse("success response");
    when(emlRenderClient.cancelEmlEntry(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(renderResponse);

    var cancelResult =
        medicationStatementService.cancelEmlEntry(
            insurantId, requestId, id, userAgent, cancelEmlEntryInput);

    assertThat(cancelResult.getSuccess()).isTrue();
    assertThat(cancelResult.getParameters()).isEqualTo("success response");
  }

  @Test
  void shouldLinkEmpSuccessfullyWithJsonFormat() {
    String insurantId = "X110435031";
    UUID requestId = UUID.randomUUID();
    String medicationStatementId = "1";
    String userAgent = "test-agent";
    String medicationPlanId = "160.000.000.012.345.67";
    String organization = "{\"resourceType\":\"Organization\",\"id\":\"org-1\"}";

    var linkEmpInput = new LinkEmpInput();
    linkEmpInput.setMedicationPlanId(medicationPlanId);
    linkEmpInput.setOrganization(organization);
    linkEmpInput.setFormat(LinkEmpInput.FormatEnum.APPLICATION_FHIR_JSON);

    var renderResponse = new RenderResponse().httpStatusCode(200).empResponse("success response");
    when(emlRenderClient.linkEmp(
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString()))
        .thenReturn(renderResponse);

    var result =
        medicationStatementService.linkEmp(
            insurantId, requestId, medicationStatementId, userAgent, linkEmpInput);

    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getParameters()).isEqualTo("success response");
  }

  @Test
  void shouldLinkEmpSuccessfullyWithXmlFormat() {
    String insurantId = "X110435031";
    UUID requestId = UUID.randomUUID();
    String medicationStatementId = "1";
    String userAgent = "test-agent";
    String medicationPlanId = "160.000.000.012.345.67";
    String organization = "{\"resourceType\":\"Organization\",\"id\":\"org-1\"}";

    var linkEmpInput = new LinkEmpInput();
    linkEmpInput.setMedicationPlanId(medicationPlanId);
    linkEmpInput.setOrganization(organization);
    linkEmpInput.setFormat(LinkEmpInput.FormatEnum.APPLICATION_FHIR_XML);

    var renderResponse = new RenderResponse().httpStatusCode(200).empResponse("success response");
    when(emlRenderClient.linkEmp(
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString()))
        .thenReturn(renderResponse);

    var result =
        medicationStatementService.linkEmp(
            insurantId, requestId, medicationStatementId, userAgent, linkEmpInput);

    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getParameters()).isEqualTo("success response");
  }
}
