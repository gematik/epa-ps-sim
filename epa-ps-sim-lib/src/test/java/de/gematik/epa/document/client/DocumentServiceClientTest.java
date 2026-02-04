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
package de.gematik.epa.document.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.gematik.epa.konnektor.SmbInformationProvider;
import de.gematik.epa.unit.util.TestBase;
import de.gematik.epa.unit.util.TestDataFactory;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import oasis.names.tc.ebxml_regrep.xsd.lcm._3.RemoveObjectsRequest;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DocumentServiceClientTest extends TestBase {

  private DocumentServiceClient tstObj;

  @BeforeEach
  void initialize() {
    tstObj =
        new DocumentServiceClient(
            documentInterfaceAssembly(),
            defaultdataProvider(),
            new SmbInformationProvider(konnektorContextProvider(), konnektorInterfaceAssembly()));
  }

  @Test
  void documentRegistryRegistryStoredQueryTest() {
    when(tstObj.documentService().documentRegistryRegistryStoredQuery(any()))
        .thenReturn(TestDataFactory.getSuccessResponse());

    var testData = new AdhocQueryRequest();

    var result = assertDoesNotThrow(() -> tstObj.documentRegistryRegistryStoredQuery(testData));

    assertNotNull(result);
    assertEquals(TestDataFactory.REGISTRY_RESPONSE_STATUS_SUCCESS, result.getStatus());
  }

  @Test
  void documentRepositoryProvideAndRegisterDocumentSetBTest() {
    when(tstObj.documentService().documentRepositoryProvideAndRegisterDocumentSetB(any()))
        .thenReturn(TestDataFactory.registryResponseSuccess());

    var testData = new ProvideAndRegisterDocumentSetRequestType();

    var result =
        assertDoesNotThrow(() -> tstObj.documentRepositoryProvideAndRegisterDocumentSetB(testData));

    assertNotNull(result);
    assertEquals(TestDataFactory.REGISTRY_RESPONSE_STATUS_SUCCESS, result.getStatus());
  }

  @Test
  void documentRegistryDeleteDocumentSetTest() {
    Mockito.when(tstObj.documentService().documentRegistryDeleteDocumentSet(Mockito.any()))
        .thenReturn(TestDataFactory.registryResponseSuccess());

    var testData = new RemoveObjectsRequest();

    var result = assertDoesNotThrow(() -> tstObj.documentRegistryDeleteDocumentSet(testData));

    assertNotNull(result);
    assertEquals(TestDataFactory.REGISTRY_RESPONSE_STATUS_SUCCESS, result.getStatus());
  }
}
