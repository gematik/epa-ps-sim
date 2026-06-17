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

import de.gematik.epa.api.testdriver.patient.PatientApi;
import de.gematik.epa.api.testdriver.patient.dto.PatientInformationDTO;
import de.gematik.epa.api.testdriver.patient.dto.PatientInformationResponseDTO;
import de.gematik.epa.patient.PatientService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PatientApiImpl implements PatientApi {

  private final PatientService patientService;

  @Override
  public PatientInformationResponseDTO createOrUpdatePatientInformation(
      String xInsurantid, PatientInformationDTO patientInformationDTO) {
    return patientService.createOrUpdatePatientInformation(xInsurantid, patientInformationDTO);
  }
}
