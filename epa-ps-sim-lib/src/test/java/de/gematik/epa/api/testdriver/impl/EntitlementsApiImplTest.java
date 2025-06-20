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
package de.gematik.epa.api.testdriver.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.epa.api.entitlement.client.EntitlementsApi;
import de.gematik.epa.api.entitlement.client.UserBlockingApi;
import de.gematik.epa.api.testdriver.entitlement.dto.GetBlockedUserListResponseDTO;
import de.gematik.epa.api.testdriver.entitlement.dto.GetBlockedUserListResponseDTOAllOfAssignments;
import de.gematik.epa.api.testdriver.entitlement.dto.PostEntitlementRequestDTO;
import de.gematik.epa.api.testdriver.entitlement.dto.PostEntitlementResponseDTO;
import de.gematik.epa.api.testdriver.entitlement.dto.ResponseDTO;
import de.gematik.epa.api.testdriver.entitlement.dto.SetBlockedUserRequestDTO;
import de.gematik.epa.client.JaxRsClientWrapper;
import de.gematik.epa.entitlement.EntitlementService;
import de.gematik.epa.konnektor.KonnektorContextProvider;
import de.gematik.epa.konnektor.KonnektorInterfaceAssembly;
import de.gematik.epa.unit.util.TestDataFactory;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntitlementsApiImplTest {
  private final JaxRsClientWrapper<EntitlementsApi> entitlementClientWrapper =
      mock(JaxRsClientWrapper.class);
  private final JaxRsClientWrapper<UserBlockingApi> blockingClientWrapper =
      mock(JaxRsClientWrapper.class);
  private final EntitlementService entitlementService = mock(EntitlementService.class);
  private final String telematikId = "1-23456788";
  private final String recordId = "X12345678";
  private EntitlementsApiImpl entitlementsApi;

  @BeforeAll
  void setUp() {
    KonnektorContextProvider konnektorContextProvider = TestDataFactory.konnektorContextProvider();
    KonnektorInterfaceAssembly konnektorInterface =
        TestDataFactory.konnektorInterfaceAssemblyMock();
    entitlementsApi = new EntitlementsApiImpl(entitlementService);
  }

  @Test
  void shouldSetEntitlement() {
    final PostEntitlementRequestDTO requestDTO = new PostEntitlementRequestDTO();
    final PostEntitlementResponseDTO responseDTO = new PostEntitlementResponseDTO();
    responseDTO.setSuccess(true);
    responseDTO.setValidTo(OffsetDateTime.now());

    when(entitlementService.setEntitlement(recordId, requestDTO)).thenReturn(responseDTO);

    var result = entitlementsApi.postEntitlement(recordId, requestDTO);

    assertThat(result).isEqualTo(responseDTO);
  }

  @Test
  void shouldGetBlockedUserList() {
    var assignments =
        new GetBlockedUserListResponseDTOAllOfAssignments().telematikId("telematikId");
    var expectedResponse =
        new GetBlockedUserListResponseDTO().success(true).assignments(List.of(assignments));

    when(entitlementService.getBlockedUserList(recordId)).thenReturn(expectedResponse);

    var actualResponse = entitlementsApi.getBlockedUserList(recordId);
    assertThat(actualResponse).isEqualTo(expectedResponse);
  }

  @Test
  void shouldSetBlockedUser() {
    SetBlockedUserRequestDTO requestDTO =
        new SetBlockedUserRequestDTO().actorId(telematikId).oid("1.2.3").displayName("displayName");
    ResponseDTO expectedResponse = new ResponseDTO().success(true);
    when(entitlementService.setBlockedUser(requestDTO, recordId)).thenReturn(expectedResponse);
    var actualResponse = entitlementsApi.postBlockedUser(recordId, requestDTO);
    assertThat(actualResponse).isEqualTo(expectedResponse);
  }

  @Test
  void deleteBlockedUser() {
    ResponseDTO expectedResponse = new ResponseDTO().success(true);
    when(entitlementService.deleteBlockedUser(telematikId, recordId)).thenReturn(expectedResponse);
    var actualResponse = entitlementsApi.deleteBlockedUser(telematikId, recordId);
    assertThat(actualResponse).isEqualTo(expectedResponse);
  }
}
