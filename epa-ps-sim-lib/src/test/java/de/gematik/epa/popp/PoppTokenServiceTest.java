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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.epa.api.poppToken.client.PoppTokenClientReferenceApi;
import de.gematik.epa.api.testdriver.poppToken.dto.GetPoppTokenResponseDto;
import de.gematik.epa.api.testdriver.poppToken.dto.TokenGenerationParams;
import de.gematik.epa.api.testdriver.poppToken.dto.TokenParams;
import de.gematik.epa.client.JaxRsClientWrapper;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class PoppTokenServiceTest {

  private final JaxRsClientWrapper<PoppTokenClientReferenceApi> docRefClient =
      mock(JaxRsClientWrapper.class);
  private PoppTokenService service;

  @BeforeEach
  void setUp() {
    service = new PoppTokenService(docRefClient);
  }

  @Test
  void generateToken() {
    var tokenGenerationParams = mock(TokenGenerationParams.class);
    var tokenParams = mock(TokenParams.class);
    var mockResponse = mock(Response.class);
    var expectedResult = new GetPoppTokenResponseDto();

    when(docRefClient.getServiceApi()).thenReturn(mock(PoppTokenClientReferenceApi.class));
    when(docRefClient.getServiceApi().generateToken(any())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(200);
    when(mockResponse.readEntity(GetPoppTokenResponseDto.class)).thenReturn(expectedResult);
    when(tokenGenerationParams.getTokenParamsList()).thenReturn(List.of(tokenParams));

    var response = service.generateToken(tokenGenerationParams);
    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isTrue();
  }

  @Test
  void generateToken400() {
    var tokenGenerationParams = mock(TokenGenerationParams.class);
    var tokenParams = mock(TokenParams.class);
    var mockResponse = mock(Response.class);
    var expectedResult = new GetPoppTokenResponseDto();

    when(docRefClient.getServiceApi()).thenReturn(mock(PoppTokenClientReferenceApi.class));
    when(docRefClient.getServiceApi().generateToken(any())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(400);
    when(mockResponse.readEntity(GetPoppTokenResponseDto.class)).thenReturn(expectedResult);
    when(tokenGenerationParams.getTokenParamsList()).thenReturn(List.of(tokenParams));

    var response = service.generateToken(tokenGenerationParams);
    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isFalse();
  }
}
