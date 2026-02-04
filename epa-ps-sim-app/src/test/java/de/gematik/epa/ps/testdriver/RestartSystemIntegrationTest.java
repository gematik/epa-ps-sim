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
package de.gematik.epa.ps.testdriver;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.epa.api.psTestdriver.dto.Action;
import de.gematik.epa.api.psTestdriver.dto.ResetPrimaersystem;
import de.gematik.epa.api.psTestdriver.dto.Status;
import de.gematik.epa.ps.kob.services.KobActionsService;
import de.gematik.epa.utils.HealthRecordProvider;
import de.gematik.epa.utils.TelematikIdHolder;
import java.util.List;
import java.util.Objects;
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
    TelematikIdHolder.setTelematikId(MY_HAPPY_LITTLE_KVNR);
    mockVauProxyServer.stubFor(
        post(urlEqualTo("/destroy")).willReturn(aResponse().withStatus(200)));
    final var insertEgkResponse =
        performTestdriverCall("/system/reset", new ResetPrimaersystem().closeAllEpaSessions(true));

    // then
    assertThat(insertEgkResponse.getStatus().is2xxSuccessful()).isTrue();

    // wait for action to complete
    final UUID insertEgkActionId =
        Objects.requireNonNull(insertEgkResponse.getResponseBody()).getId();
    kobActionsService.retrieveAction(insertEgkActionId).retrieveCompletionFuture().get();

    // assert some backend requests to verify that a login took place
    configureFor(mockVauProxyServer.getPort());
    verify(postRequestedFor(urlPathMatching("/destroy")));

    // check the actions API
    val retrievedAction =
        performTestdriverCall("/actions/" + insertEgkActionId, null, Action.class, HttpMethod.GET);
    assertThat(retrievedAction.getStatus().is2xxSuccessful()).isTrue();
    assertThat(Objects.requireNonNull(retrievedAction.getResponseBody()).getId())
        .isEqualTo(insertEgkActionId);
    assertThat(retrievedAction.getResponseBody().getStatus()).isEqualTo(Status.SUCCESSFUL);

    val actionsList =
        performTestdriverCall(
            "/actions", null, new ParameterizedTypeReference<List<Action>>() {}, HttpMethod.GET);
    assertThat(actionsList.getResponseBody())
        .anyMatch(action -> Objects.requireNonNull(action.getId()).equals(insertEgkActionId));
  }

  @SneakyThrows
  @Test
  void e2eTest_resetEpaSessionsRandomTelematikId() {
    // when
    TelematikIdHolder.setTelematikId(MY_HAPPY_LITTLE_KVNR + UUID.randomUUID());
    mockVauProxyServer.stubFor(
        post(urlEqualTo("/destroy"))
            .willReturn(aResponse().withStatus(404).withBody("VAU identity not found.")));
    final var insertEgkResponse =
        performTestdriverCall("/system/reset", new ResetPrimaersystem().closeAllEpaSessions(true));

    // then
    assertThat(insertEgkResponse.getStatus().is2xxSuccessful()).isTrue();

    // wait for action to complete
    final UUID insertEgkActionId =
        Objects.requireNonNull(insertEgkResponse.getResponseBody()).getId();
    kobActionsService.retrieveAction(insertEgkActionId).retrieveCompletionFuture().get();

    // assert some backend requests to verify that a login took place
    configureFor(mockVauProxyServer.getPort());
    verify(postRequestedFor(urlPathMatching("/destroy")));

    // check the actions API
    val retrievedAction =
        performTestdriverCall("/actions/" + insertEgkActionId, null, Action.class, HttpMethod.GET);
    assertThat(retrievedAction.getStatus().is2xxSuccessful()).isTrue();
    assertThat(Objects.requireNonNull(retrievedAction.getResponseBody()).getId())
        .isEqualTo(insertEgkActionId);
    assertThat(retrievedAction.getResponseBody().getStatus()).isEqualTo(Status.SUCCESSFUL);

    val actionsList =
        performTestdriverCall(
            "/actions", null, new ParameterizedTypeReference<List<Action>>() {}, HttpMethod.GET);
    assertThat(actionsList.getResponseBody())
        .anyMatch(action -> Objects.equals(action.getId(), insertEgkActionId));
  }

  @Test
  void e2eTest_resetEpaSessionsNoTelematikId() {
    //    TelematikIdHolder.setTelematikId(MY_HAPPY_LITTLE_KVNR); omitted on purpose to test the
    // case without TelematikId
    mockVauProxyServer.stubFor(
        post(urlEqualTo("/destroy")).willReturn(aResponse().withStatus(200)));
    final var insertEgkResponse =
        performTestdriverCall("/system/reset", new ResetPrimaersystem().closeAllEpaSessions(true));

    // then
    assertThat(insertEgkResponse.getStatus().is2xxSuccessful()).isTrue();
  }

  @SneakyThrows
  @Test
  void e2eTest_failShouldPropagate() {
    // when
    TelematikIdHolder.setTelematikId(MY_HAPPY_LITTLE_KVNR);
    mockVauProxyServer.stubFor(
        post(urlEqualTo("/destroy"))
            .willReturn(aResponse().withStatus(400).withBody("My horrible error message")));
    final var insertEgkResponse =
        performTestdriverCall("/system/reset", new ResetPrimaersystem().closeAllEpaSessions(true));

    // then
    assertThat(insertEgkResponse.getStatus().is2xxSuccessful()).isTrue();

    // wait for action to complete
    final UUID insertEgkActionId =
        Objects.requireNonNull(insertEgkResponse.getResponseBody()).getId();
    kobActionsService.retrieveAction(insertEgkActionId).retrieveCompletionFuture().get();

    // check the actions API
    val retrievedAction =
        performTestdriverCall("/actions/" + insertEgkActionId, null, Action.class, HttpMethod.GET);
    assertThat(Objects.requireNonNull(retrievedAction.getResponseBody()).getId())
        .isEqualTo(insertEgkActionId);
    assertThat(retrievedAction.getStatus().is2xxSuccessful()).isTrue();
    assertThat(retrievedAction.getResponseBody().getStatus()).isEqualTo(Status.FAILED);
    assertThat(Objects.requireNonNull(retrievedAction.getResponseBody().getError()).getDetails())
        .contains("horrible");
    assertThat(retrievedAction.getResponseBody().getError().getMessage()).contains("horrible");
  }
}
