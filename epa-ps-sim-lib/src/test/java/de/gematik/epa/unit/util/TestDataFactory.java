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
package de.gematik.epa.unit.util;

import static org.apache.cxf.message.Message.REQUESTOR_ROLE;
import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import de.gematik.epa.api.testdriver.config.AddressConfig;
import de.gematik.epa.api.testdriver.config.BasicAuthenticationConfig;
import de.gematik.epa.api.testdriver.config.Context;
import de.gematik.epa.api.testdriver.config.FileInfo;
import de.gematik.epa.api.testdriver.config.ProxyAddressConfig;
import de.gematik.epa.api.testdriver.config.TlsConfig;
import de.gematik.epa.api.testdriver.dto.request.SignDocumentRequest;
import de.gematik.epa.config.DefaultdataInterface;
import de.gematik.epa.config.InsurantIdBuilder;
import de.gematik.epa.data.AuthorInstitutionConfiguration;
import de.gematik.epa.data.AuthorPerson;
import de.gematik.epa.data.HbaInformation;
import de.gematik.epa.data.SmbInformation;
import de.gematik.epa.data.SubmissionSetAuthorConfiguration;
import de.gematik.epa.document.DocumentInterfaceAssembly;
import de.gematik.epa.document.config.DocumentConnectionConfigurationMutable;
import de.gematik.epa.ihe.model.simple.AuthorInstitution;
import de.gematik.epa.konnektor.KonnektorConfigurationProvider;
import de.gematik.epa.konnektor.KonnektorContextProvider;
import de.gematik.epa.konnektor.KonnektorInterfaceAssembly;
import de.gematik.epa.konnektor.KonnektorUtils;
import de.gematik.epa.konnektor.config.KonnektorConfigurationMutable;
import de.gematik.epa.konnektor.config.KonnektorConnectionConfigurationMutable;
import de.gematik.epa.konnektor.cxf.KonnektorInterfacesCxfImpl;
import de.gematik.epa.utils.XmlUtils;
import ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import jakarta.ws.rs.core.Response;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.function.UnaryOperator;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import oasis.names.tc.dss._1_0.core.schema.Base64Data;
import oasis.names.tc.dss._1_0.core.schema.Base64Signature;
import oasis.names.tc.dss._1_0.core.schema.SignatureObject;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessageInfo.Type;
import org.apache.cxf.service.model.ServiceInfo;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import telematik.ws.conn.authsignatureservice.wsdl.v7_4.AuthSignatureServicePortType;
import telematik.ws.conn.cardservice.wsdl.v8_1.CardServicePortType;
import telematik.ws.conn.cardservice.xsd.v8_1.CardInfoType;
import telematik.ws.conn.cardservice.xsd.v8_1.CardInfoType.CardVersion;
import telematik.ws.conn.cardservice.xsd.v8_1.Cards;
import telematik.ws.conn.cardservice.xsd.v8_1.GetPinStatusResponse;
import telematik.ws.conn.cardservice.xsd.v8_1.PinStatusEnum;
import telematik.ws.conn.cardservicecommon.xsd.v2_0.CardTypeType;
import telematik.ws.conn.cardservicecommon.xsd.v2_0.PinResponseType;
import telematik.ws.conn.cardservicecommon.xsd.v2_0.PinResultEnum;
import telematik.ws.conn.certificateservice.wsdl.v6_0.CertificateServicePortType;
import telematik.ws.conn.certificateservice.xsd.v6_0.ReadCardCertificateResponse;
import telematik.ws.conn.certificateservicecommon.xsd.v2_0.CertRefEnum;
import telematik.ws.conn.certificateservicecommon.xsd.v2_0.X509DataInfoListType;
import telematik.ws.conn.certificateservicecommon.xsd.v2_0.X509DataInfoListType.X509DataInfo.X509Data.X509IssuerSerial;
import telematik.ws.conn.connectorcommon.xsd.v5_0.Status;
import telematik.ws.conn.connectorcontext.xsd.v2_0.ContextType;
import telematik.ws.conn.eventservice.wsdl.v6_1.EventServicePortType;
import telematik.ws.conn.eventservice.xsd.v6_1.GetCardsResponse;
import telematik.ws.conn.signatureservice.wsdl.v7_5.SignatureServicePortType;
import telematik.ws.conn.signatureservice.xsd.v7_5.DocumentType;
import telematik.ws.conn.signatureservice.xsd.v7_5.SignDocumentResponse;
import telematik.ws.conn.signatureservice.xsd.v7_5.SignResponse;
import telematik.ws.conn.signatureservice.xsd.v7_5.SignResponse.OptionalOutputs;
import telematik.ws.conn.vsds.vsdservice.wsdl.v5_2.VSDServicePortType;
import telematik.ws.conn.vsds.vsdservice.xsd.v5_2.ReadVSDResponse;
import telematik.ws.conn.vsds.vsdservice.xsd.v5_2.VSDStatusType;
import telematik.ws.tel.error.telematikerror.xsd.v2_0.Error;
import telematik.ws.tel.error.telematikerror.xsd.v2_0.ObjectFactory;
import xds.document.wsdl.IDocumentManagementPortType;

