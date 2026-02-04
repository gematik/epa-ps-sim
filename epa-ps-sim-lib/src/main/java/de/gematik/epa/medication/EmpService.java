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

import static java.util.Base64.getEncoder;

import ca.uhn.fhir.parser.IParser;
import de.gematik.epa.api.testdriver.medication.dto.*;
import de.gematik.epa.fhir.client.FhirClient;
import de.gematik.epa.medication.client.EmlRenderClient;
import de.gematik.epa.medication.client.RenderResponse;
import de.gematik.epa.utils.MiscUtils;
import java.util.UUID;
import org.hl7.fhir.r4.model.*;

public class EmpService {

  private static final String ADD_EMP_INPUT_PROFILE =
      "https://gematik.de/fhir/epa-medication/StructureDefinition/epa-op-add-emp-entry-input-parameters";

  private static final String UPDATE_EMP_INPUT_PROFILE =
      "https://gematik.de/fhir/epa-medication/StructureDefinition/epa-op-update-emp-entry-input-parameters";

  private final FhirClient fhirClient;
  private final EmlRenderClient emlRenderClient;

  public EmpService(FhirClient fhirClient, EmlRenderClient emlRenderClient) {
    this.fhirClient = fhirClient;
    this.emlRenderClient = emlRenderClient;
  }

  public AddEmpEntryResponseDTO addEmpEntry(
      final String insurantId,
      final UUID requestId,
      final String userAgent,
      final AddEmpEntryInput addEmpEntryInput) {

    final Parameters parameters = new Parameters();

    final IParser jsonParser = fhirClient.getContext().newJsonParser();

    Resource medicationResource =
        jsonParser.parseResource(Medication.class, addEmpEntryInput.getMedication());

    final Parameters.ParametersParameterComponent parameterComponentMedication =
        new Parameters.ParametersParameterComponent();
    parameterComponentMedication.setName("medication");
    Parameters.ParametersParameterComponent medicationPart =
        new Parameters.ParametersParameterComponent();
    medicationPart.setName("resource");
    medicationPart.setResource(medicationResource);
    parameterComponentMedication.addPart(medicationPart);

    Resource medicationRequestResource =
        jsonParser.parseResource(MedicationRequest.class, addEmpEntryInput.getMedicationRequest());
    final Parameters.ParametersParameterComponent parameterComponentMedicationRequest =
        createParameterForMedicationRequest(medicationRequestResource);

    parameters.setId(UUID.randomUUID().toString());
    parameters.setMeta(new Meta().addProfile(ADD_EMP_INPUT_PROFILE));
    parameters
        .addParameter(parameterComponentMedicationRequest)
        .addParameter(parameterComponentMedication);

    if (addEmpEntryInput.getChronologyId() != null) {
      final Parameters.ParametersParameterComponent parameterComponentChronology =
          createParameterForChronologyId(addEmpEntryInput.getChronologyId());
      parameters.addParameter(parameterComponentChronology);
    }

    final String encodedOrganization =
        getEncoder().encodeToString(addEmpEntryInput.getOrganization().getBytes());

    String parametersAsJsonOrXml =
        addEmpEntryInput.getFormat() != null
                && addEmpEntryInput.getFormat().equals(EmlRenderClient.APPLICATION_FHIR_JSON)
            ? jsonParser.setPrettyPrint(true).encodeResourceToString(parameters)
            : fhirClient
                .getContext()
                .newXmlParser()
                .setPrettyPrint(true)
                .encodeResourceToString(parameters);

    final RenderResponse renderResponse =
        emlRenderClient.addEmpEntry(
            insurantId,
            requestId != null ? requestId.toString() : UUID.randomUUID().toString(),
            userAgent,
            parametersAsJsonOrXml,
            encodedOrganization,
            addEmpEntryInput.getFormat());

    final AddEmpEntryResponseDTO responseDTO = new AddEmpEntryResponseDTO();
    responseDTO
        .success(renderResponse.httpStatusCode() == 200)
        .statusMessage(renderResponse.errorMessage())
        .parameters(renderResponse.empResponse());
    return responseDTO;
  }

