/*-
 * #%L
 * epa-ps-sim-api
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
package de.gematik.epa.api.testdriver.dto.request;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SignDocumentRequestTest {

  @Test
  void signatureTypeOfUriTest() {
    var signatureType =
        assertDoesNotThrow(
            () ->
                SignDocumentRequest.SignatureType.of(SignDocumentRequest.SignatureType.CMS.uri()));

    assertEquals(SignDocumentRequest.SignatureType.CMS, signatureType);
  }

  @Test
  void signatureTypeOfUnkownUriTest() {
    var uri = URI.create("http://wrong/uri");
    var exception =
        assertThrows(
            IllegalArgumentException.class, () -> SignDocumentRequest.SignatureType.of(uri));

    assertTrue(exception.getMessage().contains(uri.toString()));
  }

  @Test
  void signatureTypeOfNameTest() {
    var signatureType =
        assertDoesNotThrow(
            () ->
                SignDocumentRequest.SignatureType.of(SignDocumentRequest.SignatureType.CMS.name()));

    assertEquals(SignDocumentRequest.SignatureType.CMS, signatureType);
  }

  @Test
  void signatureTypeOfUnkownNameTest() {
    var name = "DOC";
    var exception =
        assertThrows(
            IllegalArgumentException.class, () -> SignDocumentRequest.SignatureType.of(name));

    assertTrue(exception.getMessage().contains(name));
  }

  @Test
  void signDocumentRequestTest() {
    var signDocumentRequest =
        assertDoesNotThrow(
            () ->
                new SignDocumentRequest(
                    "the document".getBytes(StandardCharsets.UTF_8),
                    true,
                    SignDocumentRequest.SignatureAlgorithm.ECC));
  }
}
