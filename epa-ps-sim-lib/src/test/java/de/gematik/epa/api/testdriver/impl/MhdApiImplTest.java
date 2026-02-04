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

import static de.gematik.epa.unit.util.TestDataFactory.ACCEPT_FHIR_JSON;
import static de.gematik.epa.unit.util.TestDataFactory.KVNR;
import static de.gematik.epa.unit.util.TestDataFactory.USER_ID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.epa.api.testdriver.mhd.dto.MhdResponseBundle;
import de.gematik.epa.api.testdriver.mhd.dto.ResponseDTO;
import de.gematik.epa.mhd.MhdService;
import java.io.File;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MhdApiImplTest {

  private static final String FORMAT = "format";
  private static final String CURRENT = "current";

  private final MhdService mhdService = mock(MhdService.class);
  private final MhdApiImpl mhdApiImpl = new MhdApiImpl(mhdService);

  @Test
  void epaTestdriverApiV1MhdSetupGet() {
    var expectedStatusMessage = "Indexing successful";
    when(mhdService.epaMhdClientApiV1MhdSetupGet())
        .thenReturn(new ResponseDTO().success(true).statusMessage(expectedStatusMessage));

    var responseDTO = mhdApiImpl.epaTestdriverApiV1MhdSetupGet();
    assertThat(responseDTO.getSuccess()).isTrue();
    assertThat(responseDTO.getStatusMessage()).isEqualTo(expectedStatusMessage);
  }

  @Test
  void epaTestdriverApiV1MhdCleanupGet() {
    var expectedStatusMessage = "removal successful";
    when(mhdService.epaMhdClientApiV1MhdCleanupGet())
        .thenReturn(new ResponseDTO().success(true).statusMessage(expectedStatusMessage));

    var responseDTO = mhdApiImpl.epaTestdriverApiV1MhdCleanupGet();
    assertThat(responseDTO.getSuccess()).isTrue();
    assertThat(responseDTO.getStatusMessage()).isEqualTo(expectedStatusMessage);
  }

  @Test
  void epaTestdriverApiV1MdhFhirDocumentReferenceGet() {
    var expectedBundle = "DOCUMENT JSON";
    when(mhdService.epaMhdClientApiV1MdhFhirDocumentReferenceGet(
            eq(KVNR),
            eq(USER_ID),
            eq(CURRENT),
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
        .thenReturn(new MhdResponseBundle().success(true).bundle(expectedBundle));

    var mhdResponseBundle =
        mhdApiImpl.epaTestdriverApiV1MdhFhirDocumentReferenceGet(
            KVNR,
            USER_ID,
            CURRENT,
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
    assertThat(mhdResponseBundle.getBundle()).isEqualTo(expectedBundle);
  }

  @Test
  void epaTestdriverApiV1MdhFhirDocumentReferenceSearchPost() {
    var expectedBundle = "DOCUMENT JSON";
    when(mhdService.epaTestdriverApiV1MdhFhirDocumentReferenceSearchPost(
            eq(KVNR),
            eq(USER_ID),
            eq(CURRENT),
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
        .thenReturn(new MhdResponseBundle().success(true).bundle(expectedBundle));

    var mhdResponseBundle =
        mhdApiImpl.epaTestdriverApiV1MdhFhirDocumentReferenceSearchPost(
            KVNR,
            USER_ID,
            CURRENT,
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
    assertThat(mhdResponseBundle.getBundle()).isEqualTo(expectedBundle);
  }

  @Test
  void epaTestdriverApiV1MdhFhirDocumentReferenceIdGet() {
    var expectedStatusMessage = "Indexing successful";
    when(mhdService.epaMhdApiV1FhirDocumentReferenceIdGet(
            eq(KVNR), eq(USER_ID), any(UUID.class), eq(ACCEPT_FHIR_JSON), eq(FORMAT)))
        .thenReturn(new ResponseDTO().success(true).statusMessage(expectedStatusMessage));

    var responseDTO =
        mhdApiImpl.epaTestdriverApiV1MdhFhirDocumentReferenceIdGet(
            KVNR, USER_ID, ACCEPT_FHIR_JSON, FORMAT);
    assertThat(responseDTO.getSuccess()).isTrue();
    assertThat(responseDTO.getStatusMessage()).isEqualTo(expectedStatusMessage);
  }

  @Test
  void retrieveDocumentMHDSvc() {
    var expectedFile = new File("src/test/resources/test.pdf");
    when(mhdService.epaMhdRetrieveV1Content(
            eq(KVNR), eq(USER_ID), eq("pdf"), any(UUID.class), eq(ACCEPT_FHIR_JSON)))
        .thenReturn(expectedFile);

    var file = mhdApiImpl.retrieveDocumentMHDSvc(KVNR, USER_ID, "pdf", ACCEPT_FHIR_JSON);
    assertThat(file).isEqualTo(expectedFile);
  }
}
