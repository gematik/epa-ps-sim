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

import de.gematik.epa.api.poppToken.client.PoppTokenClientReferenceApi;
import de.gematik.epa.api.poppToken.client.dto.TokenParams;
import de.gematik.epa.api.testdriver.poppToken.dto.GetPoppTokenResponseDto;
import de.gematik.epa.api.testdriver.poppToken.dto.TokenGenerationParams;
import de.gematik.epa.client.JaxRsClientWrapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record PoppTokenService(JaxRsClientWrapper<PoppTokenClientReferenceApi> poppRefClient) {

  public GetPoppTokenResponseDto generateToken(TokenGenerationParams testDriverParams) {
    var clientParams = new de.gematik.epa.api.poppToken.client.dto.TokenGenerationParams();

    var mappedTokenParams =
        testDriverParams.getTokenParamsList().stream()
            .map(
                tokenParams -> {
                  var p = new TokenParams();
                  p.setProofMethod(tokenParams.getProofMethod());
                  p.setPatientProofTime(tokenParams.getPatientProofTime());
                  p.setIat(tokenParams.getIat());
                  p.setPatientId(tokenParams.getPatientId());
                  p.setInsurerId(tokenParams.getInsurerId());
                  p.setActorId(tokenParams.getActorId());
                  p.setActorProfessionOid(tokenParams.getActorProfessionOid());
                  return p;
                })
            .toList();

    clientParams.setTokenParamsList(mappedTokenParams);

    try (var response = poppRefClient.getServiceApi().generateToken(clientParams)) {
      if (response.getStatus() == 200) {
        var responseDto = response.readEntity(GetPoppTokenResponseDto.class);
        responseDto.setSuccess(true);
        return responseDto;
      }
      var errorDto = new GetPoppTokenResponseDto();
      errorDto.setSuccess(false);
      errorDto.setStatusMessage("Error generating token, code: %d".formatted(response.getStatus()));
      return errorDto;
    }
  }
}
