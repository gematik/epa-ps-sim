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

import static de.gematik.epa.api.testdriver.poppToken.dto.PoppClientRequestDto.CommunicationTypeEnum.CONTACTLESS_STANDARD;
import static de.gematik.epa.api.testdriver.poppToken.dto.PoppClientRequestDto.CommunicationTypeEnum.CONTACT_STANDARD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.epa.api.poppToken.client.PoppTokenClientApi;
import de.gematik.epa.api.poppToken.client.dto.PoppClientRequest;
import de.gematik.epa.api.poppToken.client.dto.PoppClientResponse;
import de.gematik.epa.api.testdriver.poppToken.dto.PoppClientRequestDto;
import de.gematik.epa.client.JaxRsClientWrapper;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@SuppressWarnings("unchecked")
class PoppTokenClientServiceTest {

  private static final JaxRsClientWrapper<PoppTokenClientApi> JAX_RS_CLIENT_WRAPPER =
      mock(JaxRsClientWrapper.class);
  private static PoppTokenClientService clientService;

  @BeforeAll
  static void setUp() {
    clientService = new PoppTokenClientService(JAX_RS_CLIENT_WRAPPER);
  }

  @Test
  void getToken() {
    var testDriverRequest = new PoppClientRequestDto();
    testDriverRequest.setCommunicationType(CONTACT_STANDARD);
    testDriverRequest.setClientSessionId("session-123");

    var poppApi = mock(PoppTokenClientApi.class);
    var mockResponse = mock(Response.class);
    var poppClientResponse = new PoppClientResponse().token("token-value");

    when(JAX_RS_CLIENT_WRAPPER.getServiceApi()).thenReturn(poppApi);
    when(poppApi.createToken(any())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(200);
    when(mockResponse.readEntity(PoppClientResponse.class)).thenReturn(poppClientResponse);

    var response = clientService.getToken(testDriverRequest);

    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getTokenResults()).containsExactly("token-value");

    var captor = ArgumentCaptor.forClass(PoppClientRequest.class);
    try (var ignored = verify(poppApi).createToken(captor.capture())) {
      var capturedRequest = captor.getValue();
      assertThat(capturedRequest.getCommunicationType())
          .isEqualTo(PoppClientRequest.CommunicationTypeEnum.CONTACT_STANDARD.value());
      assertThat(capturedRequest.getClientSessionId()).isEqualTo("session-123");
    }
  }

  @Test
  void getTokenError() {
    var testDriverRequest = new PoppClientRequestDto();
    testDriverRequest.setCommunicationType(CONTACTLESS_STANDARD);

    var poppApi = mock(PoppTokenClientApi.class);
    var mockResponse = mock(Response.class);

    when(JAX_RS_CLIENT_WRAPPER.getServiceApi()).thenReturn(poppApi);
    when(poppApi.createToken(any())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(400);

    var response = clientService.getToken(testDriverRequest);

    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getStatusMessage()).contains("400");
  }
}
