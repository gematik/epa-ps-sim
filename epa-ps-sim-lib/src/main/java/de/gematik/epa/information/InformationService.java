/*-
 * #%L
 * epa-ps-sim-lib
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
package de.gematik.epa.information;

import de.gematik.epa.api.information.client.AccountInformationApi;
import de.gematik.epa.api.information.client.ConsentDecisionsApi;
import de.gematik.epa.api.information.client.UserExperienceApi;
import de.gematik.epa.api.information.client.dto.ErrorType;
import de.gematik.epa.api.information.client.dto.GetConsentDecisionInformation200Response;
import de.gematik.epa.api.testdriver.information.dto.*;
import de.gematik.epa.client.JaxRsClientWrapper;
import de.gematik.epa.utils.HealthRecordProvider;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InformationService {

  public static final String UNKNOWN_ERROR = "unknownError";
  public static final String NO_RECORD_FOUND = "noRecordFound";
  public static final String INSURANT_ID_MISSING = "insurantIdMissing";
  public static final String FQDN_IS_MISSING = "fqdnIsMissing";

  private static final String NO_ERROR_MESSAGE_AVAILABLE = "No error message available.";

  private final List<JaxRsClientWrapper<AccountInformationApi>> accountInformationApiClients;
  private final List<JaxRsClientWrapper<ConsentDecisionsApi>> consentDecisionsApiClients;
  private final List<JaxRsClientWrapper<UserExperienceApi>> userExperienceApiClients;

  public InformationService(
      final List<JaxRsClientWrapper<AccountInformationApi>> accountInformationApiClients,
      final List<JaxRsClientWrapper<ConsentDecisionsApi>> consentDecisionsApiClients,
      final List<JaxRsClientWrapper<UserExperienceApi>> userExperienceApiClients) {
    this.accountInformationApiClients = accountInformationApiClients;
    this.consentDecisionsApiClients = consentDecisionsApiClients;
    this.userExperienceApiClients = userExperienceApiClients;
  }

  private static String calculateErrorMessage(Response response) {
    return response.getEntity() != null
        ? response.readEntity(ErrorType.class).getErrorCode()
        : NO_ERROR_MESSAGE_AVAILABLE;
  }

  private static void setStatusMessage(
      final GetRecordStatusResponseDTO result, final String statusMessage) {
    result.statusMessage(statusMessage);
  }

  public GetRecordStatusResponseDTO getRecordStatus(String insurantId) {
    var result = new GetRecordStatusResponseDTO().success(false);
    for (JaxRsClientWrapper<AccountInformationApi> client : accountInformationApiClients) {
      try {
        final Response response =
            client.getServiceApi().getRecordStatus(insurantId, client.getUserAgent());

        int status = response.getStatus();
        switch (status) {
          case 204 -> {
            HealthRecordProvider.addHealthRecord(insurantId, client.getUrl());
            return result
                .success(true)
                .fqdn(HealthRecordProvider.getHealthRecordUrl(insurantId))
                .statusMessage(null);
          }
          case 400, 404, 409, 500 -> {
            var message = calculateErrorMessage(response);
            log.warn("Could not get record: {}, {}", status, message);
            result.setStatusMessage(message);
          }
          default -> {
            return new GetRecordStatusResponseDTO().success(false).statusMessage(NO_RECORD_FOUND);
          }
        }

      } catch (Exception e) {
        log.error("Error while getting record status, {}", e.getMessage());
        setStatusMessage(result.success(false), UNKNOWN_ERROR);
      }
    }

    return result;
  }

  public GetConsentDecisionInformationResponseDTO getConsentDecisionInformation(
      final String insurantId) {

    final String url = HealthRecordProvider.getHealthRecordUrl(insurantId);
    final JaxRsClientWrapper<ConsentDecisionsApi> client =
        consentDecisionsApiClients.stream()
            .filter(clientWrapper -> clientWrapper.getUrl().equals(url))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No client found for url: " + url));
    final Response response =
        client.getServiceApi().getConsentDecisionInformation(insurantId, client.getUserAgent());

    var result = new GetConsentDecisionInformationResponseDTO();
    final List<ConsentDecisionsResponseType> consentDecisions = new ArrayList<>();
    switch (response.getStatus()) {
      case 200 -> {
        final GetConsentDecisionInformation200Response responseDto =
            response.readEntity(GetConsentDecisionInformation200Response.class);
        List<de.gematik.epa.api.information.client.dto.ConsentDecisionsResponseType> consents =
            responseDto.getData();
        for (de.gematik.epa.api.information.client.dto.ConsentDecisionsResponseType consent :
            consents) {
          final ConsentDecisionsResponseType consentDecisionsResponseType =
              new ConsentDecisionsResponseType();
          consentDecisionsResponseType
              .decision(ConsentDecisionsResponseType.DecisionEnum.fromValue(consent.getDecision()))
              .functionId(
                  ConsentDecisionsResponseType.FunctionIdEnum.fromValue(consent.getFunctionId()));
          consentDecisions.add(consentDecisionsResponseType);
        }
        result.success(true).consentDecisions(consentDecisions);
      }
      case 400, 404, 409, 500 -> {
        var errorType = response.getEntity() != null ? response.readEntity(ErrorType.class) : null;
        result
            .success(true)
            .statusMessage(
                errorType != null ? errorType.getErrorCode() : NO_ERROR_MESSAGE_AVAILABLE);
      }
      default -> result.success(false).statusMessage(UNKNOWN_ERROR);
    }

    return result;
  }

  public SetUserExperienceResponseDTO setUserExperienceResult(UxRequestType uxRequestType) {
    var client = userExperienceApiClients.get(0);
    final Response response =
        client
            .getServiceApi()
            .setUserExperienceResult(client.getUserAgent(), convert(uxRequestType));

    switch (response.getStatus()) {
      case 201 -> {
        return new SetUserExperienceResponseDTO().success(true);
      }
      case 400, 404, 409, 500 -> {
        var errorType = response.getEntity() != null ? response.readEntity(ErrorType.class) : null;
        return new SetUserExperienceResponseDTO()
            .success(true)
            .statusMessage(
                errorType != null ? errorType.getErrorCode() : NO_ERROR_MESSAGE_AVAILABLE);
      }
      default -> {
        return new SetUserExperienceResponseDTO().success(false).statusMessage(UNKNOWN_ERROR);
      }
    }
  }

  private de.gematik.epa.api.information.client.dto.UxRequestType convert(
      final UxRequestType uxRequestType) {
    de.gematik.epa.api.information.client.dto.UxRequestType uxRequestTypeApi =
        new de.gematik.epa.api.information.client.dto.UxRequestType();
    return uxRequestTypeApi
        .measurement(uxRequestType.getMeasurement())
        .useCase(
            de.gematik.epa.api.information.client.dto.UxRequestType.UseCaseEnum.fromValue(
                uxRequestType.getUseCase()));
  }

  public ResponseDTO setFqdn(final SetFqdnRequestDTO requestDTO) {
    final ResponseDTO responseDTO = new ResponseDTO().success(true);
    if (requestDTO.getInsurantId() == null || requestDTO.getInsurantId().isEmpty()) {
      return responseDTO.success(false).statusMessage(INSURANT_ID_MISSING);
    }
    if (requestDTO.getFqdn() == null || requestDTO.getFqdn().isEmpty()) {
      return responseDTO.success(false).statusMessage(FQDN_IS_MISSING);
    }

    try {
      final JaxRsClientWrapper<AccountInformationApi> informationApiClient =
          new JaxRsClientWrapper<>(requestDTO.getFqdn(), "Testsuite", AccountInformationApi.class);

      HealthRecordProvider.addHealthRecord(
          requestDTO.getInsurantId(), informationApiClient.getUrl());
      return responseDTO;
    } catch (Exception e) {
      log.error("Error while setting FQDN, {}", e.getMessage());
      return responseDTO.success(false).statusMessage(e.getMessage());
    }
  }
}
