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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.epa.api.consent_decision.client.ConsentDecisionsApi;
import de.gematik.epa.api.testdriver.consentDecision.dto.GetConsentDecisionsResponseDTO;
import de.gematik.epa.api.testdriver.consentDecision.dto.PutConsentDecisionRequestDTO;
import de.gematik.epa.api.testdriver.consentDecision.dto.ResponseDTO;
import de.gematik.epa.client.JaxRsClientWrapper;
import de.gematik.epa.consent.ConsentDecisionsService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConsentDecisionManagementApiImplTest {

  private final JaxRsClientWrapper<ConsentDecisionsApi> consentDecisionsClient =
      mock(JaxRsClientWrapper.class);
  private ConsentDecisionManagementApiImpl consentDecisionsApi;

  private final ConsentDecisionsService consentDecisionsService =
      mock(ConsentDecisionsService.class);

  @BeforeAll
  void setUp() {
    consentDecisionsApi = new ConsentDecisionManagementApiImpl(consentDecisionsClient);
    consentDecisionsApi.setConsentDecisionsService(consentDecisionsService);
  }

  @Test
  void getConsentDecisionsShouldReturnConsentDecisions() {
    String insurantId = "X12345678";
    GetConsentDecisionsResponseDTO expectedResponse = new GetConsentDecisionsResponseDTO();
    when(consentDecisionsService.getConsentDecisions(insurantId)).thenReturn(expectedResponse);
    GetConsentDecisionsResponseDTO actualResponse =
        consentDecisionsApi.getConsentDecisions(insurantId);
    assertThat(actualResponse).isEqualTo(expectedResponse);
  }

  @Test
  void putConsentDecision() {
    String functionId = "medication";
    String insurantId = "X12345678";
    var expectedResponse = new ResponseDTO();
    when(consentDecisionsService.updateConsentDecision(
            functionId, new PutConsentDecisionRequestDTO(), insurantId))
        .thenReturn(expectedResponse);
    var actualResponse =
        consentDecisionsApi.putConsentDecision(
            functionId, new PutConsentDecisionRequestDTO(), insurantId);
    assertThat(actualResponse).isEqualTo(expectedResponse);
  }
}
