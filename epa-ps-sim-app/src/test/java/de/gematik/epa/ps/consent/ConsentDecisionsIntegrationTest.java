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
package de.gematik.epa.ps.consent;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static de.gematik.epa.unit.AppTestDataFactory.clearFqdnProvider;
import static de.gematik.epa.unit.AppTestDataFactory.setupFqdnProvider;
import static de.gematik.epa.unit.util.TestDataFactory.KVNR;
import static de.gematik.epa.unit.util.TestDataFactory.USER_AGENT;
import static de.gematik.epa.unit.util.TestDataFactory.X_INSURANTID;
import static de.gematik.epa.unit.util.TestDataFactory.X_USERAGENT;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import de.gematik.epa.api.consent_decision.client.dto.ErrorType;
import de.gematik.epa.api.testdriver.consentDecision.dto.DecisionEnum;
import de.gematik.epa.api.testdriver.consentDecision.dto.GetConsentDecisionsResponseDTO;
import de.gematik.epa.api.testdriver.consentDecision.dto.PutConsentDecisionRequestDTO;
import de.gematik.epa.api.testdriver.consentDecision.dto.ResponseDTO;
import de.gematik.epa.ps.endpoint.ConsentDecisionsApiEndpoint;
import de.gematik.epa.ps.fhir.config.TestFhirClientProvider;
import de.gematik.epa.unit.TestDocumentClientConfiguration;
import de.gematik.epa.unit.TestKonnektorClientConfiguration;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(
    classes = {
      TestKonnektorClientConfiguration.class,
      TestDocumentClientConfiguration.class,
      TestFhirClientProvider.class
    })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ConsentDecisionsIntegrationTest {

  @RegisterExtension
  static WireMockExtension mockConsentDecisionsServer =
      WireMockExtension.newInstance().options(wireMockConfig().port(8089)).build();

  private final ObjectMapper objectMapper = new ObjectMapper();
  @Autowired ConsentDecisionsApiEndpoint consentDecisionsApiEndpoint;

  @Test
  void contextLoads() {
    assertThat(consentDecisionsApiEndpoint).isNotNull();
  }

  @BeforeEach
  void setup() {
    setupFqdnProvider(KVNR);
  }

  @AfterEach
  void tearDown() {
    clearFqdnProvider(KVNR);
  }

  @Test
  @SneakyThrows
  void getConsentDecisionsShouldReturnSuccessFor200Response() {
    var bodyAsString =
        FileUtils.readFileToString(
            FileUtils.getFile("src/test/resources/consent/consent-decision.json"),
            StandardCharsets.UTF_8);

    mockConsentDecisionsServer.stubFor(
        get(urlEqualTo("/epa/basic/api/v1/consents"))
            .withHeader(X_INSURANTID, equalTo(KVNR))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody(bodyAsString)));

    GetConsentDecisionsResponseDTO response = consentDecisionsApiEndpoint.getConsentDecisions(KVNR);
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getData()).hasSize(2);
    response.getData().forEach(cd -> assertThat(cd.getDecision()).isEqualTo("permit"));
  }

  @Test
  void getConsentDecisionsShouldReturnFailureFor400Response() throws JsonProcessingException {

    var errorType =
        new ErrorType().errorDetail("Request does not match schema").errorCode("malformedRequest");
    var body = objectMapper.writeValueAsString(errorType);
    mockConsentDecisionsServer.stubFor(
        get(urlEqualTo("/epa/basic/api/v1/consents"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withHeader(X_INSURANTID, "X12345678")
                    .withStatus(200)
                    .withBody(body)));
    GetConsentDecisionsResponseDTO response = consentDecisionsApiEndpoint.getConsentDecisions(KVNR);

    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getData()).isEmpty();
  }

  @Test
  @SneakyThrows
  void putConsentDecisionsShouldReturnSuccessFor200Response() {
    final PutConsentDecisionRequestDTO putConsentDecisionRequestDTO =
        new PutConsentDecisionRequestDTO();
    putConsentDecisionRequestDTO.setDecision(DecisionEnum.DENY);

    mockConsentDecisionsServer.stubFor(
        put(urlPathTemplate("/epa/basic/api/v1/consents/{functionid}"))
            .withPathParam("functionid", matching("medication|erp-submission|other-function"))
            .withHeader(X_INSURANTID, equalTo(KVNR))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .withRequestBody(
                equalToJson("{\"decision\": \"${json-unit.regex}(?i)deny\"}", true, true))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                    .withStatus(200)
                    .withBody("{\"success\": true}")));

    var functionId = "medication";
    ResponseDTO response =
        consentDecisionsApiEndpoint.putConsentDecision(
            functionId, putConsentDecisionRequestDTO, KVNR);
    assertThat(response.getSuccess()).isTrue();

    mockConsentDecisionsServer.verify(
        putRequestedFor(urlPathTemplate("/epa/basic/api/v1/consents/{functionid}"))
            .withPathParam("functionid", equalTo(functionId)));
  }

  @SneakyThrows
  @Test
  void putConsentDecisionsShouldReturnErrorFor404Response() {
    final PutConsentDecisionRequestDTO putConsentDecisionRequestDTO =
        new PutConsentDecisionRequestDTO();
    putConsentDecisionRequestDTO.setDecision(DecisionEnum.DENY);

    final String errorCode = "noResource";
    var errorType =
        new ErrorType().errorDetail("Resource for functionid does not exist").errorCode(errorCode);
    var body = objectMapper.writeValueAsString(errorType);

    mockConsentDecisionsServer.stubFor(
        put(urlPathTemplate("/epa/basic/api/v1/consents/{functionid}"))
            .withPathParam("functionid", equalTo("invalid_function"))
            .withHeader(X_INSURANTID, equalTo(KVNR))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .withRequestBody(
                equalToJson("{\"decision\": \"${json-unit.regex}(?i)deny\"}", true, true))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                    .withBody(body)
                    .withStatus(400)));

    var functionId = "invalid_function";
    ResponseDTO response =
        consentDecisionsApiEndpoint.putConsentDecision(
            functionId, putConsentDecisionRequestDTO, KVNR);

    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getStatusMessage()).contains(errorCode);

    mockConsentDecisionsServer.verify(
        putRequestedFor(urlPathTemplate("/epa/basic/api/v1/consents/{functionid}"))
            .withPathParam("functionid", equalTo(functionId)));
  }
}
