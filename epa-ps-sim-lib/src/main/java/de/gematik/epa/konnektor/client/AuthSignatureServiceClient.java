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
package de.gematik.epa.konnektor.client;

import de.gematik.epa.konnektor.KonnektorContextProvider;
import de.gematik.epa.konnektor.KonnektorInterfaceAssembly;
import java.util.Optional;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import oasis.names.tc.dss._1_0.core.schema.Base64Data;
import org.apache.commons.codec.digest.DigestUtils;
import telematik.ws.conn.authsignatureservice.wsdl.v7_4.AuthSignatureServicePortType;
import telematik.ws.conn.connectorcontext.xsd.v2_0.ContextType;
import telematik.ws.conn.signatureservice.xsd.v7_4.BinaryDocumentType;
import telematik.ws.conn.signatureservice.xsd.v7_4.ExternalAuthenticate;
import telematik.ws.conn.signatureservice.xsd.v7_4.ExternalAuthenticateResponse;

@Accessors(fluent = true)
@Slf4j
public class AuthSignatureServiceClient extends KonnektorServiceClient {

  public static final String SIGNATURE_TYPE_ECDSA = "urn:bsi:tr:03111:ecdsa";

  private ContextType contextType;
  private AuthSignatureServicePortType authSignatureService;

  public AuthSignatureServiceClient(
      KonnektorContextProvider konnektorContextProvider,
      KonnektorInterfaceAssembly konnektorInterfaceAssembly) {
    super(konnektorContextProvider, konnektorInterfaceAssembly);
    runInitializationSynchronized();
  }

  @Override
  protected void initialize() {
    contextType = konnektorContextProvider.getContext();
    authSignatureService = konnektorInterfaceAssembly.authSignatureService();
  }

  public Optional<ExternalAuthenticateResponse> externalAuthenticate(
      final String cardHandle, final byte[] dataToSign) {
    ExternalAuthenticate externalAuthenticate = new ExternalAuthenticate();
    externalAuthenticate
        .withContext(contextType)
        .withCardHandle(cardHandle)
        .withOptionalInputs(
            new ExternalAuthenticate.OptionalInputs().withSignatureType(SIGNATURE_TYPE_ECDSA));
    externalAuthenticate.setBinaryString(
        new BinaryDocumentType()
            .withBase64Data(
                new Base64Data()
                    .withMimeType("application/octet-stream")
                    .withValue(DigestUtils.sha256(dataToSign))));

    ExternalAuthenticateResponse externalAuthenticateResponse =
        authSignatureService.externalAuthenticate(externalAuthenticate);
    if (externalAuthenticateResponse.getStatus().getError() != null
        || !externalAuthenticateResponse.getStatus().getResult().equals("OK")
        || externalAuthenticateResponse.getSignatureObject().getBase64Signature() == null
        || !externalAuthenticateResponse
            .getSignatureObject()
            .getBase64Signature()
            .getType()
            .equals(SIGNATURE_TYPE_ECDSA)) {

      log.warn(
          "externalAuthenticate failed: {}", externalAuthenticateResponse.getStatus().getError());
      return Optional.empty();
    }
    return Optional.of(externalAuthenticateResponse);
  }
}
