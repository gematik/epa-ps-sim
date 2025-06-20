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

import de.gematik.epa.api.consent_decision.client.ConsentDecisionsApi;
import de.gematik.epa.api.testdriver.consentDecision.ConsentDecisionManagementApi;
import de.gematik.epa.api.testdriver.consentDecision.dto.GetConsentDecisionsResponseDTO;
import de.gematik.epa.api.testdriver.consentDecision.dto.PutConsentDecisionRequestDTO;
import de.gematik.epa.api.testdriver.consentDecision.dto.ResponseDTO;
import de.gematik.epa.client.JaxRsClientWrapper;
import de.gematik.epa.consent.ConsentDecisionsService;
import lombok.Setter;

@Setter
public class ConsentDecisionManagementApiImpl implements ConsentDecisionManagementApi {

  private ConsentDecisionsService consentDecisionsService;

  public ConsentDecisionManagementApiImpl(
      final JaxRsClientWrapper<ConsentDecisionsApi> consentDecisionsClientWrapper) {
    this.consentDecisionsService = new ConsentDecisionsService(consentDecisionsClientWrapper);
  }

  @Override
  public GetConsentDecisionsResponseDTO getConsentDecisions(String xInsurantid) {
    return consentDecisionsService.getConsentDecisions(xInsurantid);
  }

  @Override
  public ResponseDTO putConsentDecision(
      String functionid,
      PutConsentDecisionRequestDTO putConsentDecisionRequestDTO,
      String xInsurantid) {
    return consentDecisionsService.updateConsentDecision(
        functionid, putConsentDecisionRequestDTO, xInsurantid);
  }
}
