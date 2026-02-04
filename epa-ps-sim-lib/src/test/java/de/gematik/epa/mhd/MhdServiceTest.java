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
package de.gematik.epa.mhd;

import static de.gematik.epa.unit.util.TestDataFactory.ACCEPT_FHIR_JSON;
import static de.gematik.epa.unit.util.TestDataFactory.KVNR;
import static de.gematik.epa.unit.util.TestDataFactory.USER_AGENT;
import static de.gematik.epa.unit.util.TestDataFactory.USER_ID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.epa.api.mhd.client.DocumentReferenceApi;
import de.gematik.epa.api.mhd.client.RetrieveDocumentApi;
import de.gematik.epa.api.mhd_dev.client.DevReferenceApi;
import de.gematik.epa.client.JaxRsClientWrapper;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class MhdServiceTest {

  private static final String FORMAT = "format";
  private static final String STATUS = "current";
  private static final String DUMMY_BODY = "DOCUMENT JSON";

  private final JaxRsClientWrapper<DocumentReferenceApi> docRefClient =
      mock(JaxRsClientWrapper.class);
  private final JaxRsClientWrapper<RetrieveDocumentApi> retrieveDocClient =
      mock(JaxRsClientWrapper.class);
  private final JaxRsClientWrapper<DevReferenceApi> homeRefClient = mock(JaxRsClientWrapper.class);
  private MhdService mhdService;

  @BeforeEach
  void setUp() {

    mhdService = new MhdService(USER_AGENT, docRefClient, retrieveDocClient, homeRefClient);
  }

  @Test
  void epaMhdClientApiV1MhdSetupGet() {
    var expectedStatus = "Indexing successful";
    when(homeRefClient.getServiceApi()).thenReturn(mock(DevReferenceApi.class));
    when(homeRefClient.getServiceApi().epaMhdSetupGet())
        .thenReturn(Response.status(200).entity(expectedStatus).build());

    var responseDTO = mhdService.epaMhdClientApiV1MhdSetupGet();
    assertThat(responseDTO.getSuccess()).isTrue();
    assertThat(responseDTO.getStatusMessage()).isEqualTo(expectedStatus);
  }

  @Test
  void epaMhdClientApiV1MhdCleanupGet() {
    var expectedStatus = "removal successful";
    when(homeRefClient.getServiceApi()).thenReturn(mock(DevReferenceApi.class));
    when(homeRefClient.getServiceApi().epaMhdCleanupGet())
        .thenReturn(Response.status(200).entity(expectedStatus).build());

    var responseDTO = mhdService.epaMhdClientApiV1MhdCleanupGet();
    assertThat(responseDTO.getSuccess()).isTrue();
    assertThat(responseDTO.getStatusMessage()).isEqualTo(expectedStatus);
  }

  @Test
  void epaMhdApiV1FhirDocumentReferenceIdGet() {
    var expectedStatus = "DocumentReference successfully read";
    when(docRefClient.getServiceApi()).thenReturn(mock(DocumentReferenceApi.class));
    when(docRefClient
            .getServiceApi()
            .epaMhdApiV1FhirDocumentReferenceIdGet(
                anyString(),
                anyString(),
                anyString(),
                any(UUID.class),
                eq(ACCEPT_FHIR_JSON),
                anyString()))
        .thenReturn(Response.status(200).entity(expectedStatus).build());

    var responseDTO =
        mhdService.epaMhdApiV1FhirDocumentReferenceIdGet(
            KVNR, USER_ID, UUID.randomUUID(), ACCEPT_FHIR_JSON, FORMAT);
    assertThat(responseDTO.getSuccess()).isTrue();
    assertThat(responseDTO.getStatusMessage()).isEqualTo(expectedStatus);
  }

  @Test
  void epaMhdRetrieveV1Content() {
    var expectedFile = new File("src/test/resources/test.pdf");
    when(retrieveDocClient.getServiceApi()).thenReturn(mock(RetrieveDocumentApi.class));
    when(retrieveDocClient
            .getServiceApi()
            .retrieveDocumentMHDSvc(
                anyString(), anyString(), anyString(), anyString(), any(UUID.class), anyString()))
        .thenReturn(Response.status(200).entity(expectedFile).build());

    var fileResponse =
        mhdService.epaMhdRetrieveV1Content(
            KVNR, USER_ID, "pdf", UUID.randomUUID(), ACCEPT_FHIR_JSON);
    assertThat(fileResponse).isEqualTo(expectedFile);
  }

  @Test
  void epaMhdClientApiV1MdhFhirDocumentReferenceGet() {
    when(docRefClient.getServiceApi()).thenReturn(mock(DocumentReferenceApi.class));
    when(docRefClient
            .getServiceApi()
            .epaMhdApiV1FhirDocumentReferenceGet(
                anyString(),
                anyString(),
                isNull(),
                anyString(),
                any(UUID.class),
                eq(ACCEPT_FHIR_JSON),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull()))
        .thenReturn(Response.status(200).entity("DOCUMENT JSON").build());
    var mhdResponseBundle =
        mhdService.epaMhdClientApiV1MdhFhirDocumentReferenceGet(
            KVNR,
            null,
            STATUS,
            UUID.randomUUID(),
            ACCEPT_FHIR_JSON,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    assertThat(mhdResponseBundle.getSuccess()).isTrue();
    assertThat(mhdResponseBundle.getStatusMessage()).isNull();
    assertThat(mhdResponseBundle.getBundle()).isEqualTo("DOCUMENT JSON");
  }

  @Test
  void epaTestdriverApiV1MdhFhirDocumentReferenceSearchPost() {
    when(docRefClient.getServiceApi()).thenReturn(mock(DocumentReferenceApi.class));
    when(docRefClient
            .getServiceApi()
            .epaMhdApiV1FhirDocumentReferenceSearchPost(
                eq(KVNR),
                eq(USER_AGENT),
                isNull(),
                eq(STATUS),
                any(UUID.class),
                eq(ACCEPT_FHIR_JSON),
                eq(FORMAT),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull()))
        .thenReturn(Response.status(200).entity("DOCUMENT JSON").build());
    var mhdResponseBundle =
        mhdService.epaTestdriverApiV1MdhFhirDocumentReferenceSearchPost(
            KVNR,
            null,
            STATUS,
            UUID.randomUUID(),
            ACCEPT_FHIR_JSON,
            FORMAT,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    assertThat(mhdResponseBundle.getSuccess()).isTrue();
    assertThat(mhdResponseBundle.getStatusMessage()).isNull();

    assertThat(mhdResponseBundle.getBundle()).isEqualTo(DUMMY_BODY);
  }
}
