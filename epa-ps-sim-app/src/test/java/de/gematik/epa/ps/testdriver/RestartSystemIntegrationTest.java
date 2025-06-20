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
import de.gematik.epa.api.psTestdriver.dto.ResetPrimaersystem;
import de.gematik.epa.api.psTestdriver.dto.Status;
import de.gematik.epa.ps.kob.services.KobActionsService;
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
class RestartSystemIntegrationTest extends AbstractTestdriverIntegrationTest {

  @Autowired private KobActionsService kobActionsService;

  @BeforeEach
  public void initFdSessions() {
    HealthRecordProvider.addHealthRecord(MY_HAPPY_LITTLE_KVNR, "localhost");
  }

  @SneakyThrows
  @Test
  void e2eTest_resetEpaSessions() {
    // when
    final var insertEgkResponse =
        performTestdriverCall("/system/reset", new ResetPrimaersystem().closeAllEpaSessions(true));

    // then
    assertThat(insertEgkResponse.getStatusCode().is2xxSuccessful()).isTrue();

    // wait for action to complete
    final UUID insertEgkActionId = insertEgkResponse.getBody().getId();
    kobActionsService.retrieveAction(insertEgkActionId).retrieveCompletionFuture().get();

    // assert some backend requests to verify that a login took place
    configureFor(mockVauProxyServer.getPort());
    verify(
        postRequestedFor(urlPathMatching("/restart"))
            .withHeader("x-target-fqdn", equalTo("localhost")));

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

  @SneakyThrows
  @Test
  void e2eTest_failShouldPropagate() {
    // when
    mockVauProxyServer.stubFor(
        post(urlEqualTo("/restart"))
            .willReturn(aResponse().withStatus(400).withBody("My horrible error message")));
    final var insertEgkResponse =
        performTestdriverCall("/system/reset", new ResetPrimaersystem().closeAllEpaSessions(true));

    // then
    assertThat(insertEgkResponse.getStatusCode().is2xxSuccessful()).isTrue();

    // wait for action to complete
    final UUID insertEgkActionId = insertEgkResponse.getBody().getId();
    kobActionsService.retrieveAction(insertEgkActionId).retrieveCompletionFuture().get();

    // check the actions API
    val retrievedAction =
        performTestdriverCall("/actions/" + insertEgkActionId, null, Action.class, HttpMethod.GET);
    assertThat(retrievedAction.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(retrievedAction.getBody().getId()).isEqualTo(insertEgkActionId);
    assertThat(retrievedAction.getBody().getStatus()).isEqualTo(Status.FAILED);
    assertThat(retrievedAction.getBody().getError().getDetails()).contains("horrible");
  }
}