@Slf4j
public class TestDataFactory {

  public static final String MANDANT_ID = "Test_Mandant";
  public static final String CLIENTSYSTEM_ID = "Test_Clientsytem";
  public static final String WORKPLACE_ID = "Test_Workplace";
  public static final String USER_ID = "Test_User";
  public static final String KVNR = "X110435031";
  @Deprecated public static final String HOME_COMMUNITY_ID = "1.2.3.4.5.67";
  public static final String STATUS_RESULT_OK = KonnektorUtils.STATUS_OK;
  public static final String STATUS_RESULT_WARNING = KonnektorUtils.STATUS_WARNING;
  public static final String REGISTRY_RESPONSE_STATUS_SUCCESS =
      "urn:oasis:names:tc:ebxml-regrep:ResponseStatusType:Success";
  public static final String SMB_AUT_TELEMATIK_ID = "1-SMC-B-Testkarte--883110000163969";
  public static final String CARD_HANDLE = "cardHandle";
  public static final String USER_AGENT = "PSSIM123456789012345/1.2.4";
  public static final String X_INSURANTID = "x-insurantid";
  public static final String X_USERAGENT = "x-useragent";

  private static final String ALLGEMEINE_VERSICHERUNGSDATEN =
      "H4sIAAAAAAAAAM1S30/CMBD+V5a9s+tmZpgpJQjGEASM6DS+LHU7toXtZtaChr/ejhCzEYKvvrS57+77kV758LssrB3WKq9oYLsOsy2kuEpySgf2dLXs9ft+0HN9eyj4yzgaFUWKJeaEYcOJM6y3lKpEaqS3+YNl1EgN7EzrzxuAL+WYaanzjZMgrCXsVFI2B+x8x7Ot8WQehXdPq+lyMbANYtwF/xXWWLeqxkbF2VbvBb/FNCcSHvMYc70rDkeAzyplguhaYtqQO+UGiYyIcFngMxZcBxzO9rusQiIlWJsXQTE5YbR7fCFLFM+otHU/C3urkMMB4aOPGuOMDpP/MB5cyAen9dllvG+V1Puc1pXqFMans0yStRZ+S+QItTnRMY3Rjw781+WjYC6H5uZwaRJOraETDLrfCv7+y+IHk1/iLhwDAAA=";
  private static final String GESCHUETZTE_VERSICHERUNGSDATEN =
      "H4sIAAAAAAAAAIVP22rCQBD9lbDvZmIhoDJZKSoS8AINFfFFlmTMBpONZCdpydd3QwsqLfTlzMyZy5mD88+q9DpqbFGbSIz9QHhk0jorTB6JONmPJpNwOhqHYi7xfXFek011S9wzHYalVFPDZDLl4LjdeO6asZHQzLcZwIf1c6oUF1c/I7go6GxWDQBd6L8Ib7Hcng+rtyTe7yLhGKcu8dT2Spetya1lxa2VmHzHAOEnQ/hjiEq6ctE5L9woykniKzU9l+5HOUW4F3hS2qjH3jPhZH7fgv/Nyy+lvE/gTQEAAA==";
  private static final String PERSOENLICHE_VERSICHERTENDATEN =
      "H4sIAAAAAAAAAI1SwW7aQBD9lZXvYTEFEarxRkmIUqQAUaxCbmhjD7bT9Wy1u4aW322/IeeMU0Kh7aGXtd/MvHkzegMX32ojNuh8ZSmJ4k43EkiZzSsqkmiSzs/Ozwejs3gQCR805dpYwiT6jj66UPD5enXPVItkqqzERduGvy4gV/LzOL0T1+PpanHzkE7msyQadHqtAmuST6IyhK8fpdz6ToG1DtWXTo5yreXG53X7yA3XRwqO+roTRKvJWD3Gcbc/7I+GXZB/5uBtPFJwi0+NC56namoVjwZx3OsOQZ6EYWEd6RpVutUUnlFMf7hKE6GYVZk1jHWLxT0Gp1lrXw0znZVvf0v0Hk2w6wDyEGRpn5UGszKoZat4QEyskfyu8Trs1K37ua6oJR4FIWUpbkqXuWub80LWB4NV2OnSqN6HYb8P8iQGcxdUmpUvtNUmR6FrcdVQ4Z/QFSDbJNyxkQqWtiTPJKPZLXTsOaoxyH+FQf6i7KdRD03ptQH5juGTbjw1dc3+9EAeIbgk3tdVazZkv9MNn5amQvDgfycPPX9vLN89lKd3IP/j+tQr5eQJHuACAAA=";
  private static final String PRUEFUNGSNACHWEIS =
      "H4sIAAAAAAAAAB2N0U7CMABFf2Xpq2HtVkaY6UoMqwHjCro6nC+m2gIT1hK6jMnXW3y5Dzc555DZ0B6DXp9dY00GohCBQJtvqxqzy8CyXI2m0yQdRSBwnTRKHq3RGfjVDswoWfNgnhefFXstlyv+T994bzQuA/uuO91DeHHhTreyaw6h0nArYe9UC0/mAvtbjRJR0hjFY5TEGKVRhBIC/UUYjQlkPvJBNzkbivzlysXDtRAMF6JGXLwh/lNVm/zx8PX+/FTjAQ+ThVyoczq3PdzbCRpvMU/u6oxAL/HD6R8G598c7wAAAA==";
  private static final String pnWithoutUpdate =
      "eNodjdFOwjAARX9l6ath7VZGmOlKDKsB4wq6OpwvptoCE9YSuozJ11t8uQ83OeeQ2dAeg16fXWNNBqIQgUCbb6sas8vAslyNptMkHUUgcJ00Sh6t0Rn41Q7MKFnzYJ4XnxV7LZcr/k/feG80LgP7rjvdQ3hx4U63smsOodJwK2HvVAtP5gL7W40SUdIYxWOUxBilUYQSAv1FGMUEMh/5oJucDUX+cuXi4VoIhgtRIy7eEP+pqk3+ePh6f36q8YCHyUIu1Dmd2x7u7QSNt5gnd3VGoJf44fQP9B9HXw==";
  private static final String PERSOENLICHE_VERSICHERTENDATEN_STRASSE_EMPTY_STR =
      "H4sIAPACnWcA/41STW8aMRD9K5bvwSwFEapZR2mIUqQAUVeF3JCzO7BuvbOV7YWWv9v+hp4zmxIKbQ+92H7z8d6MnuHqa+XEFn2wNaUy6XSlQMrrwtImlZNsfnF5ORhdJAMpQjRUGFcTpvIbBnml4ePN6oFbayRn8xIXLQ3fPiJX8vE4vRc34+lqcfshm8xnqRx0eq0Ca1JIZRnjl7dK7UJng5WJ9nOnQLU2ahuKqj3UluulhhNef4ZoNRnrxyTp9of90bAL6s8cvIxHGu7wqfEx8FRNpZPRIEl63SGoszAsak+mQp3tDMVPKKbfvTVEKGY2rx1j02LxgNEb1jpUw8zk5ctriSGgi/U6gjoGWTrkpcO8jHrZKh4RN1ZIYd8EE/f6zv9YW2obT4KQsRST0nXhW3JeqA7RoY17UzrdezPs90GdxWDuo87y8iftjCtQmEq8a2gTntBvQLVJuGcjNSzrkgI3OcNuoWfPUY9B/SsM6lfLYRrGx9d70wRqqoqd6YE6QXBNvKm3a7bisM0tfypDG8Ej/508cv7eVb26d+orM6v/+Hf6GbaWl3faAgAA";

