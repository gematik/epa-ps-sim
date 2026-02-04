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
package de.gematik.epa.api.testdriver.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.epa.api.testdriver.poppToken.dto.GetPoppTokenResponseDto;
import de.gematik.epa.api.testdriver.poppToken.dto.TokenGenerationParams;
import de.gematik.epa.api.testdriver.poppToken.dto.TokenParams;
import de.gematik.epa.popp.PoppTokenService;
import java.util.List;
import org.junit.jupiter.api.Test;

class PoppTokenApiImplTest {
  private final PoppTokenService service = mock(PoppTokenService.class);

  @Test
  void generateToken() {
    var tokenGenerationParams = mock(TokenGenerationParams.class);
    var tokenParams = mock(TokenParams.class);
    var mockResponse = mock(GetPoppTokenResponseDto.class);

    when(service.generateToken(any())).thenReturn(mockResponse);
    when(tokenGenerationParams.getTokenParamsList()).thenReturn(List.of(tokenParams));

    var response = service.generateToken(tokenGenerationParams);
    assertThat(response).isNotNull();
  }
}
