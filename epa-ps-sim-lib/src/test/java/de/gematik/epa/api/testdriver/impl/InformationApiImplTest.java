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

import de.gematik.epa.api.testdriver.information.dto.GetConsentDecisionInformationResponseDTO;
import de.gematik.epa.api.testdriver.information.dto.GetRecordStatusResponseDTO;
import de.gematik.epa.api.testdriver.information.dto.SetUserExperienceResponseDTO;
import de.gematik.epa.api.testdriver.information.dto.UxRequestType;
import de.gematik.epa.information.InformationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InformationApiImplTest {

  private InformationApiImpl informationApi;
  private final InformationService informationService = mock(InformationService.class);

  @BeforeAll
  void setUp() {
    informationApi = new InformationApiImpl(informationService);
  }

  @Test
  void shouldReturnConsentDecisionInformation() {
    var insurantId = "12345";
    var expectedResponse = new GetConsentDecisionInformationResponseDTO();
    when(informationService.getConsentDecisionInformation(insurantId)).thenReturn(expectedResponse);

    var actualResponse = informationApi.getConsentDecisionInformation(insurantId);

    assertThat(actualResponse).isEqualTo(expectedResponse);
  }

  @Test
  void shouldReturnRecordStatus() {
    var insurantId = "12345";
    var expectedResponse = new GetRecordStatusResponseDTO();
    when(informationService.getRecordStatus(insurantId)).thenReturn(expectedResponse);

    var actualResponse = informationApi.getRecordStatus(insurantId);

    assertThat(actualResponse).isEqualTo(expectedResponse);
  }

  @Test
  void shouldSetUserExperienceResult() {
    var uxRequestType = new UxRequestType();
    var expectedResponse = new SetUserExperienceResponseDTO();
    when(informationService.setUserExperienceResult(uxRequestType)).thenReturn(expectedResponse);

    var actualResponse = informationApi.setUserExperienceResult(uxRequestType);

    assertThat(actualResponse).isEqualTo(expectedResponse);
  }
}
