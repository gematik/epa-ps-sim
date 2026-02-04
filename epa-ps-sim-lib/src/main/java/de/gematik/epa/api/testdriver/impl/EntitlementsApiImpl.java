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

import de.gematik.epa.api.testdriver.entitlement.EntitlementApi;
import de.gematik.epa.api.testdriver.entitlement.dto.GetBlockedUserListResponseDTO;
import de.gematik.epa.api.testdriver.entitlement.dto.PostEntitlementRequestDTO;
import de.gematik.epa.api.testdriver.entitlement.dto.PostEntitlementResponseDTO;
import de.gematik.epa.api.testdriver.entitlement.dto.ResponseDTO;
import de.gematik.epa.api.testdriver.entitlement.dto.SetBlockedUserRequestDTO;
import de.gematik.epa.entitlement.EntitlementService;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Slf4j
@RequiredArgsConstructor
public class EntitlementsApiImpl implements EntitlementApi {

  private final EntitlementService entitlementService;

  @Override
  public PostEntitlementResponseDTO postEntitlement(
      String xInsurantid, PostEntitlementRequestDTO postEntitlementRequest) {
    return entitlementService.setEntitlement(xInsurantid, postEntitlementRequest);
  }

  @Override
  public GetBlockedUserListResponseDTO getBlockedUserList(String xInsurantid) {
    return entitlementService.getBlockedUserList(xInsurantid);
  }

  @Override
  public ResponseDTO postBlockedUser(
      String xInsurantid, SetBlockedUserRequestDTO setBlockedUserRequestDTO) {
    return entitlementService.setBlockedUser(setBlockedUserRequestDTO, xInsurantid);
  }

  @Override
  public ResponseDTO deleteBlockedUser(String xInsurantid, String telematikId) {
    return entitlementService.deleteBlockedUser(xInsurantid, telematikId);
  }
}
