/*-
 * #%L
 * epa-ps-sim-lib
 * %%
 * Copyright (C) 2025 gematik GmbH
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */
package de.gematik.epa.api.testdriver.impl;

import de.gematik.epa.api.testdriver.information.InformationApi;
import de.gematik.epa.api.testdriver.information.dto.*;
import de.gematik.epa.information.InformationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class InformationApiImpl implements InformationApi {

  private final InformationService informationService;

  @Override
  public GetConsentDecisionInformationResponseDTO getConsentDecisionInformation(String insurantId) {
    return informationService.getConsentDecisionInformation(insurantId);
  }

  @Override
  public GetRecordStatusResponseDTO getRecordStatus(String insurantId) {
    return informationService.getRecordStatus(insurantId);
  }

  @Override
  public SetUserExperienceResponseDTO setUserExperienceResult(UxRequestType uxRequestType) {
    return informationService.setUserExperienceResult(uxRequestType);
  }

  @Override
  public ResponseDTO setFqdn(final SetFqdnRequestDTO setFqdnRequestDTO) {
    return informationService.setFqdn(setFqdnRequestDTO);
  }
}
