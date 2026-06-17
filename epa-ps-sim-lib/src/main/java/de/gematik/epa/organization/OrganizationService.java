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
package de.gematik.epa.organization;

import static de.gematik.epa.utils.StringUtils.appendCauses;
import static org.apache.commons.lang3.StringUtils.*;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import de.gematik.epa.api.testdriver.organization.dto.GetOrganizationResponseDTO;
import de.gematik.epa.fhir.client.FhirClient;
import de.gematik.epa.medication.SearchUtils;
import de.gematik.epa.utils.FhirUtils;
import de.gematik.epa.utils.MiscUtils;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;

@Slf4j
public class OrganizationService {
  private final FhirClient fhirClient;

  public OrganizationService(FhirClient fhirClient) {
    this.fhirClient = fhirClient;
  }

  private static GetOrganizationResponseDTO createResponseForException(Exception e) {
    var statusMsgBuilder = new StringBuilder().append(e);

    return new GetOrganizationResponseDTO()
        .success(false)
        .statusMessage(appendCauses(e, statusMsgBuilder).toString());
  }

  public GetOrganizationResponseDTO getOrganizationById(String id) {
    final Organization organization;
    try {
      organization =
          fhirClient.getClient().read().resource(Organization.class).withId(id).execute();
      var organizationAsJson = FhirUtils.asJson(organization);
      return new GetOrganizationResponseDTO()
          .organizations(List.of(organizationAsJson))
          .success(Boolean.TRUE);
    } catch (ResourceNotFoundException e) {
      var statusMessage = "No organization found for ID: " + id;
      log.warn(statusMessage);
      return new GetOrganizationResponseDTO().success(Boolean.TRUE).statusMessage(statusMessage);
    } catch (Exception e) {
      log.error("Error occurred during get organization by id", e);
      return createResponseForException(e);
    }
  }

  public GetOrganizationResponseDTO getOrganizations(OrganizationSearch searchRequest) {
    try {
      final IGenericClient client = fhirClient.getClient();
      final IUntypedQuery<IBaseBundle> search = client.search();

      IQuery<IBaseBundle> baseQuery =
          search
              .forResource(Organization.class)
              .where(Organization.IDENTIFIER.exactly().identifier(searchRequest.identifier()))
              .count(searchRequest.count())
              .offset(searchRequest.offset())
              .totalMode(FhirUtils.calculateTotalMode(searchRequest.total()));

      baseQuery = SearchUtils.addLastUpdated(searchRequest.lastUpdated(), baseQuery);

      if (isNotEmpty(searchRequest.name())) {
        baseQuery = baseQuery.and(Organization.NAME.matches().value(searchRequest.name()));
      }

      final Bundle result = baseQuery.returnBundle(Bundle.class).execute();

      var response = new GetOrganizationResponseDTO();
      if (result.getEntry().isEmpty()) {
        var statusMessage = "No organization found for search params: " + searchRequest;
        log.warn(statusMessage);
        return response.success(Boolean.TRUE).statusMessage(statusMessage);
      }

      var expectedFormat = MiscUtils.expectedFormat(searchRequest.format());
      response.success(Boolean.TRUE).organizations(FhirUtils.extractData(result, expectedFormat));
      return response;
    } catch (Exception e) {
      log.error("Error occurred during search for organization", e);
      return createResponseForException(e);
    }
  }
}
