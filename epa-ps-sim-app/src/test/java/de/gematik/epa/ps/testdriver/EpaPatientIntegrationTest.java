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

import de.gematik.epa.api.psTestdriver.dto.Action;
import de.gematik.epa.api.psTestdriver.dto.Status;
import de.gematik.epa.ps.kob.services.KobActionsService;
import lombok.SneakyThrows;
import lombok.val;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

class EpaPatientIntegrationTest extends AbstractTestdriverIntegrationTest {

  @Autowired private KobActionsService kobActionsService;

  @SneakyThrows
  @ParameterizedTest()
  @ValueSource(
      strings = {
        "/patient/epa/get-status",
        "/patient/epa/get-session",
        "/patient/epa/get-entitlement"
      })
  void patientEpaTest(String endpoint) {
    mockVauStatusWithNoneAuthentication();
    stubSuccessfulGetRecordStatus(mockInformationServer1, 204);
    val consentPermit =
        """
              {
                "data" : [ {
                  "functionId" : "medication",
                  "decision" : "permit"
                } ]
              }
              """;
    stubGetConsentDecisions(mockInformationServer1, 200, consentPermit);

    var headers = new HttpHeaders();
    headers.set("kvnr", MY_HAPPY_LITTLE_KVNR);
    val getStatusAction =
        performTestdriverCall(endpoint, null, Action.class, HttpMethod.POST, headers);

    // wait for action to complete
    val actionId = getStatusAction.getBody().getId();
    val action = kobActionsService.retrieveAction(actionId);
    action.retrieveCompletionFuture().get();

    // check the actions API
    val retrievedAction =
        performTestdriverCall("/actions/" + actionId, null, Action.class, HttpMethod.GET);
    Assertions.assertThat(retrievedAction.getStatusCode().is2xxSuccessful()).isTrue();
    Assertions.assertThat(retrievedAction.getBody().getId()).isEqualTo(actionId);
    Assertions.assertThat(retrievedAction.getBody().getStatus()).isEqualTo(Status.SUCCESSFUL);
  }
}
