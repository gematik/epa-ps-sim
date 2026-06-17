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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.epa.api.poppToken.client.PoppTokenGeneratorApi;
import de.gematik.epa.api.poppToken.client.dto.TokenGenerationParams;
import de.gematik.epa.api.testdriver.poppToken.dto.PoppTokenResponseDto;
import de.gematik.epa.api.testdriver.poppToken.dto.SecurityParams;
import de.gematik.epa.api.testdriver.poppToken.dto.TokenParams;
import de.gematik.epa.api.testdriver.poppToken.dto.TokenRequest;
import de.gematik.epa.client.JaxRsClientWrapper;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@SuppressWarnings("unchecked")
class PoppTokenGeneratorServiceTest {

  private static final JaxRsClientWrapper<PoppTokenGeneratorApi> JAX_RS_CLIENT_WRAPPER =
      mock(JaxRsClientWrapper.class);
  private static PoppTokenGeneratorService generatorService;

  @BeforeAll
  static void setUp() {
    generatorService = new PoppTokenGeneratorService(JAX_RS_CLIENT_WRAPPER);
  }

  @Test
  void generateToken() {
    var tokenGenerationParams = mock(TokenRequest.class);
    var tokenParams =
        new TokenParams()
            .proofMethod("GK")
            .patientProofTime(1700000000L)
            .iat(1700000123L)
            .patientId("X110000001")
            .insurerId("104212059")
            .actorId("actor-1")
            .actorProfessionOid("1.2.276.0.76.4.32");
    var poppApi = mock(PoppTokenGeneratorApi.class);
    var mockResponse = mock(Response.class);
    var expectedResult = new PoppTokenResponseDto();

    when(JAX_RS_CLIENT_WRAPPER.getServiceApi()).thenReturn(poppApi);
    when(poppApi.generateToken(any())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(200);
    when(mockResponse.readEntity(PoppTokenResponseDto.class)).thenReturn(expectedResult);
    when(tokenGenerationParams.getTokenParamsList()).thenReturn(List.of(tokenParams));

    var response = generatorService.generateToken(tokenGenerationParams);
    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isTrue();

    var captor = ArgumentCaptor.forClass(TokenGenerationParams.class);
    try (Response ignored = verify(poppApi).generateToken(captor.capture())) {
      var clientParams = captor.getValue();
      assertThat(clientParams.getTokenParamsList()).hasSize(1);
      var mapped = clientParams.getTokenParamsList().getFirst();
      assertThat(mapped.getProofMethod()).isEqualTo("GK");
      assertThat(mapped.getPatientProofTime()).isEqualTo(1700000000L);
      assertThat(mapped.getIat()).isEqualTo(1700000123L);
      assertThat(mapped.getPatientId()).isEqualTo("X110000001");
      assertThat(mapped.getInsurerId()).isEqualTo("104212059");
      assertThat(mapped.getActorId()).isEqualTo("actor-1");
      assertThat(mapped.getActorProfessionOid()).isEqualTo("1.2.276.0.76.4.32");
    }
  }

  @Test
  void generateTokenMapsNullIat() {
    var tokenGenerationParams = mock(TokenRequest.class);
    var tokenParams =
        new TokenParams()
            .patientId("X110000001")
            .insurerId("104212059")
            .actorId("actor-1")
            .actorProfessionOid("1.2.276.0.76.4.32");
    var poppApi = mock(PoppTokenGeneratorApi.class);
    var mockResponse = mock(Response.class);

    when(JAX_RS_CLIENT_WRAPPER.getServiceApi()).thenReturn(poppApi);
    when(poppApi.generateToken(any())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(200);
    when(mockResponse.readEntity(PoppTokenResponseDto.class))
        .thenReturn(new PoppTokenResponseDto());
    when(tokenGenerationParams.getTokenParamsList()).thenReturn(List.of(tokenParams));

    generatorService.generateToken(tokenGenerationParams);

    var captor = ArgumentCaptor.forClass(TokenGenerationParams.class);
    try (var ignored = verify(poppApi).generateToken(captor.capture())) {
      assertThat(captor.getValue().getTokenParamsList().getFirst().getIat()).isNull();
    }
  }

  @Test
  void generateTokenWithCustomSecurityParams() {
    var tokenGenerationParams = mock(TokenRequest.class);
    var tokenParams = mock(TokenParams.class);
    var securityParams = mock(SecurityParams.class);
    var mockResponse = mock(Response.class);
    var expectedResult = new PoppTokenResponseDto();

    when(JAX_RS_CLIENT_WRAPPER.getServiceApi()).thenReturn(mock(PoppTokenGeneratorApi.class));
    when(JAX_RS_CLIENT_WRAPPER.getServiceApi().generateToken(any())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(200);
    when(mockResponse.readEntity(PoppTokenResponseDto.class)).thenReturn(expectedResult);
    when(tokenGenerationParams.getTokenParamsList()).thenReturn(List.of(tokenParams));
    when(tokenGenerationParams.getSecurityParams()).thenReturn(securityParams);

    var response = generatorService.generateToken(tokenGenerationParams);
    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isTrue();
  }

  @Test
  void generateToken400() {
    var tokenGenerationParams = mock(TokenRequest.class);
    var tokenParams = mock(TokenParams.class);
    var mockResponse = mock(Response.class);
    var expectedResult = new PoppTokenResponseDto();

    when(JAX_RS_CLIENT_WRAPPER.getServiceApi()).thenReturn(mock(PoppTokenGeneratorApi.class));
    when(JAX_RS_CLIENT_WRAPPER.getServiceApi().generateToken(any())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(400);
    when(mockResponse.readEntity(PoppTokenResponseDto.class)).thenReturn(expectedResult);
    when(tokenGenerationParams.getTokenParamsList()).thenReturn(List.of(tokenParams));

    var response = generatorService.generateToken(tokenGenerationParams);
    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isFalse();
  }
}
