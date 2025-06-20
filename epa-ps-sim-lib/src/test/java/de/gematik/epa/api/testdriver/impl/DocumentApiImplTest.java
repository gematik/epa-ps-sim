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

import static de.gematik.epa.api.testdriver.impl.DocumentApiImpl.NOT_ENTITLED;
import static org.junit.jupiter.api.Assertions.*;

import de.gematik.epa.api.testdriver.dto.request.DeleteObjectsRequestDTO;
import de.gematik.epa.api.testdriver.dto.request.FindRequestDTO;
import de.gematik.epa.api.testdriver.dto.request.PutDocumentsRequestDTO;
import de.gematik.epa.api.testdriver.dto.request.UpdateDocumentsRequestDTO;
import de.gematik.epa.api.testdriver.dto.response.FindObjectsResponseDTO;
import de.gematik.epa.api.testdriver.dto.response.ResponseDTO;
import de.gematik.epa.ihe.model.response.RegistryObjectLists;
import de.gematik.epa.ihe.model.response.RetrieveDocumentElement;
import de.gematik.epa.ihe.model.simple.ByteArray;
import de.gematik.epa.konnektor.SmbInformationProvider;
import de.gematik.epa.unit.util.ResourceLoader;
import de.gematik.epa.unit.util.TestBase;
import de.gematik.epa.unit.util.TestDataFactory;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import jakarta.xml.ws.WebServiceException;
import java.net.URL;
import java.util.List;
import lombok.SneakyThrows;
import oasis.names.tc.ebxml_regrep.xsd.lcm._3.RemoveObjectsRequest;
import oasis.names.tc.ebxml_regrep.xsd.lcm._3.SubmitObjectsRequest;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;
import org.apache.cxf.transport.http.HTTPException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import xds.document.wsdl.IDocumentManagementPortType;

class DocumentApiImplTest extends TestBase {
  private final String X_INSURANT_ID = "X110123123";

  private final DocumentApiImpl xdsDocumentApi =
      new DocumentApiImpl(
          documentInterfaceAssembly(),
          defaultdataProvider(),
          new SmbInformationProvider(
              TestDataFactory.konnektorContextProvider(),
              TestDataFactory.konnektorInterfaceAssemblyMock()),
          insurantIdBuilder());

  IDocumentManagementPortType documentServiceMock = documentInterfaceAssembly().documentService();

  @Test
  void findByPatientIdRequestTest() {
    Mockito.when(
            documentServiceMock.documentRegistryRegistryStoredQuery(
                Mockito.any(AdhocQueryRequest.class)))
        .thenReturn(TestDataFactory.getSuccessResponse());
    FindRequestDTO findRequestDTO = ResourceLoader.findByPatientIdRequest();
    FindObjectsResponseDTO actualResponseDTO = xdsDocumentApi.find(X_INSURANT_ID, findRequestDTO);
    final RegistryObjectLists registryObjectLists = new RegistryObjectLists(null, null, null, null);
    FindObjectsResponseDTO expectedResponseDTO =
        new FindObjectsResponseDTO(
            true, TestDataFactory.REGISTRY_RESPONSE_STATUS_SUCCESS, registryObjectLists);
    assertEquals(expectedResponseDTO, actualResponseDTO);
  }

  @Test
  void findByPatientIdExceptionTest() {
    var exception = new RuntimeException("I am an expected exception");
    Mockito.when(
            documentServiceMock.documentRegistryRegistryStoredQuery(
                Mockito.any(AdhocQueryRequest.class)))
        .thenThrow(exception);
    var findDocumentRequest = ResourceLoader.findByPatientIdRequest();
    var actualResponseDTO =
        assertDoesNotThrow(() -> xdsDocumentApi.find(X_INSURANT_ID, findDocumentRequest));
    assertNotNull(actualResponseDTO);
    assertFalse(actualResponseDTO.success());
    assertTrue(actualResponseDTO.statusMessage().contains(exception.getMessage()));
  }

