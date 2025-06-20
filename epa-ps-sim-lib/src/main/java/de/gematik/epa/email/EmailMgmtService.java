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
package de.gematik.epa.email;

import de.gematik.epa.api.email.client.EmailManagementApi;
import de.gematik.epa.api.email.client.dto.EmailRequestType;
import de.gematik.epa.api.email.client.dto.EmailResponseType;
import de.gematik.epa.api.email.client.dto.ErrorType;
import de.gematik.epa.api.testdriver.email.dto.GetEmailAddressResponseDTO;
import de.gematik.epa.api.testdriver.email.dto.PutEmailRequestDTO;
import de.gematik.epa.api.testdriver.email.dto.PutEmailResponseDTO;
import de.gematik.epa.client.JaxRsClientWrapper;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EmailMgmtService {
  private static final String UNKNOWN_ERROR = "Unknown error";
  private static final String ERROR_WHILE_REPLACING_EMAIL = "Error while replacing email: ";
  private final JaxRsClientWrapper<EmailManagementApi> client;

  public EmailMgmtService(JaxRsClientWrapper<EmailManagementApi> client) {
    this.client = client;
  }

  public GetEmailAddressResponseDTO getEmailAddress(String insurantId) {
    GetEmailAddressResponseDTO result = new GetEmailAddressResponseDTO().success(false);

    try {
      final Response response =
          client.getServiceApi().getEmailAddress(client.getUserAgent(), insurantId);

      switch (response.getStatus()) {
        case 200 -> {
          EmailResponseType emailResponseType = response.readEntity(EmailResponseType.class);
          result =
              result
                  .success(true)
                  .email(emailResponseType.getEmail())
                  .actor(emailResponseType.getActor())
                  .createdAt(emailResponseType.getCreatedAt().toString());
        }
        case 400, 403, 404, 409, 500 -> {
          var errorType = response.hasEntity() ? response.readEntity(ErrorType.class) : null;
          setStatusMessage(result, errorType != null ? errorType.getErrorCode() : UNKNOWN_ERROR);
        }
        default -> setStatusMessage(result, UNKNOWN_ERROR);
      }
    } catch (Exception e) {
      log.error("Error occurred while fetching email address: {}", e.getMessage());
      setStatusMessage(result, UNKNOWN_ERROR);
    }

    return result;
  }

  private void setStatusMessage(GetEmailAddressResponseDTO result, String message) {
    result.statusMessage(message);
  }

  public PutEmailResponseDTO replaceEmail(
      String insurantId, PutEmailRequestDTO putEmailRequestDTO) {
    try {
      final Response response =
          client
              .getServiceApi()
              .replaceEmailAddress(
                  client.getUserAgent(),
                  new EmailRequestType().email(putEmailRequestDTO.getEmail()),
                  insurantId);
      return createResponseDTO(response);
    } catch (Exception e) {
      log.error(ERROR_WHILE_REPLACING_EMAIL, e);
      return new PutEmailResponseDTO()
          .success(false)
          .statusMessage(ERROR_WHILE_REPLACING_EMAIL + e.getMessage());
    }
  }

  private PutEmailResponseDTO createResponseDTO(final Response response) {
    switch (response.getStatus()) {
      case 200 -> {
        final var entity = response.readEntity(EmailResponseType.class);
        return new PutEmailResponseDTO()
            .success(true)
            .email(entity.getEmail())
            .actor(entity.getActor())
            .createdAt(entity.getCreatedAt().toString());
      }
      case 400, 403, 404, 409, 500 -> {
        var errorType =
            response.getEntity() != null
                ? response.readEntity(de.gematik.epa.api.email.client.dto.ErrorType.class)
                : null;
        return new PutEmailResponseDTO()
            .success(false)
            .statusMessage(errorType != null ? errorType.getErrorCode() : UNKNOWN_ERROR);
      }
      default -> {
        return new PutEmailResponseDTO().success(false).statusMessage(UNKNOWN_ERROR);
      }
    }
  }
}
