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

import static org.jose4j.jws.AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256;
import static org.jose4j.jws.AlgorithmIdentifiers.RSA_PSS_USING_SHA256;
import static org.jose4j.jws.EcdsaUsingShaAlgorithm.convertDerToConcatenated;

import de.gematik.epa.authentication.exception.ExternalAuthenticateException;
import de.gematik.epa.authentication.exception.TelematikIdNotFoundException;
import de.gematik.epa.data.SmbInformation;
import de.gematik.epa.konnektor.client.AuthSignatureServiceClient;
import de.gematik.idp.client.IdpClientRuntimeException;
import de.gematik.idp.token.JsonWebToken;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.function.UnaryOperator;
import oasis.names.tc.dss._1_0.core.schema.Base64Signature;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import telematik.ws.conn.cardservice.xsd.v8_1.CardInfoType;
import telematik.ws.conn.certificateservice.xsd.v6_0.CryptType;

public class CardAuthenticationService {
  private final SmbInformationProvider smbInformationProvider;
  private final AuthSignatureServiceClient authSignatureServiceClient;

  public CardAuthenticationService(
      SmbInformationProvider smbInformationProvider,
      AuthSignatureServiceClient authSignatureServiceClient) {
    this.smbInformationProvider = smbInformationProvider;
    this.authSignatureServiceClient = authSignatureServiceClient;
  }

  public String getCardHandle(String telematikId) {
    return smbInformationProvider
        .getSmbInformationForTelematikId(telematikId)
        .map(SmbInformation::cardHandle)
        .orElseThrow(
            () ->
                new TelematikIdNotFoundException(
                    "TelematikId " + telematikId + " was not found in connector slots"));
  }

  public UnaryOperator<byte[]> getContentSigner(String cardHandle) {
    return tbs -> externalAuthenticate(cardHandle, tbs).getValue();
  }

  public Base64Signature externalAuthenticate(String cardHandle, byte[] dataToSign) {
    return authSignatureServiceClient
        .externalAuthenticate(cardHandle, dataToSign)
        .map(response -> response.getSignatureObject().getBase64Signature())
        .orElseThrow(
            () ->
                new ExternalAuthenticateException(
                    "Error while signing with externalAuthenticate for cardHandle: " + cardHandle));
  }

  public String determineAlgorithm(final PublicKey signerKey) {
    if (signerKey instanceof ECPublicKey) {
      // based on A_24590 it has to be ES256 even if brainpoolP256r1 is used
      return ECDSA_USING_P256_CURVE_AND_SHA256;
    } else {
      return RSA_PSS_USING_SHA256;
    }
  }

  public X509Certificate getX509Certificate(CardInfoType cardInfo) {
    return smbInformationProvider
        .certificateServiceClient()
        .getX509Certificate(cardInfo, CryptType.ECC);
  }

  public String createSignedJwt(
      final JwtClaims claims,
      final X509Certificate certificate,
      final UnaryOperator<byte[]> contentSigner,
      final String algorithm) {
    final JsonWebSignature jsonWebSignature = new JsonWebSignature();
    jsonWebSignature.setHeader("typ", "JWT");
    jsonWebSignature.setCertificateChainHeaderValue(certificate);
    jsonWebSignature.setPayload(claims.toJson());
    jsonWebSignature.setAlgorithmHeaderValue(algorithm);

    final String signedJwt =
        jsonWebSignature.getHeaders().getEncodedHeader()
            + "."
            + jsonWebSignature.getEncodedPayload()
            + "."
            + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                    getSignatureBytes(
                        contentSigner,
                        jsonWebSignature,
                        sigData -> {
                          if (certificate.getPublicKey() instanceof RSAPublicKey) {
                            return sigData;
                          } else {
                            try {
                              return convertDerToConcatenated(sigData, 64);
                            } catch (final IOException e) {
                              throw new IdpClientRuntimeException(e);
                            }
                          }
                        }));

    return new JsonWebToken(signedJwt).getRawString();
  }

  private byte[] getSignatureBytes(
      final UnaryOperator<byte[]> contentSigner,
      final JsonWebSignature jsonWebSignature,
      final UnaryOperator<byte[]> signatureStripper) {
    return signatureStripper.apply(
        contentSigner.apply(
            (jsonWebSignature.getHeaders().getEncodedHeader()
                    + "."
                    + jsonWebSignature.getEncodedPayload())
                .getBytes(StandardCharsets.UTF_8)));
  }
}
