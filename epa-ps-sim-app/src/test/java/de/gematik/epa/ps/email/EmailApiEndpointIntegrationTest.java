/*-
 * #%L
 * epa-ps-sim-app
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
package de.gematik.epa.ps.email;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static de.gematik.epa.unit.AppTestDataFactory.clearFqdnProvider;
import static de.gematik.epa.unit.AppTestDataFactory.setupFqdnProvider;
import static de.gematik.epa.unit.util.TestDataFactory.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import de.gematik.epa.api.email.client.dto.ErrorType;
import de.gematik.epa.api.testdriver.email.dto.GetEmailAddressResponseDTO;
import de.gematik.epa.ps.endpoint.EmailApiEndpoint;
import de.gematik.epa.unit.TestDocumentClientConfiguration;
import de.gematik.epa.unit.TestKonnektorClientConfiguration;
import lombok.extern.slf4j.Slf4j;
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
    classes = {TestKonnektorClientConfiguration.class, TestDocumentClientConfiguration.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
class EmailApiEndpointIntegrationTest {

  @RegisterExtension
  static WireMockExtension mockEmailServer =
      WireMockExtension.newInstance().options(wireMockConfig().port(8088)).build();

  @Autowired EmailApiEndpoint emailApiEndpoint;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final String contentTypeJson = "application/json";

  @BeforeEach
  void setup() {
    log.info("Add insurant ID to known KVNRs: {}", KVNR);
    setupFqdnProvider(KVNR);
  }

  @AfterEach
  void tearDown() {
    clearFqdnProvider(KVNR);
  }

  @Test
  void contextLoads() {
    assertThat(emailApiEndpoint).isNotNull();
  }

  @Test
  void getEmailAddressShouldReturnSuccessFor200Response() {
    // given
    stubSuccessfulGetEmailAddress();

    // when
    GetEmailAddressResponseDTO response = emailApiEndpoint.getEmailAddress(KVNR);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getEmail()).isNotBlank();
  }

  @Test
  void getEmailAddressShouldReturnFailureFor400Response() throws JsonProcessingException {
    // given
    var errorType =
        new ErrorType().errorDetail("Request does not match schema").errorCode("malformedRequest");
    var body = objectMapper.writeValueAsString(errorType);
    stubFailedGetEmailAddress(400, body);
    // when
    GetEmailAddressResponseDTO response = emailApiEndpoint.getEmailAddress(KVNR);

    // then
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getEmail()).isNull();
  }

  @Test
  void getEmailAddressShouldReturnFailureFor404Response() throws JsonProcessingException {
    // given
    var errorType = new ErrorType().errorDetail("Email does not exist").errorCode("noResource");
    var body = objectMapper.writeValueAsString(errorType);
    stubFailedGetEmailAddress(404, body);

    // when
    GetEmailAddressResponseDTO response = emailApiEndpoint.getEmailAddress(KVNR);

    // then
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getEmail()).isNull();
  }

  private void stubSuccessfulGetEmailAddress() {
    mockEmailServer.stubFor(
        get(urlEqualTo("/epa/basic/api/v1/emailaddress"))
            .withHeader(X_INSURANTID, equalTo(KVNR))
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE_HEADER, contentTypeJson)
                    .withStatus(200)
                    .withBody(
                        "{ \"email\": \"test@example.com\", \"actor\": \"Test Actor\", \"createdAt\": \"2025-04-22T14:23:01Z\" }")));
  }

  private void stubFailedGetEmailAddress(int statusCode, final String body) {
    mockEmailServer.stubFor(
        get(urlEqualTo("/epa/basic/api/v1/emailaddress"))
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE_HEADER, contentTypeJson)
                    .withStatus(statusCode)
                    .withBody(body)));
  }
}