  protected TestDataFactory() {
    throw new java.lang.UnsupportedOperationException(
        "This is a utility class and cannot be instantiated");
  }

  public static KonnektorConfigurationMutable createKonnektorConfigurationMutable() {
    return new KonnektorConfigurationMutableForTest()
        .connection(createKonnektorConnectionConfigurationMutable())
        .context(createKonnektorContext());
  }

  public static KonnektorContextProvider konnektorContextProvider() {
    return new KonnektorContextProvider(
        new KonnektorConfigurationProvider(createKonnektorConfigurationMutable()),
        konnektorInterfaceAssemblyMock());
  }

  public static Context createKonnektorContext() {
    return new Context(MANDANT_ID, CLIENTSYSTEM_ID, WORKPLACE_ID, USER_ID);
  }

  public static KonnektorConnectionConfigurationMutable
      createKonnektorConnectionConfigurationMutable() {
    return new KonnektorConnectionConfigurationMutableForTest()
        .address(createAddress())
        .tlsConfig(createTlsConfig())
        .proxyAddress(createProxyAddressConfig())
        .basicAuthentication(createBasicAuthenticationData());
  }

  public static AddressConfig createAddress() {
    return new AddressConfig(
        "localhost",
        Integer.parseInt(AddressConfig.DEFAULT_PORT),
        KonnektorInterfacesCxfImpl.HTTPS_PROTOCOL,
        "services");
  }