  @SneakyThrows
  @Test
  void findByPatientWhenNotEntitled() {
    var cause = new HTTPException(403, NOT_ENTITLED, new URL("http://localhost"));
    var exception = new WebServiceException(NOT_ENTITLED, cause);
    Mockito.when(
            documentServiceMock.documentRegistryRegistryStoredQuery(
                Mockito.any(AdhocQueryRequest.class)))
        .thenThrow(exception);
    var findDocumentRequest = ResourceLoader.findByPatientIdRequest();
    var actualResponseDTO =
        assertDoesNotThrow(() -> xdsDocumentApi.find(X_INSURANT_ID, findDocumentRequest));
    assertNotNull(actualResponseDTO);
    assertFalse(actualResponseDTO.success());
    assertTrue(actualResponseDTO.statusMessage().contains(exception.getMessage()));
  }

  @Test
  void findByPatientWithUnexpectedCause() {
    var cause = new IllegalArgumentException("I am an unexpected exception");
    var exception = new WebServiceException(NOT_ENTITLED, cause);
    Mockito.when(
            documentServiceMock.documentRegistryRegistryStoredQuery(
                Mockito.any(AdhocQueryRequest.class)))
        .thenThrow(exception);
    var findDocumentRequest = ResourceLoader.findByPatientIdRequest();
    var actualResponseDTO =
        assertThrows(
            WebServiceException.class,
            () -> xdsDocumentApi.find(X_INSURANT_ID, findDocumentRequest));
    assertNotNull(actualResponseDTO);
  }

  @Test
  void findByCommentRequestTest() {
    Mockito.when(
            documentServiceMock.documentRegistryRegistryStoredQuery(
                Mockito.any(AdhocQueryRequest.class)))
        .thenReturn(TestDataFactory.getSuccessResponse());
    FindRequestDTO findRequestDTO = ResourceLoader.findByCommentRequest();
    FindObjectsResponseDTO actualResponseDTO = xdsDocumentApi.find(X_INSURANT_ID, findRequestDTO);
    final RegistryObjectLists registryObjectLists = new RegistryObjectLists(null, null, null, null);
    FindObjectsResponseDTO expectedResponseDTO =
        new FindObjectsResponseDTO(
            true, TestDataFactory.REGISTRY_RESPONSE_STATUS_SUCCESS, registryObjectLists);
    assertEquals(expectedResponseDTO, actualResponseDTO);
  }

  @Test
  void findByReferenceIdRequestTest() {
    Mockito.when(
            documentServiceMock.documentRegistryRegistryStoredQuery(
                Mockito.any(AdhocQueryRequest.class)))
        .thenReturn(TestDataFactory.getSuccessResponse());
    FindRequestDTO findRequestDTO = ResourceLoader.findByReferenceIdRequest();
    FindObjectsResponseDTO actualResponseDTO = xdsDocumentApi.find(X_INSURANT_ID, findRequestDTO);
    final RegistryObjectLists registryObjectLists = new RegistryObjectLists(null, null, null, null);
    FindObjectsResponseDTO expectedResponseDTO =
        new FindObjectsResponseDTO(
            true, TestDataFactory.REGISTRY_RESPONSE_STATUS_SUCCESS, registryObjectLists);
    assertEquals(expectedResponseDTO, actualResponseDTO);
  }

  @Test
  void putDocumentsTest() {
    Mockito.when(
            documentServiceMock.documentRepositoryProvideAndRegisterDocumentSetB(
                Mockito.any(ProvideAndRegisterDocumentSetRequestType.class)))
        .thenReturn(TestDataFactory.registryResponseSuccess());

    PutDocumentsRequestDTO request = ResourceLoader.putDocumentWithFolderMetadataRequest();

    ResponseDTO expectedResponseDTO =
        new ResponseDTO(true, TestDataFactory.REGISTRY_RESPONSE_STATUS_SUCCESS);

    ResponseDTO actualResponseDTO =
        assertDoesNotThrow(() -> xdsDocumentApi.putDocuments(X_INSURANT_ID, request));
    assertEquals(expectedResponseDTO, actualResponseDTO);
  }

  @Test
  void putDocumentsExceptionTest() {
    var exception = new RuntimeException("I am an expected exception");
    Mockito.when(
            documentServiceMock.documentRepositoryProvideAndRegisterDocumentSetB(
                Mockito.any(ProvideAndRegisterDocumentSetRequestType.class)))
        .thenThrow(exception);
    var putDocumentRequest = ResourceLoader.putDocumentWithFolderMetadataRequest();
    var actualResponseDTO =
        assertDoesNotThrow(() -> xdsDocumentApi.putDocuments(X_INSURANT_ID, putDocumentRequest));
    assertNotNull(actualResponseDTO);
    assertFalse(actualResponseDTO.success());
    assertTrue(actualResponseDTO.statusMessage().contains(exception.getMessage()));
  }

