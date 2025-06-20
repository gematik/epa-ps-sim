/*-
 * #%L
 * epa-ps-sim-lib
 * %%
 * Copyright (C) 2025 gematik GmbH
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */
package de.gematik.epa.api.testdriver.impl;

import de.gematik.epa.LibIheXdsMain;
import de.gematik.epa.api.testdriver.DocumentsApi;
import de.gematik.epa.api.testdriver.dto.request.DeleteObjectsRequestDTO;
import de.gematik.epa.api.testdriver.dto.request.FindRequestDTO;
import de.gematik.epa.api.testdriver.dto.request.PutDocumentsRequestDTO;
import de.gematik.epa.api.testdriver.dto.request.ReplaceDocumentsRequestDTO;
import de.gematik.epa.api.testdriver.dto.request.RetrieveDocumentsRequestDTO;
import de.gematik.epa.api.testdriver.dto.request.UpdateDocumentsRequestDTO;
import de.gematik.epa.api.testdriver.dto.response.FindObjectsResponseDTO;
import de.gematik.epa.api.testdriver.dto.response.ResponseDTO;
import de.gematik.epa.api.testdriver.dto.response.RetrieveDocumentsResponseDTO;
import de.gematik.epa.config.DefaultdataProvider;
import de.gematik.epa.config.InsurantIdBuilder;
import de.gematik.epa.conversion.ResponseUtils;
import de.gematik.epa.document.DocumentInterfaceAssembly;
import de.gematik.epa.document.client.DocumentServiceClient;
import de.gematik.epa.ihe.model.document.DocumentInterface;
import de.gematik.epa.ihe.model.request.DeleteObjectsRequest;
import de.gematik.epa.ihe.model.request.DocumentReplaceRequest;
import de.gematik.epa.ihe.model.request.DocumentSubmissionRequest;
import de.gematik.epa.ihe.model.request.FindRequest;
import de.gematik.epa.ihe.model.request.RestrictedUpdateDocumentRequest;
import de.gematik.epa.ihe.model.request.RetrieveDocumentsRequest;
import de.gematik.epa.ihe.model.response.ProxyFindResponse;
import de.gematik.epa.ihe.model.response.ProxyResponse;
import de.gematik.epa.ihe.model.simple.SubmissionSetMetadata;
import de.gematik.epa.konnektor.KonnektorUtils;
import de.gematik.epa.konnektor.SmbInformationProvider;
import ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import jakarta.xml.ws.WebServiceException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType;
import org.apache.cxf.transport.http.HTTPException;

/**
 * Implementation of the operations of the {@link de.gematik.epa.api.testdriver.DocumentsApi}.<br>
 */
@Accessors(fluent = true)
@RequiredArgsConstructor
@Slf4j
public class DocumentApiImpl implements DocumentsApi {

  protected static final String NOT_ENTITLED = "NotEntitled";
  private final DocumentInterfaceAssembly documentInterfaceAssembly;
  private final DefaultdataProvider defaultdataProvider;
  private final SmbInformationProvider smbInformationProvider;
  private final InsurantIdBuilder insurantIdBuilder;

  @Override
  public ResponseDTO putDocuments(String xInsurantId, PutDocumentsRequestDTO request) {
    log.info("Running operation putDocuments");
    try (var documentServiceClient = newDocumentServiceClient()) {

      var provideAndRegisterRequest =
          LibIheXdsMain.convertDocumentSubmissionRequest(
              toDocumentSubmissionRequest(request, documentServiceClient));
      var provideAndRegisterResponse =
          documentServiceClient.documentRepositoryProvideAndRegisterDocumentSetB(
              provideAndRegisterRequest);

      return toResponseDTO(provideAndRegisterResponse);
    } catch (Exception e) {
      log.error("Operation putDocuments failed with an exception", e);
      return KonnektorUtils.fromThrowable(e);
    }
  }

  @Override
  public RetrieveDocumentsResponseDTO getDocuments(
      String xInsurantId, RetrieveDocumentsRequestDTO request) {
    log.info("Running operation getDocuments");
    try (var documentServiceClient = newDocumentServiceClient()) {
      var iheRequest =
          LibIheXdsMain.convertRetrieveDocumentsRequest(toRetrieveDocumentsRequest(request));

      var iheResponse = documentServiceClient.documentRepositoryRetrieveDocumentSet(iheRequest);

      return toRetrieveDocumentsResponseDTO(iheResponse);
    } catch (WebServiceException f) {
      return handleFaultInRetrieveDocuments(f);
    } catch (Exception e) {
      log.error("Operation getDocuments failed with an exception", e);
      return toRetrieveDocumentsResponseDTO(KonnektorUtils.fromThrowable(e));
    }
  }

