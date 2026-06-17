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
import static de.gematik.epa.unit.util.TestDataFactory.SMB_AUT_TELEMATIK_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.epa.api.testdriver.entitlement.dto.GetBlockedUserListResponseDTO;
import de.gematik.epa.api.testdriver.entitlement.dto.GetBlockedUserListResponseDTOAllOfAssignments;
import de.gematik.epa.api.testdriver.entitlement.dto.PostEntitlementRequestDTO;
import de.gematik.epa.api.testdriver.entitlement.dto.PostEntitlementResponseDTO;
import de.gematik.epa.api.testdriver.entitlement.dto.ResponseDTO;
import de.gematik.epa.api.testdriver.entitlement.dto.SetBlockedUserRequestDTO;
import de.gematik.epa.entitlement.EntitlementService;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntitlementsApiImplV1Test {
  private final EntitlementService entitlementService = mock(EntitlementService.class);
  private EntitlementsApiImplV1 entitlementsApi;

  @BeforeAll
  void setUp() {
    entitlementsApi = new EntitlementsApiImplV1(entitlementService);
  }

  @Test
  void shouldSetEntitlement() {
    final PostEntitlementRequestDTO requestDTO = new PostEntitlementRequestDTO();
    final PostEntitlementResponseDTO responseDTO = new PostEntitlementResponseDTO();
    responseDTO.setSuccess(true);
    responseDTO.setValidTo(OffsetDateTime.now());

    when(entitlementService.setEntitlement(KVNR, requestDTO)).thenReturn(responseDTO);

    var result = entitlementsApi.postEntitlement(KVNR, requestDTO);

    assertThat(result).isEqualTo(responseDTO);
  }

  @Test
  void shouldGetBlockedUserList() {
    var assignments =
        new GetBlockedUserListResponseDTOAllOfAssignments().telematikId("telematikId");
    var expectedResponse =
        new GetBlockedUserListResponseDTO().success(true).assignments(List.of(assignments));

    when(entitlementService.getBlockedUserList(KVNR)).thenReturn(expectedResponse);

    var actualResponse = entitlementsApi.getBlockedUserList(KVNR);
    assertThat(actualResponse).isEqualTo(expectedResponse);
  }

  @Test
  void shouldSetBlockedUser() {
    SetBlockedUserRequestDTO requestDTO =
        new SetBlockedUserRequestDTO()
            .actorId(SMB_AUT_TELEMATIK_ID)
            .oid("1.2.3")
            .displayName("displayName");
    ResponseDTO expectedResponse = new ResponseDTO().success(true);
    when(entitlementService.setBlockedUser(requestDTO, KVNR)).thenReturn(expectedResponse);
    var actualResponse = entitlementsApi.postBlockedUser(KVNR, requestDTO);
    assertThat(actualResponse).isEqualTo(expectedResponse);
  }

  @Test
  void deleteBlockedUser() {
    ResponseDTO expectedResponse = new ResponseDTO().success(true);
    when(entitlementService.deleteBlockedUser(SMB_AUT_TELEMATIK_ID, KVNR))
        .thenReturn(expectedResponse);
    var actualResponse = entitlementsApi.deleteBlockedUser(SMB_AUT_TELEMATIK_ID, KVNR);
    assertThat(actualResponse).isEqualTo(expectedResponse);
  }
}
