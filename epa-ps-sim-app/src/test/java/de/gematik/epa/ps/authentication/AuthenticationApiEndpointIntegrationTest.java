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
package de.gematik.epa.ps.authentication;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static de.gematik.epa.unit.util.TestDataFactory.SMB_AUT_TELEMATIK_ID;
import static de.gematik.epa.unit.util.TestDataFactory.USER_AGENT;
import static de.gematik.epa.unit.util.TestDataFactory.X_USERAGENT;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.epa.ps.endpoint.AuthenticationApiEndpoint;
import de.gematik.epa.ps.utils.AbstractIntegrationTest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuthenticationApiEndpointIntegrationTest extends AbstractIntegrationTest {

  @Autowired AuthenticationApiEndpoint authenticationApiEndpoint;

  @Test
  void contextLoads() {
    assertThat(authenticationApiEndpoint).isNotNull();
  }

  @Test
  @SneakyThrows
  void shouldLogin() {
    // VAU-Status returns 'none' for User-Authentication
    mockVauStatusWithNoneAuthentication();

    // when
    var loginResult = authenticationApiEndpoint.login(SMB_AUT_TELEMATIK_ID, null, null);

    // then
    assertThat(loginResult).isNotNull();
    assertThat(loginResult.getSuccess()).isTrue();
    assertThat(loginResult.getStatusMessage()).isBlank();
  }

  @Test
  @SneakyThrows
  void shouldHandleSendAuthCodeFailure() {
    var expectedErrorMessage = "Can not get HSM-ID-Token";
    mockAuthzServer.stubFor(
        post(urlEqualTo("/epa/authz/v1/send_authcode_sc"))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(
                matchingJsonPath("$.authorizationCode", containing("eyJ"))
                    .and(matchingJsonPath("$.clientAttest", containing("eyJ"))))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(500)
                    .withBody(
                        "{\"errorCode\":\"internalError\",\"errorDetail\":\""
                            + expectedErrorMessage
                            + "\"}")));

    // when
    var loginResult = authenticationApiEndpoint.login(SMB_AUT_TELEMATIK_ID, null, null);

    // then
    assertThat(loginResult).isNotNull();
    assertThat(loginResult.getSuccess()).isFalse();
    assertThat(loginResult.getStatusMessage()).isNotBlank();
  }

  @Test
  void loginShouldSkipAuthFlowBecauseVauStatusOk() {
    mockVauProxyServer.stubFor(
        get("/VAU-Status")
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        String.format(
                            """
                            {
                            "VAU-Type": "epa",
                            "VAU-Version" : "gematik-3.0",
                            "User-Authentication" : "%s",
                            "KeyID": "wiremockKeyId",
                            "Connection-Start": "%s"
                            }""",
                            SMB_AUT_TELEMATIK_ID, System.currentTimeMillis()))));
    // when
    var loginResult = authenticationApiEndpoint.login(SMB_AUT_TELEMATIK_ID, null, null);

    // then
    assertThat(loginResult).isNotNull();
    assertThat(loginResult.getSuccess()).isTrue();
    assertThat(loginResult.getStatusMessage()).isBlank();
  }
}