  public UpdateEmpEntryResponseDTO updateEmpEntry(
      final String insurantId,
      final UUID requestId,
      final String userAgent,
      final UpdateEmpEntryInput updateEmpEntryInput) {
    final IParser jsonParser = fhirClient.getContext().newJsonParser();

    final Parameters parameters = new Parameters();

    Resource medicationRequestResource =
        jsonParser.parseResource(
            MedicationRequest.class, updateEmpEntryInput.getMedicationRequest());
    final Parameters.ParametersParameterComponent parameterComponentMedicationRequest =
        createParameterForMedicationRequest(medicationRequestResource);

    parameters.setId(UUID.randomUUID().toString());
    parameters.setMeta(new Meta().addProfile(UPDATE_EMP_INPUT_PROFILE));
    parameters.addParameter(parameterComponentMedicationRequest);

    if (updateEmpEntryInput.getChronologyId() != null) {
      final Parameters.ParametersParameterComponent parameterComponentChronology =
          createParameterForChronologyId(updateEmpEntryInput.getChronologyId());
      parameters.addParameter(parameterComponentChronology);
    }

    if (updateEmpEntryInput.getMedicationPlanId() != null) {
      final Parameters.ParametersParameterComponent parameterMedicationPlanId =
          new Parameters.ParametersParameterComponent();
      parameterMedicationPlanId.setName("medicationPlanIdentifier");

      parameterMedicationPlanId.setValue(
          new Identifier()
              .setSystem("https://gematik.de/fhir/sid/emp-identifier")
              .setValue(updateEmpEntryInput.getMedicationPlanId()));
      parameters.addParameter(parameterMedicationPlanId);
    }

    final String encodedOrganization =
        getEncoder().encodeToString(updateEmpEntryInput.getOrganization().getBytes());
    String parametersAsJsonOrXml =
        updateEmpEntryInput.getFormat() != null
                && updateEmpEntryInput.getFormat().equals(EmlRenderClient.APPLICATION_FHIR_JSON)
            ? jsonParser.setPrettyPrint(true).encodeResourceToString(parameters)
            : fhirClient
                .getContext()
                .newXmlParser()
                .setPrettyPrint(true)
                .encodeResourceToString(parameters);

    final RenderResponse renderResponse =
        emlRenderClient.updateEmpEntry(
            insurantId,
            requestId != null ? requestId.toString() : UUID.randomUUID().toString(),
            userAgent,
            parametersAsJsonOrXml,
            encodedOrganization,
            updateEmpEntryInput.getFormat());

    final UpdateEmpEntryResponseDTO responseDTO = new UpdateEmpEntryResponseDTO();
    responseDTO
        .success(renderResponse.httpStatusCode() == 200)
        .statusMessage(renderResponse.errorMessage())
        .parameters(renderResponse.empResponse());
    return responseDTO;
  }

  public GetMedicationPlanLogsResponseDTO getMedicationPlanLogs(
      String insurantId, UUID requestId, Integer count, Integer offset, String format) {

    var response = new GetMedicationPlanLogsResponseDTO();
    try {
      var renderResponse =
          emlRenderClient.getMedicationPlanLogs(
              insurantId,
              requestId != null ? requestId.toString() : UUID.randomUUID().toString(),
              count,
              offset,
              MiscUtils.expectedFormat(format));
      response
          .success(renderResponse.httpStatusCode() == 200)
          .medicationPlanLogs(renderResponse.medicationPlanLogs())
          .statusMessage(renderResponse.errorMessage());
    } catch (IllegalArgumentException e) {
      response.success(Boolean.FALSE).statusMessage(e.getMessage());
    }
    return response;
  }

  private Parameters.ParametersParameterComponent createParameterForMedicationRequest(
      Resource medicationRequestResource) {
    final Parameters.ParametersParameterComponent parameterComponentMedicationRequest =
        new Parameters.ParametersParameterComponent();
    parameterComponentMedicationRequest.setName("empEntry");
    parameterComponentMedicationRequest.setResource(medicationRequestResource);
    return parameterComponentMedicationRequest;
  }

  private Parameters.ParametersParameterComponent createParameterForChronologyId(String value) {
    final Parameters.ParametersParameterComponent parameterComponentChronology =
        new Parameters.ParametersParameterComponent();
    parameterComponentChronology.setName("acknowledgedChronologyId");
    parameterComponentChronology.setProperty("value[x]", new IdType().setValue(value));
    return parameterComponentChronology;
  }
}
