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
package de.gematik.epa.ps.testdriver;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.epa.api.psTestdriver.dto.Action;
import de.gematik.epa.api.psTestdriver.dto.InsertEgk;
import de.gematik.epa.api.psTestdriver.dto.Status;
import de.gematik.epa.ps.kob.services.KobActionsService;
import de.gematik.epa.ps.kob.services.KobSystemService;
import de.gematik.epa.utils.HealthRecordProvider;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;

@Slf4j
class InsertEgkIntegrationTest extends AbstractTestdriverIntegrationTest {

  @Autowired private KobSystemService kobSystemService;
  @Autowired private KobActionsService kobActionsService;

  @BeforeEach
  public void initFdSessions() {
    HealthRecordProvider.addHealthRecord(MY_HAPPY_LITTLE_KVNR, "localhost");
  }

  @SneakyThrows
  @Test
  void shouldLogin_callToService() {
    // VAU-Status returns 'none' for User-Authentication
    mockVauStatusWithNoneAuthentication();

    // when
    val action = kobSystemService.insertEgk(MY_HAPPY_LITTLE_KVNR).retrieveCompletionFuture().get();

    // then
    assertThat(action.getStatus()).isEqualTo(Status.SUCCESSFUL);
    assertThat(kobActionsService.retrieveAction(action.getId()).getStatus())
        .isEqualTo(Status.SUCCESSFUL);

    configureFor(mockIdpServer.getPort());
    verify(getRequestedFor(urlEqualTo("/discoveryDocument")));
    verify(getRequestedFor(urlEqualTo("/idpSig/jwk.json")));
    verify(getRequestedFor(urlEqualTo("/idpEnc/jwk.json")));
    verify(
        getRequestedFor(urlPathMatching("/sign_response"))
            .withQueryParam("client_id", equalTo("ePA")));
    verify(postRequestedFor(urlEqualTo("/sign_response")));

    configureFor(mockAuthzServer.getPort());
    verify(getRequestedFor(urlEqualTo("/epa/authz/v1/getNonce")));
    verify(getRequestedFor(urlEqualTo("/epa/authz/v1/send_authorization_request_sc")));
    verify(postRequestedFor(urlEqualTo("/epa/authz/v1/send_authcode_sc")));
  }

  @SneakyThrows
  @Test
  void e2eTest_shouldLogin() {
    // VAU-Status returns 'none' for User-Authentication
    mockVauStatusWithNoneAuthentication();

    // when
    final var insertEgkResponse =
        performTestdriverCall("/patient/insert-egk", new InsertEgk().patient(MY_HAPPY_LITTLE_KVNR));

    // then
    assertThat(insertEgkResponse.getStatusCode().is2xxSuccessful()).isTrue();

    // wait for action to complete
    final UUID insertEgkActionId = insertEgkResponse.getBody().getId();
    val insertEgkAction = kobActionsService.retrieveAction(insertEgkActionId);
    insertEgkAction.retrieveCompletionFuture().get();

    // assert some backend requests to verify that a login took place
    configureFor(mockIdpServer.getPort());
    verify(
        getRequestedFor(urlPathMatching("/sign_response"))
            .withQueryParam("client_id", equalTo("ePA")));
    verify(postRequestedFor(urlEqualTo("/sign_response")));

    // check the actions API
    val retrievedAction =
        performTestdriverCall("/actions/" + insertEgkActionId, null, Action.class, HttpMethod.GET);
    assertThat(retrievedAction.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(retrievedAction.getBody().getId()).isEqualTo(insertEgkActionId);
    assertThat(retrievedAction.getBody().getStatus()).isEqualTo(Status.SUCCESSFUL);

    val actionsList =
        performTestdriverCall(
            "/actions", null, new ParameterizedTypeReference<List<Action>>() {}, HttpMethod.GET);
    assertThat(actionsList.getBody()).anyMatch(action -> action.getId().equals(insertEgkActionId));
  }
}
