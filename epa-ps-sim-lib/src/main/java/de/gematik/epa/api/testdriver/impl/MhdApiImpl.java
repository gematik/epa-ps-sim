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

import de.gematik.epa.api.testdriver.mhd.MhdReferenceApi;
import de.gematik.epa.api.testdriver.mhd.dto.MhdResponseBundle;
import de.gematik.epa.api.testdriver.mhd.dto.ResponseDTO;
import de.gematik.epa.mhd.MhdService;
import java.io.File;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@RequiredArgsConstructor
public class MhdApiImpl implements MhdReferenceApi {

  private final MhdService mhdService;

  @Override
  public ResponseDTO epaTestdriverApiV1MhdSetupGet() {
    return mhdService.epaMhdClientApiV1MhdSetupGet();
  }

  @Override
  public ResponseDTO epaTestdriverApiV1MhdCleanupGet() {
    return mhdService.epaMhdClientApiV1MhdCleanupGet();
  }

  @Override
  public MhdResponseBundle epaTestdriverApiV1MdhFhirDocumentReferenceGet(
      String xInsurantid,
      String patientIdentifier,
      String status,
      String accept,
      String format,
      String count,
      Integer offset,
      String total,
      String id,
      String content,
      LocalDate lastUpdated,
      String authorGiven,
      String authorFamily,
      String authorOrganizationName,
      String category,
      String contentType,
      LocalDate creation,
      LocalDate date,
      String description,
      String event,
      String facility,
      String formatCode,
      String identifier,
      String language,
      URI patient,
      LocalDate period,
      URI related,
      String securityLabel,
      String setting,
      String title,
      String type) {
    return mhdService.epaMhdClientApiV1MdhFhirDocumentReferenceGet(
        xInsurantid,
        patientIdentifier,
        status,
        UUID.randomUUID(),
        accept,
        format,
        count,
        offset,
        total,
        id,
        content,
        lastUpdated,
        authorGiven,
        authorFamily,
        authorOrganizationName,
        category,
        contentType,
        creation,
        date,
        description,
        event,
        facility,
        formatCode,
        identifier,
        language,
        patient,
        period,
        related,
        securityLabel,
        setting,
        title,
        type);
  }

  @Override
  public MhdResponseBundle epaTestdriverApiV1MdhFhirDocumentReferenceSearchPost(
      String xInsurantid,
      String patientIdentifier,
      String status,
      String accept,
      String format,
      String count,
      Integer offset,
      String total,
      String id,
      String content,
      LocalDate lastUpdated,
      String authorGiven,
      String authorFamily,
      String authorOrganizationName,
      String category,
      String contentType,
      LocalDate creation,
      LocalDate date,
      String description,
      String event,
      String facility,
      String format2,
      String identifier,
      String language,
      URI patient,
      LocalDate period,
      URI related,
      String securityLabel,
      String setting,
      String title,
      String type) {
    return mhdService.epaTestdriverApiV1MdhFhirDocumentReferenceSearchPost(
        xInsurantid,
        patientIdentifier,
        status,
        UUID.randomUUID(),
        accept,
        format,
        count,
        offset,
        total,
        id,
        content,
        lastUpdated,
        authorGiven,
        authorFamily,
        authorOrganizationName,
        category,
        contentType,
        creation,
        date,
        description,
        event,
        facility,
        format2,
        identifier,
        language,
        patient,
        period,
        related,
        securityLabel,
        setting,
        title,
        type);
  }

  @Override
  public ResponseDTO epaTestdriverApiV1MdhFhirDocumentReferenceIdGet(
      String xInsurantid, String id, String accept, String format) {
    return mhdService.epaMhdApiV1FhirDocumentReferenceIdGet(
        xInsurantid, id, UUID.randomUUID(), accept, format);
  }

  @Override
  public File retrieveDocumentMHDSvc(
      String xInsurantid,
      String documentReferenceMasterIdentifier,
      String fileExtension,
      String accept) {
    return mhdService.epaMhdRetrieveV1Content(
        xInsurantid, documentReferenceMasterIdentifier, fileExtension, UUID.randomUUID(), accept);
  }
}
