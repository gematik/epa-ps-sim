/*-
 * #%L
 * epa-ps-sim-app
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
package de.gematik.epa.unit;

import de.gematik.epa.konnektor.client.AuthSignatureServiceClient;
import de.gematik.epa.ps.document.config.DocumentConfigurationData;
import de.gematik.epa.ps.document.config.DocumentConnectionConfigurationData;
import de.gematik.epa.ps.konnektor.config.KonnektorConfigurationData;
import de.gematik.epa.ps.konnektor.config.KonnektorConnectionConfigurationData;
import de.gematik.epa.unit.util.TestDataFactory;
import de.gematik.epa.utils.HealthRecordProvider;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import oasis.names.tc.dss._1_0.core.schema.Base64Signature;
import oasis.names.tc.dss._1_0.core.schema.SignatureObject;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse;
import org.apache.commons.io.FileUtils;
import telematik.ws.conn.certificateservice.xsd.v6_0.ReadCardCertificateResponse;
import telematik.ws.conn.certificateservicecommon.xsd.v2_0.CertRefEnum;
import telematik.ws.conn.certificateservicecommon.xsd.v2_0.X509DataInfoListType;
import telematik.ws.conn.signatureservice.xsd.v7_4.ExternalAuthenticateResponse;
import telematik.ws.conn.vsds.vsdservice.xsd.v5_2.ReadVSDResponse;
import telematik.ws.fa.vsds.pruefungsnachweis.xsd.v1_0.PN;

@Slf4j
@UtilityClass
public class AppTestDataFactory extends TestDataFactory {

  public static final String HMAC_KEY =
      "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20";
  private final VsdmFdHelper vsdmFd =
      new VsdmFdHelper("X", TestUtils.hexStringToByteArray(HMAC_KEY), 2);

  public static KonnektorConfigurationData createKonnektorConfiguration() {
    return new KonnektorConfigurationData()
        .connection(createKonnektorConnectionConfiguration())
        .context(createKonnektorContext());
  }

  public static DocumentConfigurationData createDocumentConfiguration() {
    return new DocumentConfigurationData().connection(createDocumentConnectionConfiguration());
  }

  public static DocumentConnectionConfigurationData createDocumentConnectionConfiguration() {
    return new DocumentConnectionConfigurationData().address(createAddress());
  }

  public static KonnektorConnectionConfigurationData createKonnektorConnectionConfiguration() {
    return new KonnektorConnectionConfigurationData()
        .address(createAddress())
        .tlsConfig(createTlsConfig())
        .proxyAddress(createProxyAddressConfig())
        .basicAuthentication(createBasicAuthenticationData());
  }

  public static AdhocQueryResponse getAdhocQueryResponse() {
    var result = new AdhocQueryResponse();
    result.setStatus("urn:oasis:names:tc:ebxml-regrep:ResponseStatusType:Success");
    return result;
  }

  public static ExternalAuthenticateResponse getSignNonceResponse() {
    return new ExternalAuthenticateResponse()
        .withStatus(getStatusOk())
        .withSignatureObject(
            new SignatureObject()
                .withBase64Signature(
                    new Base64Signature()
                        .withType(AuthSignatureServiceClient.SIGNATURE_TYPE_ECDSA)
                        .withValue(
                            Base64.getEncoder()
                                .encode(
                                    "MEQCIDEP8zhC2lbumG0UsizWg5xtxGuQmJpxtbVtNWMDpWGkAiAeryczHhZTRKqs6+VyaokfpURmTvEUFCHy01iFYFtOtQ=="
                                        .getBytes()))));
  }

  public static ExternalAuthenticateResponse getSignChallengeResponse() {
    return new ExternalAuthenticateResponse()
        .withStatus(getStatusOk())
        .withSignatureObject(
            new SignatureObject()
                .withBase64Signature(
                    new Base64Signature()
                        .withType(AuthSignatureServiceClient.SIGNATURE_TYPE_ECDSA)
                        .withValue(
                            Base64.getDecoder()
                                .decode(
                                    "MEQCIEz3/1KiA71YhFr/Qe/Jcq2bJ6V0qbTDXsJWcl+TcGWeAiAVDPtXq1/nrrcbYwySaTVMsxsVFkLsyu1Qg3B20G0okw=="
                                        .getBytes()))));
  }

  @SneakyThrows
  public static ReadCardCertificateResponse getReadCardCertificateResponse() {
    var readCardCertificateResponse = new ReadCardCertificateResponse();

    readCardCertificateResponse.setStatus(getStatusOk());
    readCardCertificateResponse.setX509DataInfoList(new X509DataInfoListType());

    var x509DataInfo = new X509DataInfoListType.X509DataInfo();
    x509DataInfo.setCertRef(CertRefEnum.C_AUT);

    var x509Data = new X509DataInfoListType.X509DataInfo.X509Data();
    x509Data.setX509Certificate(
        FileUtils.readFileToByteArray(
            FileUtils.getFile(
                "src/test/resources/certs/80276883110000163969-C_SMCB_HCI_AUT_E256.crt")));
    x509Data.setX509SubjectName("Unfallkrankenhaus am SeeTEST-ONLY");
    x509Data.setX509IssuerSerial(new X509DataInfoListType.X509DataInfo.X509Data.X509IssuerSerial());

    x509DataInfo.setX509Data(x509Data);
    readCardCertificateResponse.getX509DataInfoList().getX509DataInfo().add(x509DataInfo);

    return readCardCertificateResponse;
  }

  public static ReadVSDResponse getReadVSDResponsePZ2() {
    return createReadVSDResponsePZ2(createPruefungsnachweisPZ2("ICCSN-1"));
  }

  public static ReadVSDResponse getReadVSDResponsePZ2RevokedEgk() {
    log.info("Test with revoked eGK");
    return createReadVSDResponsePZ2(createPruefungsnachweisPZ2("ICCSN-2"));
  }

  public static ReadVSDResponse createReadVSDResponsePZ2(String pn) {
    return createReadVSDResponse(pn);
  }

  private static String createPruefungsnachweisPZ2(String iccsn) {
    String pruefzifferV2 = vsdmFd.genPruefziffer(iccsn);
    log.info("Prüfziffer V2: {}", pruefzifferV2);

    PN pn = new PN();
    pn.setCDMVERSION("1.0.0");
    pn.setTS("20240523091105");
    pn.setE(BigInteger.valueOf(2));
    pn.setPZ(Base64.getDecoder().decode(pruefzifferV2));

    try {
      return compressAndEncodePN(pn);
    } catch (Exception e) {
      throw new RuntimeException("Error creating Pruefungsnachweis V2", e);
    }
  }

  private static String compressAndEncodePN(PN pn) throws Exception {
    JAXBContext jaxbContext = JAXBContext.newInstance(PN.class);
    Marshaller marshaller = jaxbContext.createMarshaller();
    ByteArrayOutputStream xmlOutputStream = new ByteArrayOutputStream();
    marshaller.marshal(pn, xmlOutputStream);
    byte[] xmlBytes = xmlOutputStream.toByteArray();

    ByteArrayOutputStream gzipOutputStream = new ByteArrayOutputStream();
    try (GZIPOutputStream gzip = new GZIPOutputStream(gzipOutputStream)) {
      gzip.write(xmlBytes);
    }
    byte[] compressedBytes = gzipOutputStream.toByteArray();

    return Base64.getEncoder().encodeToString(compressedBytes);
  }

  public static void setupFqdnProvider(final String insurantId) {
    HealthRecordProvider.addHealthRecord(insurantId, "http://localhost:8080");
  }

  public static void clearFqdnProvider(final String insurantId) {
    HealthRecordProvider.clearHealthRecord(insurantId);
  }
}
