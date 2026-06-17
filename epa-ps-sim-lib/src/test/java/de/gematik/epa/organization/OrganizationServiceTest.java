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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.SearchTotalModeEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.InvalidResponseException;
import ca.uhn.fhir.rest.gclient.*;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import de.gematik.epa.api.testdriver.organization.dto.GetOrganizationResponseDTO;
import de.gematik.epa.fhir.client.FhirClient;
import de.gematik.epa.utils.FhirUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrganizationServiceTest {

  private final FhirClient fhirClient = mock(FhirClient.class);
  private final FhirContext context = mock(FhirContext.class);
  private final IParser jsonParser = mock(IParser.class);
  private OrganizationService organizationService;

  @BeforeEach
  void setup() {
    when(fhirClient.getContext()).thenReturn(context);
    when(context.newJsonParser()).thenReturn(jsonParser);
    FhirUtils.setJsonParser(jsonParser);
    organizationService = new OrganizationService(fhirClient);
  }

  @Test
  void shouldGetOrganizationById() {
    // given
    var id = "org-123";
    var organization = new Organization();
    organization.setId(id);

    var iReadExecutable = mockFhirGetResourceById(Organization.class, id);
    when(iReadExecutable.execute()).thenReturn(organization);

    var organizationAsJson = "{\"resourceType\":\"Organization\",\"id\":\"org-123\"}";
    when(jsonParser.encodeResourceToString(organization)).thenReturn(organizationAsJson);

    // when
    final GetOrganizationResponseDTO response = organizationService.getOrganizationById(id);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getOrganizations()).hasSize(1);
    assertThat(response.getOrganizations().getFirst()).isEqualTo(organizationAsJson);
    assertThat(response.getStatusMessage()).isBlank();
  }

  @Test
  void getOrganizationByIdShouldReturnSuccessAndStatusMessageWhenNoResourceFound() {
    // given
    var id = "unknown-org";
    var iReadExecutable = mockFhirGetResourceById(Organization.class, id);
    when(iReadExecutable.execute()).thenThrow(ResourceNotFoundException.class);

    // when
    final GetOrganizationResponseDTO response = organizationService.getOrganizationById(id);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getOrganizations()).isEmpty();
    assertThat(response.getStatusMessage()).isNotBlank();
    assertThat(response.getStatusMessage()).contains("No organization found for ID");
    assertThat(response.getStatusMessage()).contains(id);
  }

  @Test
  void getOrganizationByIdShouldReturnFailureOnException() {
    // given
    var id = "org-error";
    var iReadExecutable = mockFhirGetResourceById(Organization.class, id);
    when(iReadExecutable.execute()).thenThrow(InvalidResponseException.class);

    // when
    final GetOrganizationResponseDTO response = organizationService.getOrganizationById(id);

    // then
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getOrganizations()).isEmpty();
    assertThat(response.getStatusMessage()).isNotBlank();
  }

  @Test
  void shouldGetOrganizations() {
    // given
    var searchRequest =
        new OrganizationSearch()
            .identifier("IK123")
            .name("Test Hospital")
            .total("accurate")
            .lastUpdated("gt2023-01-01")
            .count(10)
            .offset(0);

    var organization = new Organization();
    organization.setId("org-1");

    var executable = mockSearchOrganizations();
    var resultBundle = new Bundle();
    var entry = new Bundle.BundleEntryComponent();
    entry.setResource(organization);
    resultBundle.addEntry(entry);
    when(executable.execute()).thenReturn(resultBundle);

    var organizationAsJson = "{\"resourceType\":\"Organization\",\"id\":\"org-1\"}";
    when(jsonParser.encodeResourceToString(organization)).thenReturn(organizationAsJson);

    // when
    final GetOrganizationResponseDTO response = organizationService.getOrganizations(searchRequest);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getOrganizations()).hasSize(1);
    assertThat(response.getStatusMessage()).isBlank();
  }

  @Test
  void getOrganizationsShouldReturnSuccessAndStatusMessageWhenNoResourceFound() {
    // given
    var searchRequest = new OrganizationSearch().identifier("UNKNOWN").count(10).offset(0);

    var executable = mockSearchOrganizations();
    var emptyBundle = new Bundle();
    when(executable.execute()).thenReturn(emptyBundle);

    // when
    final GetOrganizationResponseDTO response = organizationService.getOrganizations(searchRequest);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getOrganizations()).isEmpty();
    assertThat(response.getStatusMessage()).isNotBlank();
    assertThat(response.getStatusMessage()).contains("No organization found for search params");
  }

  @Test
  void getOrganizationsShouldReturnFailureOnException() {
    // given
    var searchRequest = new OrganizationSearch().identifier("IK123").count(10).offset(0);

    var executable = mockSearchOrganizations();
    when(executable.execute()).thenThrow(InvalidResponseException.class);

    // when
    final GetOrganizationResponseDTO response = organizationService.getOrganizations(searchRequest);

    // then
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getOrganizations()).isEmpty();
    assertThat(response.getStatusMessage()).isNotBlank();
  }

  @Test
  void shouldGetOrganizationsWithoutName() {
    // given
    var searchRequest = new OrganizationSearch().identifier("IK123").count(5).offset(0);

    var organization = new Organization();
    organization.setId("org-2");

    var executable = mockSearchOrganizations();
    var resultBundle = new Bundle();
    var entry = new Bundle.BundleEntryComponent();
    entry.setResource(organization);
    resultBundle.addEntry(entry);
    when(executable.execute()).thenReturn(resultBundle);

    var organizationAsJson = "{\"resourceType\":\"Organization\",\"id\":\"org-2\"}";
    when(jsonParser.encodeResourceToString(organization)).thenReturn(organizationAsJson);

    // when
    final GetOrganizationResponseDTO response = organizationService.getOrganizations(searchRequest);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getOrganizations()).hasSize(1);
    assertThat(response.getStatusMessage()).isBlank();
  }

  private <T extends IBaseResource> IReadExecutable<T> mockFhirGetResourceById(
      final Class<T> resourceClass, final String id) {
    final IGenericClient client = mock(IGenericClient.class);
    when(fhirClient.getClient()).thenReturn(client);

    final IRead read = mock(IRead.class);
    when(client.read()).thenReturn(read);

    final IReadTyped<T> resource = mock(IReadTyped.class);
    when(read.resource(resourceClass)).thenReturn(resource);

    final IReadExecutable<T> resourceIReadExecutable = mock(IReadExecutable.class);
    when(resource.withId(id)).thenReturn(resourceIReadExecutable);
    return resourceIReadExecutable;
  }

  private IClientExecutable mockSearchOrganizations() {
    final IGenericClient client = mock(IGenericClient.class);
    when(fhirClient.getClient()).thenReturn(client);

    final IUntypedQuery<IBaseBundle> search = mock(IUntypedQuery.class);
    when(client.search()).thenReturn(search);

    final IQuery<IBaseBundle> bundleIQuery = mock(IQuery.class);
    when(search.forResource(Organization.class)).thenReturn(bundleIQuery);

    final IQuery<IBaseBundle> where = mock(IQuery.class);
    when(bundleIQuery.where(any(ICriterion.class))).thenReturn(where);
    when(where.and(any(ICriterion.class))).thenReturn(where);
    when(where.count(anyInt())).thenReturn(where);
    when(where.offset(anyInt())).thenReturn(where);
    when(where.totalMode(any(SearchTotalModeEnum.class))).thenReturn(where);
    when(where.lastUpdated(any(DateRangeParam.class))).thenReturn(where);

    final IQuery<Bundle> bundle = mock(IQuery.class);
    when(where.returnBundle(Bundle.class)).thenReturn(bundle);
    return bundle;
  }
}
