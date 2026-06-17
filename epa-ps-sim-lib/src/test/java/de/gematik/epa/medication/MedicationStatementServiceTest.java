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

import static de.gematik.epa.unit.util.TestDataFactory.KVNR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.SearchTotalModeEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import de.gematik.epa.api.testdriver.medication.dto.AddEmlEntryInput;
import de.gematik.epa.api.testdriver.medication.dto.CancelEmlEntryInput;
import de.gematik.epa.api.testdriver.medication.dto.CancelEmlEntryInput.FormatEnum;
import de.gematik.epa.api.testdriver.medication.dto.LinkEmpInput;
import de.gematik.epa.api.testdriver.medication.dto.UnlinkEmpInput;
import de.gematik.epa.fhir.client.FhirClient;
import de.gematik.epa.medication.client.EmlRenderClient;
import de.gematik.epa.medication.client.RenderResponse;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

  @Test
  void shouldLinkEmpWithChronologyIdIncludesChronologyIdInParameters() {
    UUID requestId = UUID.randomUUID();
    String medicationStatementId = "1";
    String userAgent = "test-agent";
    String medicationPlanId = "160.000.000.012.345.67";
    String chronologyId = "160.000.000.089.123.45";

    var linkEmpInput = new LinkEmpInput();
    linkEmpInput.setMedicationPlanId(medicationPlanId);
    linkEmpInput.setChronologyId(chronologyId);
    linkEmpInput.setOrganization("{\"resourceType\":\"Organization\"}");
    linkEmpInput.setFormat(LinkEmpInput.FormatEnum.APPLICATION_FHIR_JSON);

    var renderResponse = new RenderResponse().httpStatusCode(200).empResponse("success response");
    var paramCaptor = ArgumentCaptor.forClass(String.class);
    when(emlRenderClient.linkEmp(
            anyString(),
            anyString(),
            paramCaptor.capture(),
            anyString(),
            anyString(),
            anyString(),
            anyString()))
        .thenReturn(renderResponse);

    var result =
        medicationStatementService.linkEmp(
            KVNR, requestId, medicationStatementId, userAgent, linkEmpInput);
    assertThat(paramCaptor.getValue()).contains(chronologyId);
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getParameters()).isEqualTo("success response");
  }

  @Test
  void shouldUnlinkEmpSuccessfullyWithJsonFormat() {
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

    var successRenderResp =
        new RenderResponse().httpStatusCode(200).empResponse("success response");
    when(emlRenderClient.linkEmp(
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString()))
        .thenReturn(successRenderResp);

    var result =
        medicationStatementService.linkEmp(
            insurantId, requestId, medicationStatementId, userAgent, linkEmpInput);

    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getParameters()).isEqualTo("success response");

    // unlink
    when(emlRenderClient.unlinkEmp(
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString()))
        .thenReturn(successRenderResp);

    var unlinkEmpInput = new UnlinkEmpInput();
    unlinkEmpInput.setMedicationPlanId(medicationPlanId);
    unlinkEmpInput.setOrganization(organization);
    unlinkEmpInput.setFormat(UnlinkEmpInput.FormatEnum.APPLICATION_FHIR_JSON);

    var unlinkedResult =
        medicationStatementService.unlinkEmp(
            insurantId, requestId, medicationStatementId, userAgent, unlinkEmpInput);

    assertThat(unlinkedResult).isNotNull();

    assertThat(unlinkedResult.getSuccess()).isNotNull();
    assertThat(unlinkedResult.getSuccess()).isTrue();

    assertThat(unlinkedResult.getParameters()).isNotNull();
    assertThat(unlinkedResult.getParameters()).isEqualTo("success response");
  }

  @Test
  void unlinkEmpReturns400() {
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

    var successRenderResp =
        new RenderResponse().httpStatusCode(200).empResponse("success response");
    when(emlRenderClient.linkEmp(
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString()))
        .thenReturn(successRenderResp);

    var result =
        medicationStatementService.linkEmp(
            insurantId, requestId, medicationStatementId, userAgent, linkEmpInput);

    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getParameters()).isEqualTo("success response");

    // unlink
    var failureRenderResp =
        new RenderResponse().httpStatusCode(400).errorMessage("success failure");
    when(emlRenderClient.unlinkEmp(
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString()))
        .thenReturn(failureRenderResp);

    var unlinkEmpInput = new UnlinkEmpInput();
    unlinkEmpInput.setMedicationPlanId(medicationPlanId);
    unlinkEmpInput.setOrganization(organization);
    unlinkEmpInput.setFormat(UnlinkEmpInput.FormatEnum.APPLICATION_FHIR_JSON);

    var unlinkedResult =
        medicationStatementService.unlinkEmp(
            insurantId, requestId, medicationStatementId, userAgent, unlinkEmpInput);

    assertThat(unlinkedResult).isNotNull();

    assertThat(unlinkedResult.getSuccess()).isNotNull();
    assertThat(unlinkedResult.getSuccess()).isFalse();

    assertThat(unlinkedResult.getStatusMessage()).isNotNull();
    assertThat(unlinkedResult.getStatusMessage()).isEqualTo("success failure");

    assertThat(unlinkedResult.getParameters()).isNull();
  }

  @Test
  void shouldSearchMedicationStatementsReturnsResults() {
    // given
    var searchRequest =
        new MedicationStatementSearch().status("active").count(10).offset(0).context("MANUAL");

    var bundleQuery = mockMedicationStatementSearch();

    var bundle = new Bundle();
    var entry = new Bundle.BundleEntryComponent();
    var medicationStatement = new MedicationStatement();
    medicationStatement.setId("23");
    entry.setResource(medicationStatement);
    bundle.addEntry(entry);

    when(bundleQuery.execute()).thenReturn(bundle);

    // when
    var result = medicationStatementService.searchMedicationStatements(searchRequest);

    // then
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getMedicationStatements()).hasSize(1);
    assertThat(result.getStatusMessage()).isBlank();
  }

  @Test
  void shouldSearchMedicationStatementsReturnsEmptyResult() {
    // given
    var searchRequest = new MedicationStatementSearch().status("active").count(10).offset(0);

    var bundleQuery = mockMedicationStatementSearch();
    when(bundleQuery.execute()).thenReturn(new Bundle());

    // when
    var result = medicationStatementService.searchMedicationStatements(searchRequest);

    // then
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getStatusMessage()).contains("No medication statement found");
  }

  @Test
  void shouldSearchMedicationStatementsHandlesException() {
    // given
    var searchRequest = new MedicationStatementSearch().status("active").count(10).offset(0);

    var client = mock(IGenericClient.class);
    when(fhirClient.getClient()).thenReturn(client);
    when(client.search()).thenThrow(new RuntimeException("FHIR server error"));

    // when
    var result = medicationStatementService.searchMedicationStatements(searchRequest);

    // then
    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).contains("FHIR server error");
  }

  @Test
  void shouldExecuteGetMedicationStatementByIdSuccessfully() {
    // given
    var id = "23";
    var medicationStatement = new MedicationStatement();
    medicationStatement.setId(id);

    var readExecutable = mockMedicationStatementGetById(id);
    when(readExecutable.execute()).thenReturn(medicationStatement);

    // when
    var result = medicationStatementService.executeGetById(id);

    // then
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getMedicationStatements()).hasSize(1);
  }

  @Test
  void shouldExecuteGetMedicationStatementByIdReturnsNotFound() {
    // given
    var id = "unknown-id";

    var readExecutable = mockMedicationStatementGetById(id);
    when(readExecutable.execute()).thenThrow(new ResourceNotFoundException("Not found"));

    // when
    var result = medicationStatementService.executeGetById(id);

    // then
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getStatusMessage()).contains("No medicationStatement found for ID: " + id);
  }

  @Test
  void shouldExecuteGetMedicationStatementByIdHandlesException() {
    // given
    var id = "23";

    var client = mock(IGenericClient.class);
    when(fhirClient.getClient()).thenReturn(client);
    when(client.read()).thenThrow(new RuntimeException("FHIR server error"));

    // when
    var result = medicationStatementService.executeGetById(id);

    // then
    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).contains("FHIR server error");
  }

  @Test
  void shouldSearchMedicationStatementHistoryReturnsResults() {
    // given
    var id = "23";
    var searchRequest = new MedicationStatementSearch().id(id).format("application/fhir+json");

    var historyTyped = mockMedicationStatementHistory();

    var bundle = new Bundle();
    var entry = new Bundle.BundleEntryComponent();
    var medicationStatement = new MedicationStatement();
    medicationStatement.setId(id);
    entry.setResource(medicationStatement);
    bundle.addEntry(entry);

    when(historyTyped.execute()).thenReturn(bundle);

    // when
    var result = medicationStatementService.searchMedicationStatementHistory(searchRequest);

    // then
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getMedicationStatements()).hasSize(1);
  }

  @Test
  void shouldSearchMedicationStatementHistoryReturnsNotFoundForEmptyBundle() {
    // given
    var id = "unknown-id";
    var searchRequest = new MedicationStatementSearch().id(id);

    var historyTyped = mockMedicationStatementHistory();
    when(historyTyped.execute()).thenReturn(new Bundle());

    // when
    var result = medicationStatementService.searchMedicationStatementHistory(searchRequest);

    // then
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getStatusMessage())
        .contains("No medication statement historyBundle found for ID: " + id);
  }

  @Test
  void shouldSearchMedicationStatementHistoryHandlesResourceNotFoundException() {
    // given
    var id = "unknown-id";
    var searchRequest = new MedicationStatementSearch().id(id);

    var historyTyped = mockMedicationStatementHistory();
    when(historyTyped.execute()).thenThrow(new ResourceNotFoundException("Not found"));

    // when
    var result = medicationStatementService.searchMedicationStatementHistory(searchRequest);

    // then
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getStatusMessage())
        .contains("No medication statement historyBundle found for ID: " + id);
  }

  @Test
  void shouldSearchMedicationStatementHistoryHandlesException() {
    // given
    var id = "23";
    var searchRequest = new MedicationStatementSearch().id(id);

    var client = mock(IGenericClient.class);
    when(fhirClient.getClient()).thenReturn(client);
    when(client.history()).thenThrow(new RuntimeException("FHIR server error"));

    // when
    var result = medicationStatementService.searchMedicationStatementHistory(searchRequest);

    // then
    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).contains("FHIR server error");
  }

  @Test
  void shouldGetMedicationStatementHistoryByIdSuccessfully() {
    // given
    var id = "23";
    var versionId = "2";
    var searchRequest = new MedicationStatementSearch().id(id).versionId(versionId);

    var readExecutable = mockMedicationStatementGetByIdAndVersion(id, versionId);
    var medicationStatement = new MedicationStatement();
    medicationStatement.setId(id);
    when(readExecutable.execute()).thenReturn(medicationStatement);

    // when
    var result = medicationStatementService.getMedicationStatementHistoryById(searchRequest);

    // then
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getMedicationStatement()).isNotNull();
  }

  @Test
  void shouldGetMedicationStatementHistoryByIdReturnsNotFound() {
    // given
    var id = "unknown-id";
    var versionId = "1";
    var searchRequest = new MedicationStatementSearch().id(id).versionId(versionId);

    var readExecutable = mockMedicationStatementGetByIdAndVersion(id, versionId);
    when(readExecutable.execute()).thenThrow(new ResourceNotFoundException("Not found"));

    // when
    var result = medicationStatementService.getMedicationStatementHistoryById(searchRequest);

    // then
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getStatusMessage())
        .contains("No medication statement history found for ID: " + id)
        .contains(versionId);
  }

  @Test
  void shouldGetMedicationStatementHistoryByIdHandlesException() {
    // given
    var id = "23";
    var versionId = "1";
    var searchRequest = new MedicationStatementSearch().id(id).versionId(versionId);

    var client = mock(IGenericClient.class);
    when(fhirClient.getClient()).thenReturn(client);
    when(client.read()).thenThrow(new RuntimeException("FHIR server error"));

    // when
    var result = medicationStatementService.getMedicationStatementHistoryById(searchRequest);

    // then
    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).contains("FHIR server error");
  }

  private IQuery<Bundle> mockMedicationStatementSearch() {
    var client = mock(IGenericClient.class);
    when(fhirClient.getClient()).thenReturn(client);

    final IUntypedQuery<IBaseBundle> search = mock(IUntypedQuery.class);
    when(client.search()).thenReturn(search);

    final IQuery<IBaseBundle> baseQuery = mock(IQuery.class);
    when(search.forResource(MedicationStatement.class)).thenReturn(baseQuery);
    when(baseQuery.where(any(ICriterion.class))).thenReturn(baseQuery);
    when(baseQuery.count(anyInt())).thenReturn(baseQuery);
    when(baseQuery.offset(anyInt())).thenReturn(baseQuery);
    when(baseQuery.totalMode(any(SearchTotalModeEnum.class))).thenReturn(baseQuery);
    when(baseQuery.revInclude(any(Include.class))).thenReturn(baseQuery);
    when(baseQuery.include(any(Include.class))).thenReturn(baseQuery);
    when(baseQuery.and(any(ICriterion.class))).thenReturn(baseQuery);
    when(baseQuery.lastUpdated(any(DateRangeParam.class))).thenReturn(baseQuery);

    final IQuery<Bundle> bundleQuery = mock(IQuery.class);
    when(baseQuery.returnBundle(Bundle.class)).thenReturn(bundleQuery);
    return bundleQuery;
  }

  private IReadExecutable<MedicationStatement> mockMedicationStatementGetById(String id) {
    var client = mock(IGenericClient.class);
    when(fhirClient.getClient()).thenReturn(client);

    var read = mock(IRead.class);
    when(client.read()).thenReturn(read);

    final IReadTyped<MedicationStatement> resource = mock(IReadTyped.class);
    when(read.resource(MedicationStatement.class)).thenReturn(resource);

    final IReadExecutable<MedicationStatement> readExecutable = mock(IReadExecutable.class);
    when(resource.withId(id)).thenReturn(readExecutable);
    return readExecutable;
  }

  private IReadExecutable<MedicationStatement> mockMedicationStatementGetByIdAndVersion(
      String id, String versionId) {
    var client = mock(IGenericClient.class);
    when(fhirClient.getClient()).thenReturn(client);

    var read = mock(IRead.class);
    when(client.read()).thenReturn(read);

    final IReadTyped<MedicationStatement> resource = mock(IReadTyped.class);
    when(read.resource(MedicationStatement.class)).thenReturn(resource);

    final IReadExecutable<MedicationStatement> readExecutable = mock(IReadExecutable.class);
    when(resource.withIdAndVersion(id, versionId)).thenReturn(readExecutable);
    return readExecutable;
  }

  private IHistoryTyped<Bundle> mockMedicationStatementHistory() {
    var client = mock(IGenericClient.class);
    when(fhirClient.getClient()).thenReturn(client);

    var history = mock(IHistory.class);
    when(client.history()).thenReturn(history);

    var historyUntyped = mock(IHistoryUntyped.class);
    when(history.onInstance(any(IdType.class))).thenReturn(historyUntyped);

    final IHistoryTyped<Bundle> historyTyped = mock(IHistoryTyped.class);
    when(historyUntyped.returnBundle(Bundle.class)).thenReturn(historyTyped);
    return historyTyped;
  }
}
