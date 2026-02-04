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

import de.gematik.epa.api.testdriver.SignatureApi;
import de.gematik.epa.api.testdriver.dto.request.SignDocumentRequest;
import de.gematik.epa.api.testdriver.dto.response.ResponseDTO;
import de.gematik.epa.api.testdriver.dto.response.SignDocumentResponse;
import de.gematik.epa.ihe.model.simple.ByteArray;
import de.gematik.epa.konnektor.KonnektorContextProvider;
import de.gematik.epa.konnektor.KonnektorInterfaceAssembly;
import de.gematik.epa.konnektor.KonnektorUtils;
import de.gematik.epa.konnektor.client.SignatureServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/** Implementation of the operations of the {@link SignatureApi}.<br> */
@Accessors(fluent = true)
@RequiredArgsConstructor
@Slf4j
public class SignatureApiImpl implements SignatureApi {

  private final KonnektorContextProvider contextProvider;

  private final KonnektorInterfaceAssembly konnektorInterfaceAssembly;

  @Override
  public SignDocumentResponse signDocument(final SignDocumentRequest request) {
    log.info("Running operation signDocument");
    try (final var signatureServiceClient =
        new SignatureServiceClient(contextProvider, konnektorInterfaceAssembly)) {
      final var konRequest = signatureServiceClient.transformRequest(request);

      final var konResponse = signatureServiceClient.signDocument(konRequest);

      return signatureServiceClient.transformResponse(konResponse);
    } catch (final Exception e) {
      log.error("Operation signDocument failed with an exception", e);
      return toSignDocumentResponse(KonnektorUtils.fromThrowable(e));
    }
  }

  private SignDocumentResponse toSignDocumentResponse(final ResponseDTO responseDTO) {
    return new SignDocumentResponse(
        responseDTO.success(), responseDTO.statusMessage(), (ByteArray) null, null);
  }
}
