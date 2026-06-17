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

import de.gematik.epa.api.testdriver.organization.OrganizationApi;
import de.gematik.epa.api.testdriver.organization.dto.GetOrganizationResponseDTO;
import de.gematik.epa.organization.OrganizationSearch;
import de.gematik.epa.organization.OrganizationService;
import de.gematik.epa.utils.MiscUtils;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrganizationApiImpl implements OrganizationApi {

  private final OrganizationService organizationService;

  @Override
  public GetOrganizationResponseDTO getOrganizations(
      String insurantId,
      UUID requestId,
      String useragent,
      Integer count,
      Integer offset,
      String total,
      String id,
      String format,
      String lastUpdated,
      String identifier,
      String name,
      String has) {

    if (id == null) {
      var organizationSearch =
          new OrganizationSearch()
              .insurantId(insurantId)
              .requestId(requestId)
              .useragent(useragent)
              .count(Optional.ofNullable(count).orElse(MiscUtils.DEFAULT_COUNT))
              .offset(Optional.ofNullable(offset).orElse(MiscUtils.DEFAULT_OFFSET))
              .total(total)
              .format(format)
              .lastUpdated(lastUpdated)
              .identifier(identifier)
              .name(name)
              .has(has);

      return organizationService.getOrganizations(organizationSearch);
    }
    return organizationService.getOrganizationById(id);
  }
}
