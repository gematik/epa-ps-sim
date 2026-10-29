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

import de.gematik.epa.api.mhd.client.DocumentReferenceApi;
import de.gematik.epa.api.mhd.client.RetrieveDocumentApi;
import de.gematik.epa.api.mhd_dev.client.DevReferenceApi;
import de.gematik.epa.api.testdriver.mhd.dto.MhdResponseBundle;
import de.gematik.epa.api.testdriver.mhd.dto.ResponseDTO;
import de.gematik.epa.client.JaxRsClientWrapper;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.net.URI;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record MhdService(
    String useragent,
    JaxRsClientWrapper<DocumentReferenceApi> documentReferenceApi,
    JaxRsClientWrapper<RetrieveDocumentApi> retrieveDocumentApi,
    JaxRsClientWrapper<DevReferenceApi> devReferenceApi) {

  public ResponseDTO epaMhdClientApiV1MhdSetupGet() {
    log.warn("!!! epaMhdClientApiV1MhdSetupGet");
    return tryBasicResponse(devReferenceApi.getServiceApi().epaMhdSetupGet());
  }

  public ResponseDTO epaMhdClientApiV1MhdCleanupGet() {
    log.warn("!!! epaMhdClientApiV1MhdCleanupGet");
    return tryBasicResponse(devReferenceApi.getServiceApi().epaMhdCleanupGet());
  }

  public ResponseDTO epaMhdApiV1FhirDocumentReferenceIdGet(
      String xInsurantid, String id, UUID xRequestID, String accept, String format) {
    return tryBasicResponse(
        documentReferenceApi
            .getServiceApi()
            .epaMhdApiV1FhirDocumentReferenceIdGet(
                xInsurantid, useragent, id, xRequestID, accept, format));
  }

  public File epaMhdRetrieveV1Content(
      String xInsurantid,
      String documentReferenceMasterIdentifier,
      String fileExtension,
      UUID xRequestID,
      String accept) {
    try (var response =
        retrieveDocumentApi
            .getServiceApi()
            .retrieveDocumentMHDSvc(
                xInsurantid,
                useragent,
                documentReferenceMasterIdentifier,
                fileExtension,
                xRequestID,
                accept)) {
      int httpCode = response.getStatus();
      if (httpCode == 200) {
        return response.readEntity(File.class);
      }
      var contentType = response.getHeaderString("Content-Type");
      throw new WebApplicationException(
          Response.status(httpCode)
              .entity(response.readEntity(String.class))
              .type(contentType != null ? contentType : "application/json")
              .build());
    }
  }

  // region doc ref get
  public MhdResponseBundle epaMhdClientApiV1MdhFhirDocumentReferenceGet(
      String xInsurantid,
      String patientIdentifier,
      String status,
      UUID xRequestID,
      String accept,
      String format,
      String count,
      Integer offset,
      String total,
      String id,
      String content,
      String lastUpdated,
      String authorGiven,
      String authorFamily,
      String authorOrganizationName,
      String category,
      String contentType,
      String creation,
      String date,
      String description,
      String event,
      String facility,
      String formatCode,
      String identifier,
      String language,
      URI patient,
      String period,
      URI related,
      String securityLabel,
      String setting,
      String title,
      String type,
      String unsupported) {
    log.warn("!!! epaMhdClientApiV1MdhFhirDocumentReferenceGet");
    return tryMhdResponseBundle(
        documentReferenceApi
            .getServiceApi()
            .epaMhdApiV1FhirDocumentReferenceGet(
                xInsurantid,
                useragent,
                patientIdentifier,
                status,
                xRequestID,
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
                type,
                unsupported));
  }

  // endregion

  // region doc ref post
  public MhdResponseBundle epaTestdriverApiV1MdhFhirDocumentReferenceSearchPost(
      String xInsurantid,
      String patientIdentifier,
      String status,
      UUID xRequestID,
      String accept,
      String format,
      String count,
      Integer offset,
      String total,
      String id,
      String content,
      String lastUpdated,
      String authorGiven,
      String authorFamily,
      String authorOrganizationName,
      String category,
      String contentType,
      String creation,
      String date,
      String description,
      String event,
      String facility,
      String format2,
      String identifier,
      String language,
      URI patient,
      String period,
      URI related,
      String securityLabel,
      String setting,
      String title,
      String type,
      String unsupported) {
    log.warn("!!! epaTestdriverApiV1MdhFhirDocumentReferenceSearchPost");
    return tryMhdResponseBundle(
        documentReferenceApi
            .getServiceApi()
            .epaMhdApiV1FhirDocumentReferenceSearchPost(
                xInsurantid,
                useragent,
                patientIdentifier,
                status,
                xRequestID,
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
                type,
                unsupported));
  }

  // endregion

  private ResponseDTO tryBasicResponse(Response response) {
    try (response) {
      if (response.getStatus() == 200) {
        return new ResponseDTO().success(true).statusMessage(response.readEntity(String.class));
      } else {
        return new ResponseDTO().success(false).statusMessage("%d".formatted(response.getStatus()));
      }
    }
  }

  private MhdResponseBundle tryMhdResponseBundle(Response response) {
    if (response.getStatus() == 200) {
      return new MhdResponseBundle().success(true).bundle(response.readEntity(String.class));
    } else {
      return new MhdResponseBundle()
          .success(false)
          .statusMessage(
              "%d: %s".formatted(response.getStatus(), response.readEntity(String.class)));
    }
  }
}