  @Test
  void replaceDocumentsTest() {
    Mockito.when(
            documentServiceMock.documentRepositoryProvideAndRegisterDocumentSetB(
                Mockito.any(ProvideAndRegisterDocumentSetRequestType.class)))
        .thenReturn(TestDataFactory.registryResponseSuccess());

    var replaceDocumentsRequest = ResourceLoader.replaceDocumentsRequest();

    ResponseDTO expectedResponseDTO =
        new ResponseDTO(true, TestDataFactory.REGISTRY_RESPONSE_STATUS_SUCCESS);

    ResponseDTO actualResponseDTO =
        assertDoesNotThrow(
            () -> xdsDocumentApi.replaceDocuments(X_INSURANT_ID, replaceDocumentsRequest));

    assertNotNull(actualResponseDTO);
    assertEquals(expectedResponseDTO, actualResponseDTO);
  }

  @Test
  void replaceDocumentsExceptionTest() {
    var exception = new RuntimeException("I am the expected exception");
    Mockito.when(
            documentServiceMock.documentRepositoryProvideAndRegisterDocumentSetB(
                Mockito.any(ProvideAndRegisterDocumentSetRequestType.class)))
        .thenThrow(exception);

    var replaceDocumentsRequest = ResourceLoader.replaceDocumentsRequest();

    var actualResponseDTO =
        assertDoesNotThrow(
            () -> xdsDocumentApi.replaceDocuments(X_INSURANT_ID, replaceDocumentsRequest));

    assertNotNull(actualResponseDTO);
    assertFalse(actualResponseDTO.success());
    assertTrue(actualResponseDTO.statusMessage().contains(exception.getMessage()));
  }

  @Test
  void getDocumentsTest() {
    var iheResponse = TestDataFactory.retrieveDocumentSetResponse();
    Mockito.when(
            documentServiceMock.documentRepositoryRetrieveDocumentSet(
                Mockito.any(RetrieveDocumentSetRequestType.class)))
        .thenReturn(iheResponse);

    var request = ResourceLoader.retrieveDocumentsRequest();

    var response = assertDoesNotThrow(() -> xdsDocumentApi.getDocuments(X_INSURANT_ID, request));

    assertNotNull(response);

    assertArrayEquals(
        iheResponse.getDocumentResponse().stream()
            .map(RetrieveDocumentSetResponseType.DocumentResponse::getDocumentUniqueId)
            .toArray(),
        response.documents().stream().map(RetrieveDocumentElement::documentUniqueId).toArray());
    assertEquals(
        iheResponse.getDocumentResponse().stream()
            .map(RetrieveDocumentSetResponseType.DocumentResponse::getRepositoryUniqueId)
            .findFirst()
            .orElse(null),
        response.documents().stream()
            .map(RetrieveDocumentElement::repositoryUniqueId)
            .findFirst()
            .orElse(null));
    assertArrayEquals(
        iheResponse.getDocumentResponse().stream()
            .map(RetrieveDocumentSetResponseType.DocumentResponse::getDocument)
            .map(ByteArray::of)
            .toArray(),
        response.documents().stream().map(RetrieveDocumentElement::document).toArray());
  }

  @Test
  void getDocumentsExceptionTest() {
    var exception = new RuntimeException("I am the expected exception");
    Mockito.when(
            documentServiceMock.documentRepositoryRetrieveDocumentSet(
                Mockito.any(RetrieveDocumentSetRequestType.class)))
        .thenThrow(exception);

    var retrieveDocumentsRequest = ResourceLoader.retrieveDocumentsRequest();

    var actualResponseDTO =
        assertDoesNotThrow(
            () -> xdsDocumentApi.getDocuments(X_INSURANT_ID, retrieveDocumentsRequest));

    assertNotNull(actualResponseDTO);
    assertFalse(actualResponseDTO.success());
    assertTrue(actualResponseDTO.statusMessage().contains(exception.getMessage()));
  }

