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
package de.gematik.epa.konnektor.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.gematik.epa.unit.util.TestBase;
import oasis.names.tc.dss._1_0.core.schema.Base64Signature;
import oasis.names.tc.dss._1_0.core.schema.SignatureObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import telematik.ws.conn.authsignatureservice.wsdl.v7_4.AuthSignatureServicePortType;
import telematik.ws.conn.connectorcommon.xsd.v5_0.Status;
import telematik.ws.conn.signatureservice.xsd.v7_4.ExternalAuthenticateResponse;
import telematik.ws.tel.error.telematikerror.xsd.v2_0.Error;

class AuthSignatureServiceClientTest extends TestBase {

  private AuthSignatureServicePortType authSignatureService;

  private AuthSignatureServiceClient authSignatureServiceClient;

  @BeforeEach
  void setUp() {
    authSignatureServiceClient =
        new AuthSignatureServiceClient(konnektorContextProvider(), konnektorInterfaceAssembly());
    authSignatureService = konnektorInterfaceAssembly().authSignatureService();
  }

  @Test
  void shouldFailForErrorResponse() {
    var cardHandle = "cardHandle";
    var dataToSign = new byte[0];
    var response =
        new ExternalAuthenticateResponse()
            .withSignatureObject(new SignatureObject())
            .withStatus(new Status().withError(new Error()));
    when(authSignatureService.externalAuthenticate(any())).thenReturn(response);

    var result = authSignatureServiceClient.externalAuthenticate(cardHandle, dataToSign);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldFailWhenSignatureMisses() {
    var cardHandle = "cardHandle";
    var dataToSign = new byte[0];
    var response =
        new ExternalAuthenticateResponse()
            .withSignatureObject(new SignatureObject())
            .withStatus(new Status().withResult("OK"));
    when(authSignatureService.externalAuthenticate(any())).thenReturn(response);

    var result = authSignatureServiceClient.externalAuthenticate(cardHandle, dataToSign);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldExternalAuthenticate() {
    var cardHandle = "cardHandle";
    var dataToSign = new byte[0];
    var response =
        new ExternalAuthenticateResponse()
            .withSignatureObject(
                new SignatureObject()
                    .withBase64Signature(
                        new Base64Signature()
                            .withValue("hello".getBytes())
                            .withType(AuthSignatureServiceClient.SIGNATURE_TYPE_ECDSA)))
            .withStatus(new Status().withResult("OK"));
    when(authSignatureService.externalAuthenticate(any())).thenReturn(response);

    var result = authSignatureServiceClient.externalAuthenticate(cardHandle, dataToSign);

    assertThat(result).isPresent();
    assertThat(result.get().getStatus()).isEqualTo(response.getStatus());
  }
}