  public static TlsConfig createTlsConfig() {
    return new TlsConfig(
        true,
        new FileInfo(ResourceLoader.TEST_P12),
        "test1234",
        "PKCS12",
        List.of("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA", "TLS_DHE_RSA_WITH_AES_256_CBC_SHA"));
  }

  public static ProxyAddressConfig createProxyAddressConfig() {
    return new ProxyAddressConfig(
        "localhost", Integer.parseInt(ProxyAddressConfig.DEFAULT_PORT), true);
  }

  public static BasicAuthenticationConfig createBasicAuthenticationData() {
    return new BasicAuthenticationConfig("root", "root", true);
  }

  public static ContextType contextType() {
    final var context = new ContextType();
    context.setMandantId(MANDANT_ID);
    context.setClientSystemId(CLIENTSYSTEM_ID);
    context.setWorkplaceId(WORKPLACE_ID);
    context.setUserId(USER_ID);

    return context;
  }

  public static Status getStatusOk() {
    return new Status().withResult(STATUS_RESULT_OK);
  }

  public static Status getStatusWarning() {
    return new Status().withResult(STATUS_RESULT_WARNING);
  }

  public static Error getTelematikError() {

    final var objectFactory = new ObjectFactory();

    final var traceDetail = objectFactory.createErrorTraceDetail();
    traceDetail.setValue("This error is very very devastating in every detail");

    final var trace = objectFactory.createErrorTrace();
    trace.setLogReference("Test-Log-Ref");
    trace.setErrorType("TECHNICAL");
    trace.setEventID("Test-Event-Id");
    trace.setInstance("Unit-Test-Instance");
    trace.setCompType("Unit-Test");
    trace.setSeverity("ERROR");
    trace.setCode(BigInteger.valueOf(7208L));
    trace.setErrorText("Very very devastating error");
    trace.setDetail(traceDetail);

    final var error = objectFactory.createError();
    error.setTimestamp(
        DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(new GregorianCalendar()));
    error.setMessageID("Test-Msg-Id");
    error.getTrace().add(trace);

    return error;
  }

