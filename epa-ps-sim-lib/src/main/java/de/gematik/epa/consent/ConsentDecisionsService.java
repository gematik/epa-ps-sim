/*-
 * #%L
 * epa-ps-sim-lib
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
package de.gematik.epa.consent;

import de.gematik.epa.api.consent_decision.client.ConsentDecisionsApi;
import de.gematik.epa.api.consent_decision.client.dto.ConsentDecisionType;
import de.gematik.epa.api.consent_decision.client.dto.ConsentDecisionsResponseType;
import de.gematik.epa.api.consent_decision.client.dto.ErrorType;
import de.gematik.epa.api.consent_decision.client.dto.UpdateConsentDecision200Response;
import de.gematik.epa.api.testdriver.consentDecision.dto.ConsentDecisionsResponseTypeDTO;
import de.gematik.epa.api.testdriver.consentDecision.dto.GetConsentDecisionsResponseDTO;
import de.gematik.epa.api.testdriver.consentDecision.dto.PutConsentDecisionRequestDTO;
import de.gematik.epa.api.testdriver.consentDecision.dto.ResponseDTO;
import de.gematik.epa.client.JaxRsClientWrapper;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsentDecisionsService {

  private final JaxRsClientWrapper<ConsentDecisionsApi> consentDecisionsClientWrapper;
  private static final String UNKNOWN_ERROR = "Unknown error";

  public ConsentDecisionsService(
      JaxRsClientWrapper<ConsentDecisionsApi> consentDecisionClientWrapper) {
    this.consentDecisionsClientWrapper = consentDecisionClientWrapper;
  }

  public GetConsentDecisionsResponseDTO getConsentDecisions(String recordId) {
    var responseDTO =
        new GetConsentDecisionsResponseDTO()
            .success(true)
            .statusMessage("Ok. Returns a list of consent decisions");

    try (Response response =
        consentDecisionsClientWrapper
            .getServiceApi()
            .getConsentDecisions(recordId, consentDecisionsClientWrapper.getUserAgent())) {

      if (response == null) {
        setErrorResponse(responseDTO, UNKNOWN_ERROR);
        return responseDTO;
      }
      List<ConsentDecisionsResponseTypeDTO> consentDecisionsDTO = new ArrayList<>();
      switch (response.getStatus()) {
        case 200 -> {
          final UpdateConsentDecision200Response consents =
              response.readEntity(UpdateConsentDecision200Response.class);
          for (final ConsentDecisionsResponseType consent : consents.getData()) {
            final ConsentDecisionsResponseTypeDTO consentDecisionsResponseType =
                new ConsentDecisionsResponseTypeDTO();
            consentDecisionsResponseType
                .decision(
                    ConsentDecisionsResponseTypeDTO.DecisionEnum.fromValue(consent.getDecision()))
                .functionId(
                    ConsentDecisionsResponseTypeDTO.FunctionIdEnum.fromValue(
                        consent.getFunctionId()));
            consentDecisionsDTO.add(consentDecisionsResponseType);
          }
          responseDTO
              .success(true)
              .statusMessage("Ok. Returns a list of consent decisions")
              .data(consentDecisionsDTO);
        }
        case 400, 403, 404, 409, 500 -> {
          var errorType = response.hasEntity() ? response.readEntity(ErrorType.class) : null;
          setErrorResponse(
              responseDTO, errorType != null ? errorType.getErrorCode() : UNKNOWN_ERROR);
        }
        default -> setErrorResponse(responseDTO, UNKNOWN_ERROR);
      }
    } catch (Exception e) {
      log.error("Error occurred while fetching consent decisions: {}", e.getMessage());
      setErrorResponse(responseDTO, UNKNOWN_ERROR);
    }
    return responseDTO;
  }

  private void setErrorResponse(GetConsentDecisionsResponseDTO result, String message) {
    result.success(false).statusMessage(message);
  }

  public ResponseDTO updateConsentDecision(
      String functionid,
      PutConsentDecisionRequestDTO putConsentDecisionRequestDTO,
      String insurantId) {
    var responseDTO = new ResponseDTO().success(true);
    String decisionString = putConsentDecisionRequestDTO.getDecision().toString();
    ConsentDecisionType consentDecisionType = new ConsentDecisionType();
    consentDecisionType.setDecision(ConsentDecisionType.DecisionEnum.fromValue(decisionString));

    try (Response response =
        consentDecisionsClientWrapper
            .getServiceApi()
            .updateConsentDecision(
                insurantId,
                functionid,
                consentDecisionsClientWrapper.getUserAgent(),
                consentDecisionType)) {
      if (response == null) {
        responseDTO.success(false).statusMessage(UNKNOWN_ERROR);

        return responseDTO;
      }
      switch (response.getStatus()) {
        case 200 -> {
          return new ResponseDTO().success(true);
        }
        case 400, 403, 404, 409, 500 -> {
          ErrorType errorType = response.readEntity(ErrorType.class);
          return new ResponseDTO()
              .success(false)
              .statusMessage(errorType != null ? errorType.getErrorCode() : UNKNOWN_ERROR);
        }
        default -> {
          return new ResponseDTO().success(false).statusMessage(UNKNOWN_ERROR);
        }
      }
    } catch (Exception e) {
      return new ResponseDTO().success(false).statusMessage("Error: " + e.getMessage());
    }
  }
}
