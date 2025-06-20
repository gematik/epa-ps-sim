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
package de.gematik.epa.ps.kob.services;

import static de.gematik.epa.konnektor.client.CertificateServiceClient.*;
import static de.gematik.epa.utils.MiscUtils.X_TARGET_FQDN;

import de.gematik.epa.api.psTestdriver.dto.Action.TypeEnum;
import de.gematik.epa.api.psTestdriver.dto.ErrorMessage;
import de.gematik.epa.api.psTestdriver.dto.ResetPrimaersystem;
import de.gematik.epa.api.testdriver.impl.AuthenticationApiImpl;
import de.gematik.epa.data.SmbInformation;
import de.gematik.epa.konnektor.SmbInformationProvider;
import de.gematik.epa.ps.kob.config.VauProxyConfiguration;
import de.gematik.epa.ps.kob.util.KobTestdriverAction;
import de.gematik.epa.utils.HealthRecordProvider;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
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
          return Optional.empty();
        });
  }

  public KobTestdriverAction reset(final ResetPrimaersystem resetPrimaersystem) {
    return kobActionsService.createTestdriverActionWithErrorOnly(
        TypeEnum.START_PRIMAERSYSTEM,
        () -> {
          log.info("Resetting the system...");
          if (Boolean.TRUE.equals(resetPrimaersystem.getCloseAllEpaSessions())) {
            log.info(
                "Closing all EPA sessions... (found {} sessions)",
                HealthRecordProvider.getAllHealthRecords().size());
            final Set<Entry<String, String>> sessions =
                new HashSet<>(HealthRecordProvider.getAllHealthRecords().entrySet());
            sessions.add(null); // default session
            val errorMessage =
                sessions.stream()
                    .map(this::closeEpaSession)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst();
            if (errorMessage.isPresent()) {
              return errorMessage;
            }
          }
          return Optional.empty();
        });
  }

  private Optional<ErrorMessage> closeEpaSession(final Map.Entry<String, String> sessionEntry) {
    try (final HttpClient client = HttpClient.newHttpClient()) {
      val insurantId = sessionEntry == null ? "default session" : sessionEntry.getKey();
      log.info("Closing EPA session for insurant ID: {}", insurantId);
      final Builder requestBuilder =
          HttpRequest.newBuilder()
              .uri(URI.create(getVauHostUrl() + "/restart"))
              .POST(BodyPublishers.noBody());
      if (sessionEntry != null) {
        requestBuilder.header(X_TARGET_FQDN, sessionEntry.getValue());
      }
      final HttpRequest request = requestBuilder.build();

      val response = client.send(request, BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        return Optional.of(
            new ErrorMessage().message("Failed to restart VAU").details(response.body()));
      }
      HealthRecordProvider.clearHealthRecord(insurantId);
      return Optional.empty();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return Optional.of(new ErrorMessage().message("Interrupted while closing EPA session"));
    }
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
