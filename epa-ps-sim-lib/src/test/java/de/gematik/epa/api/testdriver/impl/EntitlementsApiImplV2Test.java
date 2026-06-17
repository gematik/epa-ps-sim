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

import static de.gematik.epa.unit.util.TestDataFactory.KVNR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.epa.api.testdriver.entitlement.dto.PostEntitlementRequestDTOV2;
import de.gematik.epa.api.testdriver.entitlement.dto.PostEntitlementResponseDTO;
import de.gematik.epa.entitlement.EntitlementService;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntitlementsApiImplV2Test {
  private final EntitlementService entitlementService = mock(EntitlementService.class);
  private EntitlementsApiImplV2 entitlementsApi;

  @BeforeAll
  void setUp() {
    entitlementsApi = new EntitlementsApiImplV2(entitlementService);
  }

  @Test
  void shouldSetEntitlementV2() {
    final PostEntitlementRequestDTOV2 requestDTO = new PostEntitlementRequestDTOV2();
    final PostEntitlementResponseDTO responseDTO = new PostEntitlementResponseDTO();
    responseDTO.setSuccess(true);
    responseDTO.setValidTo(OffsetDateTime.now());

    when(entitlementService.setEntitlementV2(KVNR, requestDTO)).thenReturn(responseDTO);

    var result = entitlementsApi.postEntitlementPopp(KVNR, requestDTO);

    assertThat(result).isEqualTo(responseDTO);
  }
}
