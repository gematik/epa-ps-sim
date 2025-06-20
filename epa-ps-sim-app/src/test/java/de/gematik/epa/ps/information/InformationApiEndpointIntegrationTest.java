/*-
 * #%L
 * epa-ps-sim-app
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
package de.gematik.epa.ps.information;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static de.gematik.epa.information.InformationService.NO_RECORD_FOUND;
import static de.gematik.epa.information.InformationService.UNKNOWN_ERROR;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.epa.api.information.client.dto.ConsentDecisionsResponseType;
import de.gematik.epa.api.information.client.dto.ErrorType;
import de.gematik.epa.api.information.client.dto.GetConsentDecisionInformation200Response;
import de.gematik.epa.api.testdriver.information.dto.GetConsentDecisionInformationResponseDTO;
import de.gematik.epa.api.testdriver.information.dto.GetRecordStatusResponseDTO;
import de.gematik.epa.api.testdriver.information.dto.SetFqdnRequestDTO;
import de.gematik.epa.api.testdriver.information.dto.UxRequestType;
import de.gematik.epa.ps.endpoint.InformationApiEndpoint;
import de.gematik.epa.ps.utils.AbstractIntegrationTest;
import de.gematik.epa.utils.HealthRecordProvider;
import java.util.List;
import lombok.SneakyThrows;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InformationApiEndpointIntegrationTest extends AbstractIntegrationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private static final String INSURANT_ID = "123";
  @Autowired InformationApiEndpoint informationApiEndpoint;

  @Test
  void contextLoads() {
    assertThat(informationApiEndpoint).isNotNull();
  }

  @Test
  void recordStatusShouldReturnSuccessFor204Response() {
    // given
    stubSuccessfulGetRecordStatus(mockInformationServer1, 204);

    // when
    var result = getGetRecordStatus();

    // then
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getFqdn()).isNotBlank();
  }

  @Test
  @SneakyThrows
  void recordStatusShouldReturnFailedAndErrorMessageFor409Response() {
    // given
    final String errorCode = "statusMismatch";
    var errorType =
        new ErrorType().errorDetail("Health record is not in state ACTIVATED").errorCode(errorCode);
    var body = objectMapper.writeValueAsString(errorType);
    stubFailedGetRecordStatus(mockInformationServer1, 409, body); // AS 1
    stubFailedGetRecordStatus(mockInformationServer2, 409, body); // AS 2

    // when
    var result = getGetRecordStatus();

    // then
    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).contains(errorCode);
  }

  @Test
  @SneakyThrows
  void recordStatusShouldReturnSuccessWhileFirstAsReturnErrorMessageFor409Response() {
    // given
    final String errorCode = "statusMismatch";
    var errorType =
        new ErrorType().errorDetail("Health record is not in state ACTIVATED").errorCode(errorCode);
    var body = objectMapper.writeValueAsString(errorType);
    stubFailedGetRecordStatus(mockInformationServer1, 409, body); // AS 1
    stubSuccessfulGetRecordStatus(mockInformationServer2, 204); // AS 2

    // when
    var result = getGetRecordStatus();

    // then
    assertThat(result.getSuccess()).isTrue();
  }

  @Test
  @SneakyThrows
  void recordStatusShouldReturnFailedAndErrorMessageForUnknownResponse() {
    // given
    var errorType = new ErrorType().errorCode(NO_RECORD_FOUND);
    var body = objectMapper.writeValueAsString(errorType);
    stubFailedGetRecordStatus(mockInformationServer1, 501, body);

    // when
    var result = getGetRecordStatus();

    // then
    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).contains(NO_RECORD_FOUND);
  }

  @Test
  @SneakyThrows
  void getConsentDecisionInformationShouldReturnSuccessFor200Response() {
    // given
    final List<ConsentDecisionsResponseType> consents =
        List.of(
            new ConsentDecisionsResponseType()
                .decision(ConsentDecisionsResponseType.DecisionEnum.PERMIT)
                .functionId("medication"));
    final GetConsentDecisionInformation200Response response =
        new GetConsentDecisionInformation200Response();

    response.setData(consents);
    var body = objectMapper.writeValueAsString(response);
    stubSuccessfulGetRecordStatus(mockInformationServer1, 204);
    stubGetConsentDecisions(mockInformationServer1, 200, body);

    getGetRecordStatus();
    // when
    var result = getConsentDecisionInformation();

    // then
    assertThat(result.getSuccess()).isTrue();
    var expectedConsentDecision = result.getConsentDecisions().getFirst();
    assertThat(expectedConsentDecision.getDecision()).isEqualTo("permit");
    assertThat(expectedConsentDecision.getFunctionId()).isEqualTo("medication");
  }

  @Test
  @SneakyThrows
  void getConsentDecisionInformationShouldReturnSuccessFor400Response() {
    // given
    final String errorCode = "malformedRequest";
    var errorType = new ErrorType().errorDetail("Bad Request").errorCode(errorCode);
    var body = objectMapper.writeValueAsString(errorType);
    stubSuccessfulGetRecordStatus(mockInformationServer1, 204);
    stubGetConsentDecisions(mockInformationServer1, 400, body);

    getGetRecordStatus();
    // when
    var result = getConsentDecisionInformation();

    // then
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getStatusMessage()).contains(errorCode);
  }

  @Test
  void getConsentDecisionInformationShouldReturnFailureForUnknownResponse() {
    // given
    stubSuccessfulGetRecordStatus(mockInformationServer1, 204);
    mockInformationServer1.stubFor(
        get(urlEqualTo("/information/api/v1/ehr/consentdecisions"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                    .withStatus(501)));

    getGetRecordStatus();
    // when
    var result = getConsentDecisionInformation();

    // then
    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).isEqualTo(UNKNOWN_ERROR);
  }

  @Test
  void setUserExperienceResultShouldReturnSuccessFor201Response() {
    // given
    mockInformationServer1.stubFor(
        post(urlEqualTo("/information/api/v1/userexperience"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                    .withStatus(201)));

    // when
    var uxRequestType =
        new UxRequestType().measurement(1299).useCase(UxRequestType.UseCaseEnum.UX_LOGIN_PS);

    var result = informationApiEndpoint.setUserExperienceResult(uxRequestType);

    // then
    assertThat(result.getSuccess()).isTrue();
  }

  @Test
  @SneakyThrows
  void setUserExperienceResultShouldReturnSuccessFor400Response() {
    // given
    final String errorCode = "malformedRequest";
    var errorType = new ErrorType().errorDetail("Bad Request").errorCode(errorCode);
    var body = objectMapper.writeValueAsString(errorType);
    mockInformationServer1.stubFor(
        post(urlEqualTo("/information/api/v1/userexperience"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                    .withBody(body)
                    .withStatus(400)));

    // when
    var uxRequestType =
        new UxRequestType().measurement(1299).useCase(UxRequestType.UseCaseEnum.UX_LOGIN_PS);

    var result = informationApiEndpoint.setUserExperienceResult(uxRequestType);

    // then
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getStatusMessage()).contains(errorCode);
  }

  @Test
  void setUserExperienceResultShouldReturnFailureForUnknownResponse() {
    // given
    mockInformationServer1.stubFor(
        post(urlEqualTo("/information/api/v1/userexperience"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                    .withStatus(501)));

    // when
    var uxRequestType =
        new UxRequestType().measurement(1299).useCase(UxRequestType.UseCaseEnum.UX_LOGIN_PS);

    var result = informationApiEndpoint.setUserExperienceResult(uxRequestType);

    // then
    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).isEqualTo(UNKNOWN_ERROR);
  }

  @Test
  void shouldSetFqdnAndReturn200() {
    // given
    final String insurantId = "Z123456789";
    final String fqdn = "https://fqdn.de";

    // when
    var request = new SetFqdnRequestDTO().insurantId(insurantId).fqdn(fqdn);
    var result = informationApiEndpoint.setFqdn(request);

    // then
    assertThat(result.getSuccess()).isTrue();
    assertThat(HealthRecordProvider.getHealthRecordUrl(insurantId)).isEqualTo(fqdn);
  }

  private GetRecordStatusResponseDTO getGetRecordStatus() {
    return informationApiEndpoint.getRecordStatus(INSURANT_ID);
  }

  private GetConsentDecisionInformationResponseDTO getConsentDecisionInformation() {
    return informationApiEndpoint.getConsentDecisionInformation(INSURANT_ID);
  }
}
