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
package de.gematik.epa.information;

import static de.gematik.epa.information.InformationService.*;
import static de.gematik.epa.unit.util.TestDataFactory.simulateInbound;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import de.gematik.epa.api.information.client.AccountInformationApi;
import de.gematik.epa.api.information.client.ConsentDecisionsApi;
import de.gematik.epa.api.information.client.UserExperienceApi;
import de.gematik.epa.api.information.client.dto.ErrorType;
import de.gematik.epa.api.information.client.dto.GetConsentDecisionInformation200Response;
import de.gematik.epa.api.information.client.dto.UxRequestType;
import de.gematik.epa.api.testdriver.information.dto.ConsentDecisionsResponseType;
import de.gematik.epa.api.testdriver.information.dto.ResponseDTO;
import de.gematik.epa.api.testdriver.information.dto.SetFqdnRequestDTO;
import de.gematik.epa.client.JaxRsClientWrapper;
import de.gematik.epa.utils.HealthRecordProvider;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InformationServiceTest {

  private static final String PS_SIM_AGENT = "ps-sim";
  private static final String EXAMPLE_COM = "http://example.com";
  private final JaxRsClientWrapper<AccountInformationApi> accountInformationClient =
      mock(JaxRsClientWrapper.class);
  private final JaxRsClientWrapper<ConsentDecisionsApi> consentDecisionsClient =
      mock(JaxRsClientWrapper.class);
  private final JaxRsClientWrapper<UserExperienceApi> userExperienceClient =
      mock(JaxRsClientWrapper.class);
  private final List<JaxRsClientWrapper<AccountInformationApi>> accountInformationClientWrapper =
      List.of(accountInformationClient);
  private final List<JaxRsClientWrapper<ConsentDecisionsApi>> consentDecisionsClientWrapper =
      List.of(consentDecisionsClient);
  private final List<JaxRsClientWrapper<UserExperienceApi>> userExperienceClientWrapper =
      List.of(userExperienceClient);
  private final InformationService informationService =
      new InformationService(
          accountInformationClientWrapper,
          consentDecisionsClientWrapper,
          userExperienceClientWrapper);
  private final AccountInformationApi accountInformationApi = mock(AccountInformationApi.class);
  private final ConsentDecisionsApi consentDecisionsApi = mock(ConsentDecisionsApi.class);
  private final UserExperienceApi userExperienceApi = mock(UserExperienceApi.class);
  private final String validInsurantId = "123";

  @BeforeAll
  void setUp() {
    when(accountInformationClient.getServiceApi()).thenReturn(accountInformationApi);
    when(accountInformationClient.getUserAgent()).thenReturn(PS_SIM_AGENT);
    when(accountInformationClient.getUrl()).thenReturn(EXAMPLE_COM);
    when(consentDecisionsClient.getServiceApi()).thenReturn(consentDecisionsApi);
    when(consentDecisionsClient.getUserAgent()).thenReturn(PS_SIM_AGENT);
    when(consentDecisionsClient.getUrl()).thenReturn(EXAMPLE_COM);
    when(userExperienceClient.getServiceApi()).thenReturn(userExperienceApi);
    when(userExperienceClient.getUserAgent()).thenReturn(PS_SIM_AGENT);
    when(userExperienceClient.getUrl()).thenReturn(EXAMPLE_COM);

    HealthRecordProvider.addHealthRecord("123", accountInformationClient.getUrl());
  }

  @Test
  void shouldReturnSuccessfulRecordStatusWhenResponseStatusIs204() {
    when(accountInformationClient.getServiceApi().getRecordStatus(anyString(), anyString()))
        .thenReturn(Response.status(204).build());
    var url = "http://localhost:8000/fqdn";
    when(accountInformationClient.getUrl()).thenReturn(url);

    var result = informationService.getRecordStatus("1245");
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getFqdn()).isEqualTo(url);
  }

  @Test
  void shouldNotGetRecordStatusFromInternalCache() {
    var insurantId = "556677";
    HealthRecordProvider.addHealthRecord(insurantId, "http://epa-1.de/fqdn");

    informationService.getRecordStatus(insurantId);
    verify(accountInformationApi, atLeastOnce()).getRecordStatus(insurantId, PS_SIM_AGENT);
  }

  @Test
  void shouldReturnErrorMessageWhenResponseStatusIs400() {
    var errorType = new ErrorType();
    var errorCode = "malformedRequest";
    errorType.setErrorCode(errorCode);

    var response = simulateInbound(Response.status(400).entity(errorType).build());
    when(accountInformationClient.getServiceApi().getRecordStatus(anyString(), anyString()))
        .thenReturn(response);

    var result = informationService.getRecordStatus("334");
    assertThat(result.getStatusMessage()).isEqualTo(errorCode);
  }

  @Test
  void shouldReturnUnknownErrorMessageWhenResponseStatusIsNotExpected() {
    when(accountInformationClient.getServiceApi().getRecordStatus(anyString(), anyString()))
        .thenReturn(Response.status(300).build());

    var result = informationService.getRecordStatus("999");
    assertThat(result.getStatusMessage()).isEqualTo(NO_RECORD_FOUND);
  }

  @Test
  void shouldReturnConsentDecisionInformationWhenResponseStatusIs200() {
    final GetConsentDecisionInformation200Response apiResponse =
        new GetConsentDecisionInformation200Response();
    final List<de.gematik.epa.api.information.client.dto.ConsentDecisionsResponseType> consents =
        new ArrayList<>();

    consents.add(
        new de.gematik.epa.api.information.client.dto.ConsentDecisionsResponseType()
            .decision(
                de.gematik.epa.api.information.client.dto.ConsentDecisionsResponseType.DecisionEnum
                    .PERMIT)
            .functionId("medication"));
    apiResponse.setData(consents);

    var response = simulateInbound(Response.status(200).entity(apiResponse).build());
    when(consentDecisionsClient
            .getServiceApi()
            .getConsentDecisionInformation(anyString(), anyString()))
        .thenReturn(response);

    var result = informationService.getConsentDecisionInformation(validInsurantId);
    ConsentDecisionsResponseType consentDecisionsResponseType = result.getConsentDecisions().get(0);
    assertThat(consentDecisionsResponseType.getDecision()).isEqualTo("permit");
    assertThat(consentDecisionsResponseType.getFunctionId()).isEqualTo("medication");
  }

  @Test
  void shouldReturnErrorMessageWhenConsentDecisionInformationResponseStatusIs400() {
    var errorType = new ErrorType();
    var errorCode = "malformedRequest";
    errorType.setErrorCode(errorCode);

    var response = simulateInbound(Response.status(400).entity(errorType).build());
    when(consentDecisionsClient
            .getServiceApi()
            .getConsentDecisionInformation(anyString(), anyString()))
        .thenReturn(response);

    var result = informationService.getConsentDecisionInformation(validInsurantId);
    assertThat(result.getStatusMessage()).isEqualTo(errorCode);
  }

  @Test
  void shouldReturnUnknownErrorMessageWhenConsentDecisionInformationResponseStatusIsNotExpected() {
    when(consentDecisionsClient
            .getServiceApi()
            .getConsentDecisionInformation(anyString(), anyString()))
        .thenReturn(Response.status(300).build());

    var result = informationService.getConsentDecisionInformation(validInsurantId);
    assertThat(result.getStatusMessage()).isEqualTo(UNKNOWN_ERROR);
  }

  @Test
  void shouldReturnSuccessfulUserExperienceWhenResponseStatusIs201() {
    when(userExperienceClient
            .getServiceApi()
            .setUserExperienceResult(anyString(), any(UxRequestType.class)))
        .thenReturn(Response.status(201).build());

    var result =
        informationService.setUserExperienceResult(
            new de.gematik.epa.api.testdriver.information.dto.UxRequestType()
                .measurement(1)
                .useCase(
                    de.gematik.epa.api.testdriver.information.dto.UxRequestType.UseCaseEnum
                        .UX_DOC_DOWNLOAD_PS));
    assertThat(result.getSuccess()).isTrue();
  }

  @Test
  void shouldReturnErrorMessageWhenUserExperienceResponseStatusIs400() {
    var errorType = new ErrorType();
    var errorCode = "malformedRequest";
    errorType.setErrorCode(errorCode);

    var response = simulateInbound(Response.status(400).entity(errorType).build());
    when(userExperienceClient
            .getServiceApi()
            .setUserExperienceResult(anyString(), any(UxRequestType.class)))
        .thenReturn(response);

    var result =
        informationService.setUserExperienceResult(
            new de.gematik.epa.api.testdriver.information.dto.UxRequestType()
                .measurement(1)
                .useCase(
                    de.gematik.epa.api.testdriver.information.dto.UxRequestType.UseCaseEnum
                        .UX_DOC_DOWNLOAD_PS));
    assertThat(result.getStatusMessage()).isEqualTo(errorCode);
  }

  @Test
  void shouldReturnUnknownErrorMessageWhenUserExperienceResponseStatusIsNotExpected() {
    when(userExperienceClient
            .getServiceApi()
            .setUserExperienceResult(anyString(), any(UxRequestType.class)))
        .thenReturn(Response.status(300).build());

    var result =
        informationService.setUserExperienceResult(
            new de.gematik.epa.api.testdriver.information.dto.UxRequestType()
                .measurement(1)
                .useCase(
                    de.gematik.epa.api.testdriver.information.dto.UxRequestType.UseCaseEnum
                        .UX_DOC_DOWNLOAD_PS));
    assertThat(result.getStatusMessage()).isEqualTo(UNKNOWN_ERROR);
  }

  @Test
  void shouldContinueToNextClientFor500StatusCode() {
    when(accountInformationClient.getServiceApi().getRecordStatus(anyString(), anyString()))
        .thenReturn(Response.status(500).build());

    var result = informationService.getRecordStatus("101112");

    assertThat(result.getSuccess()).isFalse();
  }

  @Test
  void shouldSetFqdn() {
    final SetFqdnRequestDTO requestDTO =
        new SetFqdnRequestDTO().fqdn("http://localhost:8000/fqdn").insurantId("Z1234");
    var response = informationService.setFqdn(requestDTO);
    assertThat(response.getSuccess()).isTrue();
  }

  @Test
  void setFqdnFailsWhenInsurantIdMissing() {
    final SetFqdnRequestDTO requestDTO = new SetFqdnRequestDTO().insurantId(null);
    var response = informationService.setFqdn(requestDTO);
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getStatusMessage()).isEqualTo(INSURANT_ID_MISSING);
  }

  @Test
  void setFqdnFailsWhenFqdnMissing() {
    final SetFqdnRequestDTO requestDTO = new SetFqdnRequestDTO().insurantId("Z12343355").fqdn(null);
    var response = informationService.setFqdn(requestDTO);
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getStatusMessage()).isEqualTo(FQDN_IS_MISSING);
  }

  @Test
  void setFqdnShouldFailWhenExceptionIsThrown() {
    try (MockedStatic<HealthRecordProvider> mockedStatic =
        Mockito.mockStatic(HealthRecordProvider.class)) {
      mockedStatic
          .when(() -> HealthRecordProvider.addHealthRecord(anyString(), anyString()))
          .thenThrow(new RuntimeException("Test Exception"));
      final SetFqdnRequestDTO requestDTO =
          new SetFqdnRequestDTO().fqdn("http://localhost:8000/fqdn").insurantId("Z12343455");
      ResponseDTO response = informationService.setFqdn(requestDTO);
      assertThat(response.getSuccess()).isFalse();
      assertEquals("Test Exception", response.getStatusMessage());
    }
  }
}