  public static CardInfoType cardInfoSmb() {
    final var cardInfo = new CardInfoType();
    cardInfo.setCardType(CardTypeType.SMC_B);
    cardInfo.setCardVersion(new CardVersion());
    cardInfo.setCardHandle("SMB123");
    cardInfo.setCardHolderName("Arztpraxis Unit Test Olé");
    cardInfo.setIccsn("80271282350235250218");
    cardInfo.setCtId("CT1");
    cardInfo.setSlotId(BigInteger.valueOf(1));

    return cardInfo;
  }

  public static CardInfoType cardInfoHba() {
    final var cardInfo = new CardInfoType();
    cardInfo.setCardType(CardTypeType.HBA);
    cardInfo.setCardVersion(new CardVersion());
    cardInfo.setCardHandle("HBA123");
    cardInfo.setCardHolderName("Arztpraxis Unit Test Olé");
    cardInfo.setIccsn("80271282350235250218");
    cardInfo.setCtId("CT1");
    cardInfo.setSlotId(BigInteger.valueOf(1));

    return cardInfo;
  }

  public static CardInfoType cardInfoEgk(final String kvnr) {
    final var cardInfo = new CardInfoType();
    cardInfo.setCardType(CardTypeType.EGK);
    cardInfo.setCardVersion(new CardVersion());
    cardInfo.setCardHandle("EGK456");
    cardInfo.setCardHolderName("Elfriede Barbara Gudrun Müller");
    cardInfo.setIccsn("80271282320235252170");
    cardInfo.setCtId("CT1");
    cardInfo.setSlotId(BigInteger.valueOf(2));
    cardInfo.setKvnr(kvnr);

    return cardInfo;
  }

  public static RegistryResponseType registryResponseSuccess() {
    final var registryResponse = new RegistryResponseType();

    registryResponse.setStatus(REGISTRY_RESPONSE_STATUS_SUCCESS);

    return registryResponse;
  }

  public static RetrieveDocumentSetResponseType retrieveDocumentSetResponse() {
    final var retrieveResponse = new RetrieveDocumentSetResponseType();

    retrieveResponse.setRegistryResponse(registryResponseSuccess());

    final var retrievedDocument = new RetrieveDocumentSetResponseType.DocumentResponse();
    final var putDoc =
        ResourceLoader.putDocumentWithFolderMetadataRequest().documentSets().getFirst();
    retrievedDocument.setDocumentUniqueId(
        ResourceLoader.retrieveDocumentsRequest().documentUniqueIds().getFirst());
    retrievedDocument.setDocument(putDoc.documentData().value());
    retrievedDocument.setMimeType(putDoc.documentMetadata().mimeType());
    retrievedDocument.setHomeCommunityId(HOME_COMMUNITY_ID);
    retrievedDocument.setRepositoryUniqueId(HOME_COMMUNITY_ID);

    retrieveResponse.getDocumentResponse().add(retrievedDocument);

    return retrieveResponse;
  }

  public static AdhocQueryResponse getSuccessResponse() {
    final var result = new AdhocQueryResponse();
    result.setStatus("urn:oasis:names:tc:ebxml-regrep:ResponseStatusType:Success");
    return result;
  }

  public static ReadVSDResponse createReadVSDResponse(String pruefungsnachweis) {
    var readVSDResponse = new ReadVSDResponse();
    readVSDResponse.setVSDStatus(new VSDStatusType().withStatus("1"));
    readVSDResponse.setAllgemeineVersicherungsdaten(
        (Base64.getDecoder().decode(ALLGEMEINE_VERSICHERUNGSDATEN.getBytes())));
    readVSDResponse.setGeschuetzteVersichertendaten(
        (Base64.getDecoder().decode(GESCHUETZTE_VERSICHERUNGSDATEN.getBytes())));
    readVSDResponse.setPersoenlicheVersichertendaten(
        Base64.getDecoder().decode(PERSOENLICHE_VERSICHERTENDATEN.getBytes()));
    readVSDResponse.setVSDStatus(new VSDStatusType().withStatus("1"));
    readVSDResponse.setPruefungsnachweis(decodeBase64(pruefungsnachweis));
    log.info("Prüfungsnachweis PZ1: {}", pruefungsnachweis);
    logResponse(readVSDResponse);
    return readVSDResponse;
  }

