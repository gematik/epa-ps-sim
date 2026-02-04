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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.gematik.epa.api.psTestdriver.dto.Action;
import de.gematik.epa.api.psTestdriver.dto.EmlRetrieval;
import de.gematik.epa.api.psTestdriver.dto.EmlType;
import de.gematik.epa.api.psTestdriver.dto.Status;
import de.gematik.epa.ps.kob.services.KobActionsService;
import de.gematik.epa.ps.kob.services.KobSystemService;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

@Slf4j
class DownloadEmlIntegrationTest extends AbstractTestdriverIntegrationTest {

  @Autowired private KobSystemService kobSystemService;
  @Autowired private KobActionsService kobActionsService;

  @SneakyThrows
  @Test
  void e2eTest_shouldDownloadEml() {
    // given
    stubSuccessfulGetRecordStatus(mockInformationServer1, 204);
    var consent200 =
        """
        {
          "data" : [ {
            "functionId" : "medication",
            "decision" : "permit"
          } ]
        }
        """;
    stubGetConsentDecisions(mockInformationServer1, 200, consent200);

    // when
    final var insertEgkResponse =
        performTestdriverCall(
            "/patient/medication/medication-service/eml",
            new EmlRetrieval().emlType(EmlType.PDF).patient(MY_HAPPY_LITTLE_KVNR));

    // then
    assertThat(insertEgkResponse.getStatus().is2xxSuccessful()).isTrue();
    assertNotNull(insertEgkResponse.getResponseBody());

    // wait for action to complete
    final UUID downloadEmlActionId = insertEgkResponse.getResponseBody().getId();
    val insertEgkAction = kobActionsService.retrieveAction(downloadEmlActionId);
    insertEgkAction.retrieveCompletionFuture().get();

    // check the actions API
    val retrievedAction =
        performTestdriverCall(
            "/actions/" + downloadEmlActionId, null, Action.class, HttpMethod.GET);
    assertThat(retrievedAction.getStatus().is2xxSuccessful()).isTrue();
    assertNotNull(retrievedAction.getResponseBody());
    assertThat(retrievedAction.getResponseBody().getId()).isEqualTo(downloadEmlActionId);
    assertThat(retrievedAction.getResponseBody().getStatus()).isEqualTo(Status.SUCCESSFUL);

    // and take screenshot
    val screenshotResponse =
        performTestdriverCall(
            "/actions/" + downloadEmlActionId + "/screenshot", null, byte[].class, HttpMethod.GET);
    assertThat(screenshotResponse.getStatus().is2xxSuccessful()).isTrue();
    assertThat(screenshotResponse.getResponseBody()).isNotNull().hasSizeGreaterThan(100);
  }
}
