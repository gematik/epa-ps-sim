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

import static java.util.Base64.*;

import ca.uhn.fhir.parser.IParser;
import de.gematik.epa.api.testdriver.medication.dto.*;
import de.gematik.epa.fhir.client.FhirClient;
import de.gematik.epa.medication.client.EmlRenderClient;
import de.gematik.epa.medication.client.RenderResponse;
import java.util.UUID;
import org.hl7.fhir.r4.model.*;

public class MedicationStatementService {

  private static final String ADD_EML_INPUT_PROFILE =
      "https://gematik.de/fhir/epa-medication/StructureDefinition/epa-op-add-eml-entry-input-parameters";
  private static final String LINK_EMP_PROFILE =
      "https://gematik.de/fhir/epa-medication/StructureDefinition/epa-op-link-emp-entry-parameters";
  private static final String EMP_IDENTIFIER = "https://gematik.de/fhir/sid/emp-identifier";

  private final FhirClient fhirClient;
  private final EmlRenderClient emlRenderClient;

  public MedicationStatementService(FhirClient fhirClient, EmlRenderClient emlRenderClient) {
    this.fhirClient = fhirClient;
    this.emlRenderClient = emlRenderClient;
  }

  public AddEmlEntryResponseDTO addEmlEntry(
      final String insurantId,
      final UUID requestId,
      final String userAgent,
      final AddEmlEntryInput addEmlEntryInput) {
    final Parameters.ParametersParameterComponent parameterComponentMedicationStatement =
        new Parameters.ParametersParameterComponent();
    parameterComponentMedicationStatement.setName("medicationStatement");

    final IParser jsonParser = fhirClient.getContext().newJsonParser();
    var parametersAsJsonOrXml =
        createParameters(addEmlEntryInput, jsonParser, parameterComponentMedicationStatement);
    final String encodedOrganization = encodeToBase64(addEmlEntryInput.getOrganization());

    final RenderResponse renderResponse =
        emlRenderClient.addEmlEntry(
            insurantId,
            requestId != null ? requestId.toString() : UUID.randomUUID().toString(),
            userAgent,
            parametersAsJsonOrXml,
            encodedOrganization,
            addEmlEntryInput.getFormat());

    final AddEmlEntryResponseDTO responseDTO = new AddEmlEntryResponseDTO();
    responseDTO
        .success(renderResponse.httpStatusCode() == 200)
        .statusMessage(renderResponse.errorMessage())
        .parameters(renderResponse.empResponse());
    return responseDTO;
  }

  public CancelEmlEntryResponseDTO cancelEmlEntry(
      String insurantId,
      UUID requestId,
      String id,
      String useragent,
      CancelEmlEntryInput cancelEmlEntryInput) {
    final String encodedOrganization = encodeToBase64(cancelEmlEntryInput.getOrganization());

    final RenderResponse renderResponse =
        emlRenderClient.cancelEmlEntry(
            insurantId,
            requestId != null ? requestId.toString() : UUID.randomUUID().toString(),
            encodedOrganization,
            id,
            useragent,
            cancelEmlEntryInput.getFormat());

    final CancelEmlEntryResponseDTO responseDTO = new CancelEmlEntryResponseDTO();
    responseDTO
        .success(renderResponse.httpStatusCode() == 200)
        .statusMessage(renderResponse.errorMessage())
        .parameters(renderResponse.empResponse());

    return responseDTO;
  }

  public LinkEmpResponseDTO linkEmp(
      String insurantId,
      UUID requestId,
      String medicationStatementId,
      String useragent,
      LinkEmpInput linkEmpInput) {

    var encodedOrganization = encodeToBase64(linkEmpInput.getOrganization());
    var parametersAsJsonOrXml =
        createLinkEmpParameters(linkEmpInput, fhirClient.getContext().newJsonParser());

    final RenderResponse renderResponse =
        emlRenderClient.linkEmp(
            insurantId,
            requestId != null ? requestId.toString() : UUID.randomUUID().toString(),
            parametersAsJsonOrXml,
            encodedOrganization,
            medicationStatementId,
            useragent,
            linkEmpInput.getFormat());
    final LinkEmpResponseDTO responseDTO = new LinkEmpResponseDTO();
    responseDTO
        .success(renderResponse.httpStatusCode() == 200)
        .statusMessage(renderResponse.errorMessage())
        .parameters(renderResponse.empResponse());
    return responseDTO;
  }

  private String createLinkEmpParameters(LinkEmpInput linkEmpInput, final IParser jsonParser) {
    final Parameters parameters = new Parameters();
    parameters.setId(UUID.randomUUID().toString());
    parameters.setMeta(new Meta().addProfile(LINK_EMP_PROFILE));

    final Parameters.ParametersParameterComponent medicationPlanIdentifierParam =
        new Parameters.ParametersParameterComponent();
    medicationPlanIdentifierParam.setName("medicationPlanIdentifier");

    final Identifier identifier = new Identifier();
    identifier.setSystem(EMP_IDENTIFIER);
    identifier.setValue(linkEmpInput.getMedicationPlanId());

    medicationPlanIdentifierParam.setValue(identifier);
    parameters.addParameter(medicationPlanIdentifierParam);

    return linkEmpInput.getFormat() != null
            && linkEmpInput.getFormat().equals(EmlRenderClient.APPLICATION_FHIR_JSON)
        ? jsonParser.setPrettyPrint(true).encodeResourceToString(parameters)
        : fhirClient
            .getContext()
            .newXmlParser()
            .setPrettyPrint(true)
            .encodeResourceToString(parameters);
  }

  private String createParameters(
      final AddEmlEntryInput addEmlEntryInput,
      final IParser jsonParser,
      final Parameters.ParametersParameterComponent parameterComponentMedicationStatement) {
    Resource medicationStatementResource =
        jsonParser.parseResource(
            MedicationStatement.class, addEmlEntryInput.getMedicationStatement());
    parameterComponentMedicationStatement.setResource(medicationStatementResource);

    final Parameters.ParametersParameterComponent parameterComponentMedication =
        new Parameters.ParametersParameterComponent();
    parameterComponentMedication.setName("medication");
    Resource medicationResource =
        jsonParser.parseResource(Medication.class, addEmlEntryInput.getMedication());
    parameterComponentMedication.setResource(medicationResource);

    final Parameters parameters = new Parameters();
    parameters.setId(UUID.randomUUID().toString());
    parameters.setMeta(new Meta().addProfile(ADD_EML_INPUT_PROFILE));
    parameters
        .addParameter(parameterComponentMedicationStatement)
        .addParameter(parameterComponentMedication);

    return addEmlEntryInput.getFormat() != null
            && addEmlEntryInput.getFormat().equals(EmlRenderClient.APPLICATION_FHIR_JSON)
        ? jsonParser.setPrettyPrint(true).encodeResourceToString(parameters)
        : fhirClient
            .getContext()
            .newXmlParser()
            .setPrettyPrint(true)
            .encodeResourceToString(parameters);
  }

  private String encodeToBase64(final String input) {
    return getEncoder().encodeToString(input.getBytes());
  }
}
