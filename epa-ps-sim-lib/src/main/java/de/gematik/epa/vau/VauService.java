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
package de.gematik.epa.vau;

import de.gematik.epa.api.testdriver.vau.dto.DestroyVauCidResponseDTO;
import de.gematik.epa.api.vau.client.VauApi;
import de.gematik.epa.api.vau.client.dto.DestroyVauCidResponse;
import de.gematik.epa.client.JaxRsClientWrapper;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VauService {

  private final JaxRsClientWrapper<VauApi> vauApiJaxRsClientWrapper;

  public VauService(JaxRsClientWrapper<VauApi> vauApiJaxRsClientWrapper) {
    this.vauApiJaxRsClientWrapper = vauApiJaxRsClientWrapper;
  }

  private static void prepareResult(
      final DestroyVauCidResponseDTO result, String message, boolean success) {
    result.setSuccess(success);
    result.statusMessage(message);
  }

  public DestroyVauCidResponseDTO destroyVauCid() {
    var result = new DestroyVauCidResponseDTO();

    try {
      Response response = vauApiJaxRsClientWrapper.getServiceApi().destroyVauCid();
      if (response.getStatus() == 200 || response.getStatus() == 404) {
        var responseEntity = response.readEntity(DestroyVauCidResponse.class);
        prepareResult(result, responseEntity.getStatusMessage(), response.getStatus() == 200);
      } else {
        var message = "Error while destroying VAU CID. Status: " + response.getStatus();
        prepareResult(result, message, false);
        log.error(message);
      }
    } catch (Exception e) {
      var message = "Unexpected error occurred while destroying VAU CID";
      prepareResult(result, message, false);
      log.error(message, e);
    }

    return result;
  }
}
