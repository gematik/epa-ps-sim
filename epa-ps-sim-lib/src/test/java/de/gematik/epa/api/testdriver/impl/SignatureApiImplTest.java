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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.epa.unit.util.ResourceLoader;
import de.gematik.epa.unit.util.TestBase;
import de.gematik.epa.unit.util.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import telematik.ws.conn.eventservice.wsdl.v6_1.FaultMessage;
import telematik.ws.conn.signatureservice.xsd.v7_5.GetJobNumberResponse;

class SignatureApiImplTest extends TestBase {

  private final SignatureApiImpl documentApi =
      new SignatureApiImpl(konnektorContextProvider(), konnektorInterfaceAssembly());

  @Test
  void signDocumentTest() {
    Mockito.when(konnektorInterfaceAssembly().eventService().getCards(Mockito.any()))
        .thenReturn(TestDataFactory.getCardsSmbResponse());
    Mockito.when(konnektorInterfaceAssembly().signatureService().getJobNumber(Mockito.any()))
        .thenReturn(new GetJobNumberResponse().withJobNumber("Job001"));
    Mockito.when(konnektorInterfaceAssembly().signatureService().signDocument(Mockito.any()))
        .thenReturn(TestDataFactory.getSignDocumentResponse());

    final var signRequest = ResourceLoader.signDocumentRequest();

    final var response = assertDoesNotThrow(() -> documentApi.signDocument(signRequest));

    assertNotNull(response);
    assertTrue(response.success());
    assertNotNull(response.signatureObject());
    assertNotNull(response.signatureForm());
  }

  @Test
  void signDocumentExceptionTest() {
    final var exceptionMsg = "No card terminal active";
    Mockito.when(konnektorInterfaceAssembly().eventService().getCards(Mockito.any()))
        .thenThrow(new FaultMessage(exceptionMsg, TestDataFactory.getTelematikError()));

    final var signRequest = ResourceLoader.signDocumentRequest();

    final var response = assertDoesNotThrow(() -> documentApi.signDocument(signRequest));

    assertNotNull(response);
    assertFalse(response.success());
    assertNull(response.signatureObject());
    assertNull(response.signatureForm());
    assertNotNull(response.statusMessage());
    assertTrue(response.statusMessage().contains(exceptionMsg));
  }
}
