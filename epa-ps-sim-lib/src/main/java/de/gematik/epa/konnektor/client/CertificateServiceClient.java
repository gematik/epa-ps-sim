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
import de.gematik.epa.utils.CertificateUtils;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import telematik.ws.conn.cardservice.xsd.v8_1.CardInfoType;
import telematik.ws.conn.certificateservice.wsdl.v6_0.CertificateServicePortType;
import telematik.ws.conn.certificateservice.xsd.v6_0.CryptType;
import telematik.ws.conn.certificateservice.xsd.v6_0.ObjectFactory;
import telematik.ws.conn.certificateservice.xsd.v6_0.ReadCardCertificate;
import telematik.ws.conn.certificateservice.xsd.v6_0.ReadCardCertificateResponse;
import telematik.ws.conn.certificateservicecommon.xsd.v2_0.CertRefEnum;
import telematik.ws.conn.certificateservicecommon.xsd.v2_0.X509DataInfoListType;
import telematik.ws.conn.certificateservicecommon.xsd.v2_0.X509DataInfoListType.X509DataInfo;
import telematik.ws.conn.certificateservicecommon.xsd.v2_0.X509DataInfoListType.X509DataInfo.X509Data;
import telematik.ws.conn.connectorcontext.xsd.v2_0.ContextType;

@Accessors(fluent = true)
public class CertificateServiceClient extends KonnektorServiceClient {

  // OID-Festlegung für Institutionstypen für die SMC-B
  // https://gemspec.gematik.de/docs/gemSpec/gemSpec_OID/gemSpec_OID_V3.20.0/#3.5.1.3
  public static final String OID_PRAXIS_ARZT = "1.2.276.0.76.4.50";
  public static final String OID_ZAHNARZTPRAXIS = "1.2.276.0.76.4.51";
  public static final String OID_PRAXIS_PSYCHOTHERAPEUT = "1.2.276.0.76.4.52";
  public static final String OID_KRANKENHAUS = "1.2.276.0.76.4.53";
  public static final String OID_OEFFENTLICHE_APOTHEKE = "1.2.276.0.76.4.54";
  public static final String OID_KRANKENHAUSAPOTHEKE = "1.2.276.0.76.4.55";
  public static final String OID_BUNDESWEHRAPOTHEKE = "1.2.276.0.76.4.56";
  public static final String OID_INSTITUTION_OEGD = "1.2.276.0.76.4.255";
  public static final String OID_INSTITUTION_PFLEGE = "1.2.276.0.76.4.245";
  public static final String OID_INSTITUTION_GEBURTSHILFE = "1.2.276.0.76.4.246";
  public static final String OID_PRAXIS_PHYSIOTHERAPEUT = "1.2.276.0.76.4.247";
  public static final String OID_INSTITUTION_ARBEITSMEDIZIN = "1.2.276.0.76.4.256";
  public static final String OID_INSTITUTION_VORSORGE_REHA = "1.2.276.0.76.4.257";

  private CertificateServicePortType certificateService;

  private ContextType context;

  public CertificateServiceClient(
      KonnektorContextProvider konnektorContextProvider,
      KonnektorInterfaceAssembly konnektorInterfaceAssembly) {
    super(konnektorContextProvider, konnektorInterfaceAssembly);
    runInitializationSynchronized();
  }

  public ReadCardCertificateResponse readCardCertificate(@NonNull ReadCardCertificate request) {
    return certificateService.readCardCertificate(request);
  }

  @SneakyThrows
  public String getTelematikIdFromCard(@NonNull CardInfoType card) {
    X509Certificate cert = getX509Certificate(card, CryptType.ECC);

    return CertificateUtils.getTelematikIdFromCertificate(cert);
  }

  @SneakyThrows
  public List<String> getProfessionOidsFromCard(@NonNull CardInfoType card) {
    X509Certificate cert = getX509Certificate(card, CryptType.ECC);

    return CertificateUtils.getProfessionOidsFromCertificate(cert);
  }

  public X509Certificate getX509Certificate(CardInfoType card, CryptType cryptType) {
    final var cardCertRequest = buildReadCardCertificateRequest(card, CertRefEnum.C_AUT, cryptType);

    var cardCertResponse = readCardCertificate(cardCertRequest);

    var certificate =
        Optional.ofNullable(cardCertResponse)
            .map(ReadCardCertificateResponse::getX509DataInfoList)
            .map(X509DataInfoListType::getX509DataInfo)
            .stream()
            .flatMap(Collection::stream)
            .map(X509DataInfo::getX509Data)
            .map(X509Data::getX509Certificate)
            .findFirst()
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "No AUT certificate in readCardCertificate response for card: " + card));

    X509Certificate cert =
        Objects.requireNonNull(
            CertificateUtils.toX509Certificate(certificate),
            "AUT certificate data could not be decoded as X509 certificate");
    return cert;
  }

  @Override
  protected void initialize() {
    context = konnektorContextProvider.getContext();
    certificateService = konnektorInterfaceAssembly.certificateService();
  }

  private ReadCardCertificate buildReadCardCertificateRequest(
      CardInfoType cardInfo, CertRefEnum certRef, CryptType cryptType) {
    final var certRefList = new ObjectFactory().createReadCardCertificateCertRefList();
    certRefList.getCertRef().add(certRef);
    final var readCardCertificateRequest = new ObjectFactory().createReadCardCertificate();
    readCardCertificateRequest.setCardHandle(cardInfo.getCardHandle());
    readCardCertificateRequest.setCertRefList(certRefList);
    readCardCertificateRequest.setContext(context);
    readCardCertificateRequest.setCrypt(cryptType);
    return readCardCertificateRequest;
  }
}