  public static ReadVSDResponse createReadVSDResponse() {
    return createReadVSDResponse(PRUEFUNGSNACHWEIS);
  }

  private static byte[] decodeBase64(String base64Data) {
    return Base64.getDecoder().decode(base64Data.getBytes());
  }

  private static void logResponse(ReadVSDResponse response) {
    byte[] xmlBytes = XmlUtils.marshal(response);
    String xmlResponse = new String(xmlBytes);
    log.info("ReadVSDResponse: {}", xmlResponse);
  }

  public static ReadVSDResponse readVSDWithUpdateNotPossible() {
    final var readVSDResponse = new ReadVSDResponse();
    readVSDResponse.setVSDStatus(new VSDStatusType().withStatus("1"));
    readVSDResponse.setAllgemeineVersicherungsdaten(
        ALLGEMEINE_VERSICHERUNGSDATEN.getBytes(StandardCharsets.UTF_8));
    readVSDResponse.setGeschuetzteVersichertendaten(
        GESCHUETZTE_VERSICHERUNGSDATEN.getBytes(StandardCharsets.UTF_8));
    readVSDResponse.setPersoenlicheVersichertendaten(
        PERSOENLICHE_VERSICHERTENDATEN.getBytes(StandardCharsets.UTF_8));
    readVSDResponse.setPruefungsnachweis(Base64.getDecoder().decode(pnWithoutUpdate.getBytes()));
    return readVSDResponse;
  }

  public static ReadVSDResponse readVSDResponseStrasseEmpty() {
    var readVSDResponse = new ReadVSDResponse();
    readVSDResponse.setVSDStatus(new VSDStatusType().withStatus("1"));
    readVSDResponse.setAllgemeineVersicherungsdaten(
        Base64.getDecoder().decode(ALLGEMEINE_VERSICHERUNGSDATEN.getBytes()));
    readVSDResponse.setGeschuetzteVersichertendaten(
        Base64.getDecoder().decode(GESCHUETZTE_VERSICHERUNGSDATEN.getBytes()));
    readVSDResponse.setPersoenlicheVersichertendaten(
        Base64.getDecoder().decode(PERSOENLICHE_VERSICHERTENDATEN_STRASSE_EMPTY_STR.getBytes()));
    readVSDResponse.setPruefungsnachweis(Base64.getDecoder().decode(PRUEFUNGSNACHWEIS.getBytes()));
    return readVSDResponse;
  }

  public static GetCardsResponse getCardsSmbResponse() {
    return new GetCardsResponse()
        .withStatus(getStatusOk())
        .withCards(new Cards().withCard(cardInfoSmb()));
  }

  public static GetCardsResponse getCardsHbaResponse() {
    return new GetCardsResponse()
        .withStatus(getStatusOk())
        .withCards(new Cards().withCard(cardInfoHba()));
  }

  public static GetCardsResponse getCardsEgkResponse(final String kvnr) {
    return new GetCardsResponse()
        .withStatus(getStatusOk())
        .withCards(new Cards().withCard(cardInfoEgk(kvnr)));
  }

  public static GetPinStatusResponse getPinStatusResponse(final PinStatusEnum pinStatus) {
    final BigInteger leftTries = new BigInteger("3");
    return new GetPinStatusResponse()
        .withStatus(getStatusOk())
        .withPinStatus(pinStatus)
        .withLeftTries(leftTries);
  }

  public static PinResponseType verifyPin(final Status status, final PinResultEnum pinResult) {
    final BigInteger leftTries = new BigInteger("3");
    return new PinResponseType()
        .withStatus(status)
        .withPinResult(pinResult)
        .withLeftTries(leftTries);
  }