  @SneakyThrows
  @Test
  void getDocumentsWhenNotEntitled() {
    var cause = new HTTPException(403, NOT_ENTITLED, new URL("http://localhost"));
    var exception = new WebServiceException(NOT_ENTITLED, cause);
    Mockito.when(
            documentServiceMock.documentRepositoryRetrieveDocumentSet(
                Mockito.any(RetrieveDocumentSetRequestType.class)))
        .thenThrow(exception);

    var retrieveDocumentsRequest = ResourceLoader.retrieveDocumentsRequest();

    var actualResponseDTO =
        assertDoesNotThrow(
            () -> xdsDocumentApi.getDocuments(X_INSURANT_ID, retrieveDocumentsRequest));

    assertNotNull(actualResponseDTO);
    assertFalse(actualResponseDTO.success());
    assertTrue(actualResponseDTO.statusMessage().contains(exception.getMessage()));
  }

  @Test
  void getDocumentsWithUnexpectedCause() {
    var cause = new IllegalArgumentException("I am an unexpected exception");
    var exception = new WebServiceException(NOT_ENTITLED, cause);
    Mockito.when(
            documentServiceMock.documentRepositoryRetrieveDocumentSet(
                Mockito.any(RetrieveDocumentSetRequestType.class)))
        .thenThrow(exception);

    var retrieveDocumentsRequest = ResourceLoader.retrieveDocumentsRequest();

    var actualResponseDTO =
        assertThrows(
            WebServiceException.class,
            () -> xdsDocumentApi.getDocuments(X_INSURANT_ID, retrieveDocumentsRequest));

    assertNotNull(actualResponseDTO);
  }

  @Test
  void deleteObjectsTest() {
    Mockito.when(
            documentServiceMock.documentRegistryDeleteDocumentSet(
                Mockito.any(RemoveObjectsRequest.class)))
        .thenReturn(TestDataFactory.registryResponseSuccess());

    var request = new DeleteObjectsRequestDTO(TestDataFactory.KVNR, List.of("id1", "id2"));

    var response = assertDoesNotThrow(() -> xdsDocumentApi.deleteObjects(X_INSURANT_ID, request));

    assertNotNull(response);
    assertTrue(response.success());
    assertEquals(TestDataFactory.REGISTRY_RESPONSE_STATUS_SUCCESS, response.statusMessage());
  }

  @Test
  void deleteDocumentsExceptionTest() {
    var exception = new RuntimeException("I am the expected exception");
    Mockito.when(
            documentServiceMock.documentRegistryDeleteDocumentSet(
                Mockito.any(RemoveObjectsRequest.class)))
        .thenThrow(exception);

    var deleteObjectsRequest =
        new DeleteObjectsRequestDTO(TestDataFactory.KVNR, List.of("id1", "id2"));

    var actualResponseDTO =
        assertDoesNotThrow(() -> xdsDocumentApi.deleteObjects(X_INSURANT_ID, deleteObjectsRequest));

    assertNotNull(actualResponseDTO);
    assertFalse(actualResponseDTO.success());
    assertTrue(actualResponseDTO.statusMessage().contains(exception.getMessage()));
  }

  @Test
  void updateDocumentsTest() {
    Mockito.when(
            documentServiceMock.updateResponderRestrictedUpdateDocumentSet(
                Mockito.any(SubmitObjectsRequest.class)))
        .thenReturn(TestDataFactory.registryResponseSuccess());

    UpdateDocumentsRequestDTO request = ResourceLoader.updateDocumentsRequest();

    ResponseDTO expectedResponseDTO =
        new ResponseDTO(true, TestDataFactory.REGISTRY_RESPONSE_STATUS_SUCCESS);

    ResponseDTO actualResponseDTO =
        assertDoesNotThrow(() -> xdsDocumentApi.updateDocuments(X_INSURANT_ID, request));
    assertEquals(expectedResponseDTO, actualResponseDTO);
  }

  @Test
  void updateDocumentsExceptionTest() {
    var exception = new RuntimeException("I am an expected exception");
    Mockito.when(
            documentServiceMock.updateResponderRestrictedUpdateDocumentSet(
                Mockito.any(SubmitObjectsRequest.class)))
        .thenThrow(exception);
    UpdateDocumentsRequestDTO request = ResourceLoader.updateDocumentsRequest();
    var actualResponseDTO =
        assertDoesNotThrow(() -> xdsDocumentApi.updateDocuments(X_INSURANT_ID, request));
    assertNotNull(actualResponseDTO);
    assertFalse(actualResponseDTO.success());
    assertTrue(actualResponseDTO.statusMessage().contains(exception.getMessage()));
  }
}