  @Override
  public ResponseDTO deleteObjects(String xInsurantId, DeleteObjectsRequestDTO request) {
    log.info("Running operation deleteObjects");
    try (var documentServiceClient = newDocumentServiceClient()) {

      var iheRequest = LibIheXdsMain.convertDeleteObjectsRequest(toDeleteObjectsRequest(request));

      var iheResponse = documentServiceClient.documentRegistryDeleteDocumentSet(iheRequest);

      return toResponseDTO(iheResponse);
    } catch (Exception e) {
      log.error("Operation deleteObjects failed with an exception", e);
      return KonnektorUtils.fromThrowable(e);
    }
  }

  @Override
  public FindObjectsResponseDTO find(String xInsurantId, FindRequestDTO request) {
    log.info("Running operation find");
    try (var documentServiceClient = newDocumentServiceClient()) {

      var fmRequestBody = LibIheXdsMain.convertFindRequest(toFindRequest(request));

      var fmResponse = documentServiceClient.documentRegistryRegistryStoredQuery(fmRequestBody);
      return toFindResponseDTO(ResponseUtils.toProxyFindResponse(fmResponse));
    } catch (WebServiceException f) {
      return handleFaultInFindDocuments(f);
    } catch (Exception e) {
      log.error("Operation find failed with an exception", e);
      return toFindObjectsResponseDTO(KonnektorUtils.fromThrowable(e));
    }
  }

  @Override
  public ResponseDTO replaceDocuments(String xInsurantId, ReplaceDocumentsRequestDTO request) {
    log.info("Running operation replaceDocuments");
    try (var documentServiceClient = newDocumentServiceClient()) {

      var provideAndRegisterRequest =
          LibIheXdsMain.convertDocumentReplaceRequest(
              toDocumentReplaceRequest(request, documentServiceClient));

      var provideAndRegisterResponse =
          documentServiceClient.documentRepositoryProvideAndRegisterDocumentSetB(
              provideAndRegisterRequest);

      return toResponseDTO(provideAndRegisterResponse);
    } catch (Exception e) {
      log.error("Operation replaceDocuments failed with an exception", e);
      return KonnektorUtils.fromThrowable(e);
    }
  }

  @Override
  public ResponseDTO updateDocuments(String xInsurantId, UpdateDocumentsRequestDTO request) {
    log.info("Running operation updateDocuments");
    try (var documentServiceClient = newDocumentServiceClient()) {

      var updateDocumentSetRequest =
          LibIheXdsMain.convertRestrictedUpdateDocumentSetRequest(
              toDocumentUpdateRequest(request, documentServiceClient));
      var updateDocumentSetResponse =
          documentServiceClient.updateResponderRestrictedUpdateDocumentSet(
              updateDocumentSetRequest);

      return toResponseDTO(updateDocumentSetResponse);
    } catch (Exception e) {
      log.error("Operation updateDocuments failed with an exception", e);
      return KonnektorUtils.fromThrowable(e);
    }
  }

  // region private

  private FindObjectsResponseDTO handleFaultInFindDocuments(WebServiceException f) {
    if (f.getCause() instanceof HTTPException fault && fault.getResponseCode() == 403) {
      log.error("Operation failed with an fault", fault);
      return toFindObjectsResponseDTO(fault, NOT_ENTITLED);
    }
    throw f;
  }

  private RetrieveDocumentsResponseDTO handleFaultInRetrieveDocuments(WebServiceException f) {
    if (f.getCause() instanceof HTTPException fault && fault.getResponseCode() == 403) {
      log.error("Operation failed with an fault", fault);
      return toRetrieveDocumentsResponseDTO(fault, NOT_ENTITLED);
    }
    throw f;
  }

  private RetrieveDocumentsResponseDTO toRetrieveDocumentsResponseDTO(
      final HTTPException fault, final String... message) {
    var statusMessage = fault.getResponseCode() + ": " + Arrays.toString(message);
    return new RetrieveDocumentsResponseDTO(false, statusMessage, null);
  }

  private FindObjectsResponseDTO toFindObjectsResponseDTO(
      final HTTPException fault, final String... message) {
    var statusMessage = fault.getResponseCode() + ": " + Arrays.toString(message);
    return new FindObjectsResponseDTO(false, statusMessage, null);
  }

