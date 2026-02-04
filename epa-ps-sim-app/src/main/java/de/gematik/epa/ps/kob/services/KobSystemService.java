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
package de.gematik.epa.ps.kob.services;

import static de.gematik.epa.konnektor.client.CertificateServiceClient.*;
import static de.gematik.epa.utils.MiscUtils.X_ACTOR_ID;
import static java.util.Optional.empty;

import de.gematik.epa.api.psTestdriver.dto.Action.TypeEnum;
import de.gematik.epa.api.psTestdriver.dto.ErrorMessage;
import de.gematik.epa.api.psTestdriver.dto.ResetPrimaersystem;
import de.gematik.epa.api.testdriver.impl.AuthenticationApiImpl;
import de.gematik.epa.data.SmbInformation;
import de.gematik.epa.konnektor.SmbInformationProvider;
import de.gematik.epa.ps.kob.config.VauProxyConfiguration;
import de.gematik.epa.ps.kob.util.KobTestdriverAction;
import de.gematik.epa.utils.HealthRecordProvider;
import de.gematik.epa.utils.TelematikIdHolder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KobSystemService {

  // https://github.com/gematik/ePA-Medication/blob/ePA-3.0.3/src/openapi/I_Medication_Service_eML_Render.yaml#L65
  private static final List<String> allowedOids =
      List.of(
          OID_PRAXIS_ARZT,
          OID_ZAHNARZTPRAXIS,
          OID_PRAXIS_PSYCHOTHERAPEUT,
          OID_KRANKENHAUS,
          OID_OEFFENTLICHE_APOTHEKE,
          OID_INSTITUTION_OEGD,
          OID_INSTITUTION_PFLEGE,
          OID_INSTITUTION_GEBURTSHILFE,
          OID_PRAXIS_PHYSIOTHERAPEUT,
          OID_INSTITUTION_ARBEITSMEDIZIN,
          OID_INSTITUTION_VORSORGE_REHA);

  private final KobActionsService kobActionsService;
  private final AuthenticationApiImpl authenticationApi;
  private final SmbInformationProvider smbInformationProvider;
  private final VauProxyConfiguration vauProxyConfiguration;

  public KobTestdriverAction insertEgk(final String patient) {
    return kobActionsService.createTestdriverActionWithErrorOnly(
        TypeEnum.INSERT_EGK,
        () -> {
          log.info("Inserting eGK: {}", patient);
          val telematikId = determineSmcb();
          log.info("Performing Authentication API login, using telematik ID: {}", telematikId);
          authenticationApi.login(telematikId, patient, "<unused parameters>");
          return empty();
        });
  }

  @SneakyThrows
  private Optional<ErrorMessage> doResetVAUSession() {
    log.info("Resetting the VAU session ...");
    try (final HttpClient client = HttpClient.newHttpClient()) {
      var telematikId = TelematikIdHolder.getTelematikId();
      if (telematikId == null) {
        log.warn("No telematik ID found, cannot destroy VAU session, that's okay for now.");
        return empty();
      }
      var requestBuilder =
          HttpRequest.newBuilder()
              .uri(URI.create(getVauHostUrl() + "/destroy"))
              .header(X_ACTOR_ID, telematikId)
              .POST(BodyPublishers.noBody());
      var response = client.send(requestBuilder.build(), BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        log.info("Successfully destroyed VAU session for telematik ID: {}", telematikId);
        return empty();
      } else if (response.statusCode() == 404
          && response.body() != null
          && response.body().contains("VAU identity not found.")) {
        log.info("No VAU session for telematik ID: {}", telematikId);
        return empty();
      } else {
        var msg =
            "Failed to destroy VAU session for telematik ID: %s. Status code: %s, Response: %s"
                .formatted(telematikId, response.statusCode(), response.body());
        log.error(msg);
        return Optional.of(new ErrorMessage().message(msg).details(response.body()));
      }
    }
  }

  private Optional<ErrorMessage> doResetCachedHealthRecords() {
    log.info("Resetting the cached health records ...");
    HealthRecordProvider.getAllHealthRecords()
        .forEach((recordKey, fqdn) -> HealthRecordProvider.clearHealthRecord(recordKey));
    return Optional.empty();
  }

  public KobTestdriverAction reset(final ResetPrimaersystem resetPrimaersystem) {
    return kobActionsService.createTestdriverActionWithErrorOnly(
        TypeEnum.START_PRIMAERSYSTEM,
        () -> {
          log.info("Resetting the system...");
          if (Boolean.TRUE.equals(resetPrimaersystem.getCloseAllEpaSessions())) {
            // it is not an "Or" - it works as "and" but sequentially
            return doResetVAUSession().or(this::doResetCachedHealthRecords);
          }
          return Optional.empty();
        });
  }

  private String getVauHostUrl() {
    return "http://" + vauProxyConfiguration.getHost() + ":" + vauProxyConfiguration.getPort();
  }

  public String determineSmcb() {
    return smbInformationProvider.getCardsInformations().stream()
        .filter(smbInfo -> smbInfo.professionOids().stream().anyMatch(allowedOids::contains))
        .findFirst()
        .map(SmbInformation::telematikId)
        .orElseThrow(() -> new IllegalStateException("No SMC-B found!"));
  }
}
