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
package de.gematik.epa.ps.kob.endpoints;

import static de.gematik.epa.ps.kob.util.KobTestdriverAction.mapAction;

import de.gematik.epa.api.psTestdriver.PatientApi;
import de.gematik.epa.api.psTestdriver.dto.*;
import de.gematik.epa.api.psTestdriver.dto.Action.TypeEnum;
import de.gematik.epa.api.testdriver.entitlement.dto.PostEntitlementRequestDTO;
import de.gematik.epa.authentication.AuthenticationService;
import de.gematik.epa.entitlement.EntitlementService;
import de.gematik.epa.information.InformationService;
import de.gematik.epa.medication.MedicationService;
import de.gematik.epa.ps.kob.services.KobActionsService;
import de.gematik.epa.ps.kob.services.KobSystemService;
import de.gematik.epa.ps.kob.util.KobTestdriverAction;
import de.gematik.epa.utils.InsurantIdHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
@Slf4j
public class PatientApiImpl implements PatientApi {

  private final KobSystemService kobSystemService;
  private final KobActionsService kobActionsService;

  private final InformationService informationService;
  private final AuthenticationService authenticationService;
  private final EntitlementService entitlementService;
  private final MedicationService medicationService;

  @Override
  public ResponseEntity<Action> insertEgk(InsertEgk insertEgk) {
    return new ResponseEntity<>(
        mapAction(kobSystemService.insertEgk(insertEgk.getPatient())), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Action> getStatus(String kvnr) {
    ResponseEntity<Action> response =
        ResponseEntity.ok(
            mapAction(
                kobActionsService.createTestdriverAction(
                    KobTestdriverAction.builder()
                        .type(TypeEnum.GET_STATUS)
                        .actionRunnable(() -> doGetStatus(kvnr))
                        .build())));
    InsurantIdHolder.clear();
    return response;
  }

  @Override
  public ResponseEntity<Action> getSession(String kvnr) {
    ResponseEntity<Action> response =
        ResponseEntity.ok(
            mapAction(
                kobActionsService.createTestdriverAction(
                    KobTestdriverAction.builder()
                        .type(TypeEnum.GET_SESSION)
                        .actionRunnable(() -> doGetSession(kvnr))
                        .build())));
    InsurantIdHolder.clear();
    return response;
  }

  @Override
  public ResponseEntity<Action> getEntitlement(String kvnr) {
    ResponseEntity<Action> response =
        ResponseEntity.ok(
            mapAction(
                kobActionsService.createTestdriverAction(
                    KobTestdriverAction.builder()
                        .type(TypeEnum.GET_ENTITLEMENT)
                        .actionRunnable(() -> doGetEntitlement(kvnr))
                        .build())));
    InsurantIdHolder.clear();
    return response;
  }

  @Override
  public ResponseEntity<Action> retrieveEml(EmlRetrieval emlRetrieval) {
    ResponseEntity<Action> response =
        ResponseEntity.ok(
            mapAction(
                kobActionsService.createTestdriverAction(
                    KobTestdriverAction.builder()
                        .type(TypeEnum.RETRIEVE_EML)
                        .actionRunnable(() -> doRetrieveEml(emlRetrieval))
                        .build())));
    InsurantIdHolder.clear();
    return response;
  }

  private KobTestdriverAction doGetStatus(String insurantId) {
    log.info("Checking health record status for patient '{}'", insurantId);
    var recordStatus = informationService.getRecordStatus(insurantId);
    if (recordStatus.getSuccess() != null && recordStatus.getSuccess()) {
      InsurantIdHolder.setInsurantId(insurantId);
      log.info("Health record located at {}", recordStatus.getFqdn());
      var consentDecisionInformation = informationService.getConsentDecisionInformation(insurantId);
      if (consentDecisionInformation.getSuccess() != null
          && consentDecisionInformation.getSuccess()) {
        return new KobTestdriverAction().setStatus(Status.SUCCESSFUL);
      }
      log.info("No consent decision information found for patient '{}'", insurantId);
      return new KobTestdriverAction().setStatus(Status.FAILED);
    }
    log.info(
        "No health record found for patient '{}'. Error message: {}",
        insurantId,
        recordStatus.getStatusMessage());
    return new KobTestdriverAction().setStatus(Status.FAILED);
  }

  private KobTestdriverAction doGetSession(String insurantId) {
    var action = doGetStatus(insurantId);
    var telematikId = kobSystemService.determineSmcb();
    authenticationService.login(telematikId);
    log.info("Created user-session with telematikId {}", telematikId);
    return action;
  }

  private KobTestdriverAction doGetEntitlement(String insurantId) {
    var action = doGetSession(insurantId);
    var telematikId = kobSystemService.determineSmcb();
    var response =
        entitlementService.setEntitlement(
            insurantId, new PostEntitlementRequestDTO().telematikId(telematikId).kvnr(insurantId));
    log.info(
        "New entitlement for {} at health record for patient '{}' valid to {}",
        telematikId,
        insurantId,
        response.getValidTo());
    return action;
  }

  private KobTestdriverAction doRetrieveEml(EmlRetrieval emlRetrieval) {
    var insurantId = emlRetrieval.getPatient();
    var action = doGetEntitlement(insurantId);
    var emlType = emlRetrieval.getEmlType();
    var emlNullMsg = "Unsupported EML type '%s'".formatted(emlType);
    if (emlType == null) {
      return new KobTestdriverAction()
          .setError(new ErrorMessage().message(emlNullMsg))
          .setStatus(Status.FAILED);
    }
    switch (emlType) {
      case PDF -> medicationService.getEmlAsPdf(insurantId);
      case XHTML -> medicationService.getEmlAsXhtml(insurantId);
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, emlNullMsg);
    }
    log.info("Retrieving EML for patient '{}'", insurantId);
    return action;
  }
}