  private DocumentServiceClient newDocumentServiceClient() {
    return new DocumentServiceClient(
        documentInterfaceAssembly, defaultdataProvider, smbInformationProvider);
  }

  private FindObjectsResponseDTO toFindResponseDTO(ProxyFindResponse proxyFindResponse) {
    return new FindObjectsResponseDTO(
        proxyFindResponse.success(),
        proxyFindResponse.statusMessage(),
        proxyFindResponse.registryObjectLists());
  }

  private FindObjectsResponseDTO toFindObjectsResponseDTO(ResponseDTO responseDTO) {
    return new FindObjectsResponseDTO(responseDTO.success(), responseDTO.statusMessage(), null);
  }

  private FindRequest toFindRequest(FindRequestDTO dto) {
    return new FindRequest(
        insurantIdBuilder.buildInsurantId(dto.kvnr()),
        dto.returnType(),
        dto.query(),
        dto.queryData());
  }

  private DocumentSubmissionRequest toDocumentSubmissionRequest(
      PutDocumentsRequestDTO request, DocumentServiceClient documentServiceClient) {
    return new DocumentSubmissionRequest(
        insurantIdBuilder.buildInsurantId(request.kvnr()),
        request.documentSets(),
        getSubmissionSetMetadata(request.documentSets(), documentServiceClient));
  }

  private SubmissionSetMetadata getSubmissionSetMetadata(
      List<? extends DocumentInterface> documentSet, DocumentServiceClient documentServiceClient) {
    return new SubmissionSetMetadata(
        Collections.singletonList(
            Boolean.TRUE.equals(defaultdataProvider.useFirstDocumentAuthorForSubmissionSet())
                ? defaultdataProvider.getSubmissionSetAuthorFromDocuments(documentSet)
                : defaultdataProvider.getSubmissionSetAuthorFromConfig(
                    documentServiceClient.authorInstitutionProvider())),
        null,
        LocalDateTime.now(),
        null,
        null,
        null);
  }

  private ResponseDTO toResponseDTO(ProxyResponse proxyResponse) {
    return new ResponseDTO(proxyResponse.success(), proxyResponse.statusMessage());
  }

  private ResponseDTO toResponseDTO(RegistryResponseType iheResponse) {
    var proxyResponse = ResponseUtils.toProxyResponse(iheResponse);
    return toResponseDTO(proxyResponse);
  }

  private RetrieveDocumentsRequest toRetrieveDocumentsRequest(
      RetrieveDocumentsRequestDTO psRequest) {
    return new RetrieveDocumentsRequest(
        psRequest.repositoryUniqueId(), psRequest.documentUniqueIds());
  }

  private RetrieveDocumentsResponseDTO toRetrieveDocumentsResponseDTO(
      RetrieveDocumentSetResponseType iheResponse) {
    var proxyResponse = LibIheXdsMain.convertRetrieveDocumentSetResponse(iheResponse);
    return new RetrieveDocumentsResponseDTO(
        proxyResponse.success(), proxyResponse.statusMessage(), proxyResponse.documents());
  }

  private RetrieveDocumentsResponseDTO toRetrieveDocumentsResponseDTO(ResponseDTO responseDTO) {
    return new RetrieveDocumentsResponseDTO(
        responseDTO.success(), responseDTO.statusMessage(), List.of());
  }

  private DeleteObjectsRequest toDeleteObjectsRequest(DeleteObjectsRequestDTO psRequest) {
    return new DeleteObjectsRequest(psRequest.entryUUIDs());
  }

  private DocumentReplaceRequest toDocumentReplaceRequest(
      ReplaceDocumentsRequestDTO request, DocumentServiceClient documentServiceClient) {
    return new DocumentReplaceRequest(
        insurantIdBuilder.buildInsurantId(request.kvnr()),
        request.documentSets(),
        getSubmissionSetMetadata(request.documentSets(), documentServiceClient));
  }

  private RestrictedUpdateDocumentRequest toDocumentUpdateRequest(
      UpdateDocumentsRequestDTO request, DocumentServiceClient documentServiceClient) {
    return new RestrictedUpdateDocumentRequest(
        insurantIdBuilder.buildInsurantId(request.kvnr()),
        request.documentMetadataList(),
        getSubmissionSetMetadata(request.documentMetadataList(), documentServiceClient));
  }
  // endregion private
}
