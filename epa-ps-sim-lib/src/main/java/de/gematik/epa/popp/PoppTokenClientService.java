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
package de.gematik.epa.popp;

import de.gematik.epa.api.poppToken.client.PoppTokenClientApi;
import de.gematik.epa.api.poppToken.client.dto.PoppClientRequest;
import de.gematik.epa.api.poppToken.client.dto.PoppClientRequest.CommunicationTypeEnum;
import de.gematik.epa.api.poppToken.client.dto.PoppClientResponse;
import de.gematik.epa.api.testdriver.poppToken.dto.PoppClientRequestDto;
import de.gematik.epa.api.testdriver.poppToken.dto.PoppTokenResponseDto;
import de.gematik.epa.client.JaxRsClientWrapper;
import java.util.List;

public record PoppTokenClientService(JaxRsClientWrapper<PoppTokenClientApi> client) {

  public PoppTokenResponseDto getToken(PoppClientRequestDto testDriverRequest) {
    var clientRequest = mapRequest(testDriverRequest);

    try (var response = client.getServiceApi().createToken(clientRequest)) {
      if (response.getStatus() == 200) {
        var entity = response.readEntity(PoppClientResponse.class);
        var dto = new PoppTokenResponseDto();
        dto.setSuccess(true);
        dto.setTokenResults(List.of(entity.getToken()));
        return dto;
      }
      return new PoppTokenResponseDto()
          .success(false)
          .statusMessage("Error generating token, code: %d".formatted(response.getStatus()));
    }
  }

  private static PoppClientRequest mapRequest(PoppClientRequestDto request) {
    var communicationTypeEnum = CommunicationTypeEnum.fromValue(request.getCommunicationType());
    return new PoppClientRequest()
        .clientSessionId(request.getClientSessionId())
        .communicationType(communicationTypeEnum);
  }
}
