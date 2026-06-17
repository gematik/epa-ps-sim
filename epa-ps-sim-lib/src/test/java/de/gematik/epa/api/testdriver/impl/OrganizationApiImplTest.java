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

import de.gematik.epa.api.testdriver.organization.dto.GetOrganizationResponseDTO;
import de.gematik.epa.organization.OrganizationService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrganizationApiImplTest {

  private OrganizationService organizationService;
  private OrganizationApiImpl organizationApi;

  @BeforeEach
  void setup() {
    organizationService = mock(OrganizationService.class);
    organizationApi = new OrganizationApiImpl(organizationService);
  }

  @Test
  void shouldGetOrganizationById() {
    // given
    var id = "org-123";
    var organization = "{\"resourceType\":\"Organization\",\"id\":\"org-123\"}";
    var expectedResponse =
        new GetOrganizationResponseDTO().organizations(List.of(organization)).success(true);
    when(organizationService.getOrganizationById(id)).thenReturn(expectedResponse);

    // when
    var response =
        organizationApi.getOrganizations(
            null, null, null, null, null, null, id, null, null, null, null, null);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getOrganizations()).hasSize(1);
    assertThat(response.getOrganizations().getFirst()).isEqualTo(organization);
    assertThat(response.getStatusMessage()).isBlank();
    assertThat(response).isEqualTo(expectedResponse);
  }

  @Test
  void shouldSearchForOrganizations() {
    // given
    var organization = "{\"resourceType\":\"Organization\",\"id\":\"org-1\"}";
    var expectedResponse =
        new GetOrganizationResponseDTO().organizations(List.of(organization)).success(true);
    when(organizationService.getOrganizations(any())).thenReturn(expectedResponse);

    // when
    var response =
        organizationApi.getOrganizations(
            "insurantId",
            UUID.randomUUID(),
            "useragent",
            10,
            0,
            "accurate",
            null,
            "application/fhir+json",
            "gt2023-01-01",
            "IK123",
            "Test Hospital",
            null);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getOrganizations()).hasSize(1);
    assertThat(response.getStatusMessage()).isBlank();
    assertThat(response).isEqualTo(expectedResponse);
  }

  @Test
  void shouldSearchForOrganizationsWithDefaultCountAndOffset() {
    // given
    var organization = "{\"resourceType\":\"Organization\",\"id\":\"org-2\"}";
    var expectedResponse =
        new GetOrganizationResponseDTO().organizations(List.of(organization)).success(true);
    when(organizationService.getOrganizations(any())).thenReturn(expectedResponse);

    // when
    var response =
        organizationApi.getOrganizations(
            "insurantId",
            UUID.randomUUID(),
            "useragent",
            null,
            null,
            null,
            null,
            null,
            null,
            "IK123",
            null,
            null);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getOrganizations()).hasSize(1);
    assertThat(response.getStatusMessage()).isBlank();
  }

  @Test
  void shouldReturnNotFoundStatusMessageWhenOrganizationNotFoundById() {
    // given
    var id = "unknown-id";
    var expectedResponse =
        new GetOrganizationResponseDTO()
            .success(true)
            .statusMessage("No organization found for ID: " + id);
    when(organizationService.getOrganizationById(id)).thenReturn(expectedResponse);

    // when
    var response =
        organizationApi.getOrganizations(
            null, null, null, null, null, null, id, null, null, null, null, null);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getOrganizations()).isEmpty();
    assertThat(response.getStatusMessage()).contains("No organization found for ID");
  }

  @Test
  void shouldReturnNotFoundStatusMessageWhenOrganizationsNotFoundBySearch() {
    // given
    var expectedResponse =
        new GetOrganizationResponseDTO()
            .success(true)
            .statusMessage("No organization found for search params");
    when(organizationService.getOrganizations(any())).thenReturn(expectedResponse);

    // when
    var response =
        organizationApi.getOrganizations(
            "insurantId", null, null, 10, 0, null, null, null, null, "UNKNOWN", null, null);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getOrganizations()).isEmpty();
    assertThat(response.getStatusMessage()).isNotBlank();
  }

  @Test
  void shouldReturnFailureWhenServiceThrowsExceptionOnGetById() {
    // given
    var id = "error-id";
    var expectedResponse =
        new GetOrganizationResponseDTO().success(false).statusMessage("Unexpected error");
    when(organizationService.getOrganizationById(id)).thenReturn(expectedResponse);

    // when
    var response =
        organizationApi.getOrganizations(
            null, null, null, null, null, null, id, null, null, null, null, null);

    // then
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getStatusMessage()).isNotBlank();
  }

  @Test
  void shouldReturnFailureWhenServiceThrowsExceptionOnSearch() {
    // given
    var expectedResponse =
        new GetOrganizationResponseDTO().success(false).statusMessage("Unexpected error");
    when(organizationService.getOrganizations(any())).thenReturn(expectedResponse);

    // when
    var response =
        organizationApi.getOrganizations(
            "insurantId", null, null, 10, 0, null, null, null, null, "IK123", null, null);

    // then
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getStatusMessage()).isNotBlank();
  }
}