  public static ReadCardCertificateResponse readCardCertificateResponse() {
    final var readCardCertificateResponse = new ReadCardCertificateResponse();

    readCardCertificateResponse.setStatus(getStatusOk());
    readCardCertificateResponse.setX509DataInfoList(new X509DataInfoListType());

    final var x509DataInfo = new X509DataInfoListType.X509DataInfo();
    x509DataInfo.setCertRef(CertRefEnum.C_AUT);

    final var x509Data = new X509DataInfoListType.X509DataInfo.X509Data();
    x509Data.setX509Certificate(ResourceLoader.autCertificateAsByteArray());
    x509Data.setX509SubjectName("Unfallkrankenhaus am SeeTEST-ONLY");
    x509Data.setX509IssuerSerial(new X509IssuerSerial());

    x509DataInfo.setX509Data(x509Data);
    readCardCertificateResponse.getX509DataInfoList().getX509DataInfo().add(x509DataInfo);

    return readCardCertificateResponse;
  }

  public static SignDocumentResponse getSignDocumentResponse() {
    return new SignDocumentResponse()
        .withSignResponse(
            new SignResponse()
                .withRequestID("reth456")
                .withStatus(getStatusOk())
                .withSignatureObject(
                    new SignatureObject()
                        .withBase64Signature(
                            new Base64Signature()
                                .withType(SignDocumentRequest.SignatureType.CMS.uri().toString())
                                .withValue("I am a Signature".getBytes(StandardCharsets.UTF_8))))
                .withOptionalOutputs(
                    new OptionalOutputs()
                        .withDocumentWithSignature(
                            new DocumentType().withBase64Data(new Base64Data()))));
  }

  public static SoapMessage createCxfSoapMessage() {
    final var soapMessage = new SoapMessage(Soap12.getInstance());
    final var interfaceInfo =
        new InterfaceInfo(new ServiceInfo(), new QName("urn:test:service", "TestServicePortName"));
    final var operationInfo =
        interfaceInfo.addOperation(new QName("urn:test:operation", "TestOperationName"));
    final var msgInfo = new MessageInfo(operationInfo, Type.OUTPUT, operationInfo.getName());
    soapMessage.put(MessageInfo.class, msgInfo);
    soapMessage.put(REQUESTOR_ROLE, Boolean.TRUE);
    return soapMessage;
  }

  public static AuthorInstitutionConfiguration authorInstitutionConfiguration(
      final boolean retrieveFromSmb) {
    return new AuthorInstitutionConfiguration(
        retrieveFromSmb, new AuthorInstitution("Arztpraxis", SMB_AUT_TELEMATIK_ID));
  }

  public static AuthorPerson authorPerson() {
    return new AuthorPerson(
        SMB_AUT_TELEMATIK_ID,
        "Müller-Lüdenscheidt",
        "Manfred",
        "Soldier Boy",
        "von und zu",
        "Prof. Dr. Freiherr",
        "1.2.276.0.76.4.16");
  }

  public static SubmissionSetAuthorConfiguration submissionSetAuthorConfiguration(
      final boolean useFirstDocumentAuthor, final boolean retrieveFromSmb) {
    return new SubmissionSetAuthorConfiguration(
        useFirstDocumentAuthor,
        authorPerson(),
        authorInstitutionConfiguration(retrieveFromSmb),
        "11^^^&1.3.6.1.4.1.19376.3.276.1.5.13&ISO");
  }

  public static DefaultdataInterface defaultdata(
      final boolean useFirstDocumentAuthor, final boolean retrieveFromSmb) {
    return () -> submissionSetAuthorConfiguration(useFirstDocumentAuthor, retrieveFromSmb);
  }

