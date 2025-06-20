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
package de.gematik.epa.konnektor;

import static de.gematik.epa.unit.util.TestDataFactory.CARD_HANDLE;
import static de.gematik.epa.unit.util.TestDataFactory.SMB_AUT_TELEMATIK_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jose4j.jws.AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.epa.authentication.exception.TelematikIdNotFoundException;
import de.gematik.epa.konnektor.client.AuthSignatureServiceClient;
import de.gematik.epa.unit.util.ResourceLoader;
import de.gematik.epa.unit.util.TestDataFactory;
import de.gematik.epa.utils.CertificateUtils;
import de.gematik.idp.field.ClaimName;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.UnaryOperator;
import oasis.names.tc.dss._1_0.core.schema.Base64Signature;
import oasis.names.tc.dss._1_0.core.schema.SignatureObject;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.Test;
import telematik.ws.conn.signatureservice.xsd.v7_4.ExternalAuthenticateResponse;

class CardAuthenticationServiceTest {

  private final SmbInformationProvider smbInformationProvider = mock(SmbInformationProvider.class);
  private final AuthSignatureServiceClient authSignatureServiceClient =
      mock(AuthSignatureServiceClient.class);

  private final CardAuthenticationService cardAuthenticationService =
      new CardAuthenticationService(smbInformationProvider, authSignatureServiceClient);

  @Test
  void shouldReturnCardHandle() {
    when(smbInformationProvider.getSmbInformationForTelematikId(SMB_AUT_TELEMATIK_ID))
        .thenReturn(Optional.of(TestDataFactory.createSmbInformation(SMB_AUT_TELEMATIK_ID)));

    assertThat(cardAuthenticationService.getCardHandle(SMB_AUT_TELEMATIK_ID))
        .isEqualTo(CARD_HANDLE);
  }

  @Test
  void shouldThrowExceptionWhenTelematikIdDoesNotExist() {
    when(smbInformationProvider.getSmbInformationForTelematikId(SMB_AUT_TELEMATIK_ID))
        .thenReturn(Optional.empty());

    assertThrows(
        TelematikIdNotFoundException.class,
        () -> cardAuthenticationService.getCardHandle(SMB_AUT_TELEMATIK_ID));
  }

  @Test
  void shouldReturnSignature() {
    byte[] dataToSign = "testData".getBytes();
    Base64Signature signature = new Base64Signature();
    when(authSignatureServiceClient.externalAuthenticate(CARD_HANDLE, dataToSign))
        .thenReturn(
            Optional.of(
                new ExternalAuthenticateResponse()
                    .withSignatureObject(new SignatureObject().withBase64Signature(signature))));

    assertThat(cardAuthenticationService.externalAuthenticate(CARD_HANDLE, dataToSign))
        .isEqualTo(signature);
  }

  @Test
  void shouldReturnContentSigner() {
    when(authSignatureServiceClient.externalAuthenticate(CARD_HANDLE, new byte[0]))
        .thenReturn(
            Optional.of(
                new ExternalAuthenticateResponse()
                    .withSignatureObject(
                        new SignatureObject().withBase64Signature(new Base64Signature()))));

    final UnaryOperator<byte[]> resultSigner =
        cardAuthenticationService.getContentSigner(CARD_HANDLE);

    assertThat(resultSigner).isNotNull();
  }

  @Test
  void shouldCreateSignedJwt() {
    final JwtClaims claims = new JwtClaims();
    final ZonedDateTime now = ZonedDateTime.now();
    claims.setClaim(ClaimName.ISSUED_AT.getJoseName(), now.toEpochSecond());
    claims.setClaim(ClaimName.EXPIRES_AT.getJoseName(), now.plusMinutes(20).toEpochSecond());
    int auditValue = 2;
    claims.setClaim("auditEvidence", auditValue);

    final X509Certificate certificate =
        CertificateUtils.toX509Certificate(ResourceLoader.autCertificateAsByteArray());

    final UnaryOperator<byte[]> contentSigner = TestDataFactory.getContentSigner(CARD_HANDLE);

    when(authSignatureServiceClient.externalAuthenticate(CARD_HANDLE, new byte[0]))
        .thenReturn(
            Optional.of(
                new ExternalAuthenticateResponse()
                    .withSignatureObject(
                        new SignatureObject().withBase64Signature(new Base64Signature()))));

    var resultJwt =
        cardAuthenticationService.createSignedJwt(
            claims, certificate, contentSigner, ECDSA_USING_P256_CURVE_AND_SHA256);
    assertThat(resultJwt).isNotBlank().contains("eyJ");
  }

  @Test
  void shouldDetermineKeyAlgorithm() {
    var algo =
        cardAuthenticationService.determineAlgorithm(
            CertificateUtils.toX509Certificate(ResourceLoader.autCertificateAsByteArray())
                .getPublicKey());

    assertThat(algo).isEqualTo(ECDSA_USING_P256_CURVE_AND_SHA256);
  }
}
