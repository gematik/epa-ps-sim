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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.epa.api.testdriver.vau.dto.DestroyVauCidResponseDTO;
import de.gematik.epa.api.vau.client.VauApi;
import de.gematik.epa.api.vau.client.dto.DestroyVauCidResponse;
import de.gematik.epa.client.JaxRsClientWrapper;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VauServiceTest {

  private final JaxRsClientWrapper<VauApi> vauApiJaxRsClientWrapper =
      mock(JaxRsClientWrapper.class);

  private final VauApi vauApi = mock(VauApi.class);
  private final VauService vauService = new VauService(vauApiJaxRsClientWrapper);

  @BeforeEach
  void setup() {
    when(vauApiJaxRsClientWrapper.getServiceApi()).thenReturn(vauApi);
  }

  @Test
  void shouldDestroyVauCidSuccessfully() {
    var response = mock(Response.class);
    var destroyVauCidResponse = new DestroyVauCidResponse();
    destroyVauCidResponse.setStatusMessage("Success");

    when(vauApi.destroyVauCid()).thenReturn(response);
    when(response.getStatus()).thenReturn(200);
    when(response.readEntity(DestroyVauCidResponse.class)).thenReturn(destroyVauCidResponse);

    var result = vauService.destroyVauCid();

    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getStatusMessage()).isEqualTo("Success");
  }

  @Test
  void shouldHandleVauCidNotFound() {
    var response = mock(Response.class);
    var destroyVauCidResponse = new DestroyVauCidResponse();
    destroyVauCidResponse.setStatusMessage("Not Found");

    when(vauApi.destroyVauCid()).thenReturn(response);
    when(response.getStatus()).thenReturn(404);
    when(response.readEntity(DestroyVauCidResponse.class)).thenReturn(destroyVauCidResponse);

    DestroyVauCidResponseDTO result = vauService.destroyVauCid();

    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).isEqualTo("Not Found");
  }

  @Test
  void shouldHandleServerError() {
    var response = mock(Response.class);

    when(vauApi.destroyVauCid()).thenReturn(response);
    when(response.getStatus()).thenReturn(500);

    var result = vauService.destroyVauCid();

    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).isNotBlank();
  }

  @Test
  void shouldHandleUnexpectedException() {
    when(vauApiJaxRsClientWrapper.getServiceApi()).thenReturn(vauApi);
    when(vauApi.destroyVauCid()).thenThrow(new RuntimeException("Unexpected error"));

    var result = vauService.destroyVauCid();

    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).isNotBlank();
  }
}