  public static KonnektorInterfaceAssembly konnektorInterfaceAssemblyMock() {
    return new KonnektorInterfaceAssemblyMock()
        .eventService(Mockito.mock(EventServicePortType.class))
        .cardService(Mockito.mock(CardServicePortType.class))
        .certificateService(Mockito.mock(CertificateServicePortType.class))
        .signatureService(Mockito.mock(SignatureServicePortType.class))
        .vsdService(Mockito.mock(VSDServicePortType.class))
        .authSignatureService(Mockito.mock(AuthSignatureServicePortType.class));
  }

  public static DocumentInterfaceAssembly documentInterfaceAssemblyMock() {
    return new DocumentInterfaceAssemblyMock()
        .documentService(Mockito.mock(IDocumentManagementPortType.class));
  }

  public static InsurantIdBuilder insurantBuilder() {
    return new InsurantIdBuilder().extension("X1105690");
  }

  @SneakyThrows
  public static void setupMocksForSmbInformationProvider(
      final KonnektorInterfaceAssembly konnektorInterfaceAssembly) {
    final var eventServiceMock = konnektorInterfaceAssembly.eventService();
    final var certificateServiceMock = konnektorInterfaceAssembly.certificateService();
    final var getCardsSmbResponse = getCardsSmbResponse();

    Mockito.when(eventServiceMock.getCards(Mockito.any())).thenReturn(getCardsSmbResponse);

    Mockito.when(certificateServiceMock.readCardCertificate(Mockito.any()))
        .thenReturn(readCardCertificateResponse());
  }

  public static DocumentConnectionConfigurationMutable createDocumentConnectionConfiguration() {
    return new DocumentConnectionConfigurationMutableForTest().address(createAddress());
  }

  @Data
  @Accessors(fluent = true)
  static class DocumentConnectionConfigurationMutableForTest
      implements DocumentConnectionConfigurationMutable {

    private AddressConfig address;
  }

  @Data
  @Accessors(fluent = true)
  static class KonnektorConnectionConfigurationMutableForTest
      implements KonnektorConnectionConfigurationMutable {

    private AddressConfig address;
    private BasicAuthenticationConfig basicAuthentication;
    private ProxyAddressConfig proxyAddress;
    private TlsConfig tlsConfig;
  }

  @Data
  @Accessors(fluent = true)
  static class KonnektorConfigurationMutableForTest implements KonnektorConfigurationMutable {

    private KonnektorConnectionConfigurationMutable connection;
    private Context context;
  }

  public static Response simulateInbound(final Response asOutbound) {
    final Response toReturn = spy(asOutbound);
    doAnswer(answer((Class<?> type) -> readEntity(toReturn, type)))
        .when(toReturn)
        .readEntity(ArgumentMatchers.<Class<?>>any());
    return toReturn;
  }

  public static UnaryOperator<byte[]> getContentSigner(final String cardHandle) {
    return tbsData ->
        createECDSASignature(
            "MEQCIH0AKLM8Pun/zBPjvVCjhfQ53zvZHZ6Xcfwy6ldCCHXIAiBpv2itD/XTGopk/MJkbNE4UKH1izM646uD371GspKysA=="
                .getBytes());
  }

  @SneakyThrows
  public static byte[] createECDSASignature(final byte[] data) {
    final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
    final ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
    keyGen.initialize(ecSpec, new SecureRandom());
    final KeyPair keyPair = keyGen.generateKeyPair();

    final Signature ecdsaSign = Signature.getInstance("SHA256withECDSA");
    ecdsaSign.initSign(keyPair.getPrivate());
    ecdsaSign.update(data);
    return ecdsaSign.sign();
  }

  @SuppressWarnings("unchecked")
  public static <T> T readEntity(final Response realResponse, final Class<T> t) {
    return (T) realResponse.getEntity();
  }

  public static SmbInformation createSmbInformation(String telematikId) {
    return new SmbInformation(
        telematikId, "iccsn", "cardHolderName", CARD_HANDLE, List.of("professionOids"));
  }

  public static SmbInformation createSmbInformation() {
    return createSmbInformation("telematikId");
  }

  public static HbaInformation createHbaInformation() {
    return new HbaInformation(
        "telematikId2", "iccsn2", "cardHolderName2", "cardHandle2", List.of("professionOids"));
  }
}
