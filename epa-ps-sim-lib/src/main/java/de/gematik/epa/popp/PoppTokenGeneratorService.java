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

import de.gematik.epa.api.poppToken.client.PoppTokenGeneratorApi;
import de.gematik.epa.api.poppToken.client.dto.SecurityParams;
import de.gematik.epa.api.poppToken.client.dto.TokenGenerationParams;
import de.gematik.epa.api.poppToken.client.dto.TokenParams;
import de.gematik.epa.api.testdriver.poppToken.dto.PoppTokenResponseDto;
import de.gematik.epa.api.testdriver.poppToken.dto.TokenRequest;
import de.gematik.epa.client.JaxRsClientWrapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

@Slf4j
public record PoppTokenGeneratorService(JaxRsClientWrapper<PoppTokenGeneratorApi> client) {

  public PoppTokenResponseDto generateToken(TokenRequest testDriverParams) {
    var clientParams = new TokenGenerationParams();
    clientParams.setTokenParamsList(getTokenParams(testDriverParams));
    clientParams.setSecurityParams(getSecurityParams(testDriverParams));

    try (var response = client.getServiceApi().generateToken(clientParams)) {
      if (response.getStatus() == 200) {
        var successDto = response.readEntity(PoppTokenResponseDto.class);
        successDto.setSuccess(true);
        return successDto;
      }
      return new PoppTokenResponseDto()
          .success(false)
          .statusMessage("Error generating token, code: %d".formatted(response.getStatus()));
    }
  }

  private List<TokenParams> getTokenParams(TokenRequest testDriverParams) {
    return testDriverParams.getTokenParamsList().stream()
        .map(
            tp ->
                new TokenParams()
                    .proofMethod(tp.getProofMethod())
                    .patientProofTime(tp.getPatientProofTime())
                    .iat(tp.getIat())
                    .patientId(tp.getPatientId())
                    .insurerId(tp.getInsurerId())
                    .actorId(tp.getActorId())
                    .actorProfessionOid(tp.getActorProfessionOid()))
        .toList();
  }

  private static @Nullable SecurityParams getSecurityParams(TokenRequest testDriverParams) {
    var sp = testDriverParams.getSecurityParams();
    if (sp != null) {
      return new SecurityParams()
          .keyAlias(sp.getKeyAlias())
          .keyPass(sp.getKeyPass())
          .storeContent(sp.getStoreContent())
          .storePass(sp.getStorePass());
    }
    return null;
  }
}
