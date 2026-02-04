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
package de.gematik.epa.consent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.epa.api.consent_decision.client.ConsentDecisionsApi;
import de.gematik.epa.api.consent_decision.client.dto.ConsentDecisionType;
import de.gematik.epa.api.consent_decision.client.dto.ConsentDecisionsResponseType;
import de.gematik.epa.api.consent_decision.client.dto.ErrorType;
import de.gematik.epa.api.consent_decision.client.dto.UpdateConsentDecision200Response;
import de.gematik.epa.api.testdriver.consentDecision.dto.ConsentDecisionsResponseTypeDTO;
import de.gematik.epa.api.testdriver.consentDecision.dto.DecisionEnum;
import de.gematik.epa.api.testdriver.consentDecision.dto.GetConsentDecisionsResponseDTO;
import de.gematik.epa.api.testdriver.consentDecision.dto.PutConsentDecisionRequestDTO;
import de.gematik.epa.api.testdriver.consentDecision.dto.ResponseDTO;
import de.gematik.epa.client.JaxRsClientWrapper;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConsentDecisionsServiceTest {

  private static final String PS_SIM_AGENT = "ps-sim";
  private static final String UNKNOWN_ERROR = "Unknown error";
  private final JaxRsClientWrapper<ConsentDecisionsApi> consentDecisionsClient =
      mock(JaxRsClientWrapper.class);

  private final ConsentDecisionsApi consentDecisionsApi = mock(ConsentDecisionsApi.class);

  private final ConsentDecisionsService consentDecisionsService =
      new ConsentDecisionsService(consentDecisionsClient);

  private final String insurantId = "X12345677";

  static Stream<Object[]> provideErrorCases() {
    return Stream.of(
        new Object[] {400, "malformedRequest"},
        new Object[] {403, "notEntitled"},
        new Object[] {403, "invalidOid"},
        new Object[] {404, "noHealthRecord"},
        new Object[] {409, "statusMismatch"},
        new Object[] {500, "internalError"});
  }

  static Stream<Object[]> provideErrorCasesForUpdateConsentDecision() {
    return Stream.of(
        new Object[] {400, "malformedRequest"},
        new Object[] {403, "notEntitled"},
        new Object[] {403, "invalidOid"},
        new Object[] {404, "noHealthRecord"},
        new Object[] {404, "noResource"},
        new Object[] {409, "statusMismatch"},
        new Object[] {500, "internalError"});
  }

  @BeforeEach
  void setup() {
    when(consentDecisionsClient.getServiceApi()).thenReturn(consentDecisionsApi);
    when(consentDecisionsClient.getUserAgent()).thenReturn(PS_SIM_AGENT);
  }

  @Test
  void getConsentDecisionsShouldReturn200Success() {
    final UpdateConsentDecision200Response consents = new UpdateConsentDecision200Response();
    final ConsentDecisionsResponseType cd = new ConsentDecisionsResponseType();
    cd.setDecision(ConsentDecisionsResponseType.DecisionEnum.DENY);
    cd.functionId("medication");
    consents.data(List.of(cd));

    Response mockResponse = mock(Response.class);
    when(mockResponse.getStatus()).thenReturn(200);
    when(mockResponse.readEntity(UpdateConsentDecision200Response.class)).thenReturn(consents);
    when(consentDecisionsClient.getServiceApi().getConsentDecisions(anyString(), anyString()))
        .thenReturn(mockResponse);

    GetConsentDecisionsResponseDTO result = consentDecisionsService.getConsentDecisions(insurantId);
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getStatusMessage()).isEqualTo("Ok. Returns a list of consent decisions");

    assertThat(result.getData()).isNotNull();
    assertThat(result.getData()).hasSize(1);

    ConsentDecisionsResponseTypeDTO consentDecisionsResponseType = result.getData().get(0);
    assertThat(consentDecisionsResponseType.getDecision()).isEqualTo("deny");
    assertThat(consentDecisionsResponseType.getFunctionId()).isEqualTo("medication");
  }

  @ParameterizedTest
  @MethodSource("provideErrorCases")
  void getConsentDecisionsShouldReturnError(int statusCode, String errorCode) {
    var errorType = new ErrorType();
    errorType.setErrorCode(errorCode);

    Response response = mock(Response.class);
    when(response.getStatus()).thenReturn(statusCode);
    when(response.hasEntity()).thenReturn(true);
    when(response.readEntity(ErrorType.class)).thenReturn(errorType);
    when(consentDecisionsApi.getConsentDecisions(anyString(), anyString())).thenReturn(response);

    GetConsentDecisionsResponseDTO result = consentDecisionsService.getConsentDecisions(insurantId);
    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result.getSuccess()).isFalse();
    Assertions.assertThat(result.getStatusMessage()).isEqualTo(errorCode);
  }

  @Test
  void getConsentDecisionsShouldHandleNullResponse() {
    when(consentDecisionsApi.getConsentDecisions(anyString(), anyString())).thenReturn(null);

    GetConsentDecisionsResponseDTO result = consentDecisionsService.getConsentDecisions(insurantId);
    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result.getSuccess()).isFalse();
    Assertions.assertThat(result.getStatusMessage()).isEqualTo(UNKNOWN_ERROR);
  }

  @Test
  void updateConsentDecisionShouldReturn200Response() {

    String functionId = "medication";
    String insurantId = "X12345677";
    PutConsentDecisionRequestDTO putConsentDecisionRequestDTO = new PutConsentDecisionRequestDTO();
    putConsentDecisionRequestDTO.setDecision(DecisionEnum.DENY);

    Response response = mock(Response.class);
    when(response.getStatus()).thenReturn(200);

    when(consentDecisionsApi.updateConsentDecision(
            anyString(), anyString(), anyString(), any(ConsentDecisionType.class)))
        .thenReturn(response);

    ResponseDTO result =
        consentDecisionsService.updateConsentDecision(
            functionId, putConsentDecisionRequestDTO, insurantId);

    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getStatusMessage()).isNull();
    verify(consentDecisionsApi)
        .updateConsentDecision(
            insurantId,
            functionId,
            PS_SIM_AGENT,
            new ConsentDecisionType().decision(ConsentDecisionType.DecisionEnum.DENY));
  }

  @ParameterizedTest
  @MethodSource("provideErrorCasesForUpdateConsentDecision")
  void updateConsentDecisionsShouldReturnError(int statusCode, String errorCode) {
    var errorType = new ErrorType();
    errorType.setErrorCode(errorCode);

    Response response = mock(Response.class);
    when(response.getStatus()).thenReturn(statusCode);
    when(response.hasEntity()).thenReturn(true);
    when(response.readEntity(ErrorType.class)).thenReturn(errorType);
    when(consentDecisionsApi.updateConsentDecision(
            anyString(), anyString(), anyString(), any(ConsentDecisionType.class)))
        .thenReturn(response);

    PutConsentDecisionRequestDTO putConsentDecisionRequestDTO = new PutConsentDecisionRequestDTO();
    putConsentDecisionRequestDTO.setDecision(DecisionEnum.DENY);
    ResponseDTO result =
        consentDecisionsService.updateConsentDecision(
            "medication", putConsentDecisionRequestDTO, insurantId);
    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result.getSuccess()).isFalse();
    Assertions.assertThat(result.getStatusMessage()).isEqualTo(errorCode);
  }

  @Test
  void updateConsentDecisionShouldHandleNullResponse() {
    when(consentDecisionsApi.updateConsentDecision(
            anyString(), anyString(), anyString(), any(ConsentDecisionType.class)))
        .thenReturn(null);

    PutConsentDecisionRequestDTO putConsentDecisionRequestDTO = new PutConsentDecisionRequestDTO();
    putConsentDecisionRequestDTO.setDecision(DecisionEnum.DENY);

    ResponseDTO result =
        consentDecisionsService.updateConsentDecision(
            "medication", putConsentDecisionRequestDTO, insurantId);
    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result.getSuccess()).isFalse();
    Assertions.assertThat(result.getStatusMessage()).isEqualTo(UNKNOWN_ERROR);
  }
}
