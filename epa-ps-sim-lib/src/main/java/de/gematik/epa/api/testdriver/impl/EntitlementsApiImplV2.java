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

import de.gematik.epa.api.testdriver.entitlement.EntitlementV2Api;
import de.gematik.epa.api.testdriver.entitlement.dto.PostEntitlementRequestDTOV2;
import de.gematik.epa.api.testdriver.entitlement.dto.PostEntitlementResponseDTO;
import de.gematik.epa.entitlement.EntitlementService;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Slf4j
@RequiredArgsConstructor
public class EntitlementsApiImplV2 implements EntitlementV2Api {

  private final EntitlementService entitlementService;

  @Override
  public PostEntitlementResponseDTO postEntitlementPopp(
      String xInsurantid, PostEntitlementRequestDTOV2 postEntitlementRequestDTOV2) {
    return entitlementService.setEntitlementV2(xInsurantid, postEntitlementRequestDTOV2);
  }
}
