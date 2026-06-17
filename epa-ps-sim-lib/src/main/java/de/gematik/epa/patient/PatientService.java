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
package de.gematik.epa.patient;

import static de.gematik.epa.utils.StringUtils.appendCauses;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import de.gematik.epa.api.testdriver.patient.dto.PatientInformationDTO;
import de.gematik.epa.api.testdriver.patient.dto.PatientInformationResponseDTO;
import de.gematik.epa.fhir.client.FhirClient;
import de.gematik.epa.utils.FhirUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@AllArgsConstructor
@Service
public class PatientService {
  private final FhirClient fhirClient;

  public PatientInformationResponseDTO createOrUpdatePatientInformation(
      String xInsurantid, PatientInformationDTO patientInformationDTO) {
    var client = fhirClient.getClient();

    try {
      if (patientInformationDTO == null || patientInformationDTO.getPatient() == null) {
        throw new IllegalArgumentException("No patient information provided");
      }

      var patient = FhirUtils.fromString(patientInformationDTO.getPatient());
      String patientQueryParams =
          "Patient?identifier=http://fhir.de/sid/gkv/kvid-10|%s".formatted(xInsurantid);
      var outcome =
          client.update().resource(patient).conditionalByUrl(patientQueryParams).execute();

      if (outcome == null || outcome.getResource() == null) {
        throw new NullPointerException("No response or resource received from FHIR server");
      }

      if (outcome.getResource() instanceof Patient myPatient
          && (outcome.getResponseStatusCode() == HttpStatus.OK.value()
              || outcome.getResponseStatusCode() == HttpStatus.CREATED.value())) {
        return new PatientInformationResponseDTO()
            .success(true)
            .patient(FhirUtils.asJson(myPatient));
      } else {
        return new PatientInformationResponseDTO()
            .success(false)
            .statusMessage(
                "{ \"httpCode\": \"%d\", \"details\": %s }"
                    .formatted(
                        outcome.getResponseStatusCode(), FhirUtils.asJson(outcome.getResource())));
      }
    } catch (BaseServerResponseException e) {
      var msg =
          "{ \"httpCode\": \"%d\", \"details\": %s }"
              .formatted(e.getStatusCode(), e.getResponseBody());
      log.warn(msg);
      return new PatientInformationResponseDTO().success(false).statusMessage(msg);
    } catch (Exception e) {
      log.error("Error occurred during put patient", e);
      return new PatientInformationResponseDTO()
          .success(false)
          .statusMessage(appendCauses(e, new StringBuilder().append(e)).toString());
    }
  }
}
