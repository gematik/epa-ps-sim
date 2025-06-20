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
package de.gematik.epa.konnektor.cxf;

import static de.gematik.epa.utils.LoggingFeatureUtil.newLoggingFeature;
import static de.gematik.epa.utils.XmlUtils.getJaxWsProxyFactoryBean;
import static jakarta.xml.ws.soap.SOAPBinding.SOAP11HTTP_BINDING;

import de.gematik.epa.api.testdriver.config.AddressConfig;
import de.gematik.epa.api.testdriver.config.BasicAuthenticationConfig;
import de.gematik.epa.api.testdriver.config.KonnektorConnectionConfiguration;
import de.gematik.epa.api.testdriver.config.KonnektorConnectionConfigurationDTO;
import de.gematik.epa.api.testdriver.config.ProxyAddressConfig;
import de.gematik.epa.konnektor.KonnektorContextProvider;
import de.gematik.epa.konnektor.KonnektorInterfaceAssembly;
import de.gematik.epa.konnektor.client.CardServiceClient;
import de.gematik.epa.utils.ThrowingFunction;
import de.gematik.epa.utils.XmlUtils;
import jakarta.ws.rs.InternalServerErrorException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.slf4j.event.Level;
import telematik.ws.conn.SdsApi;
import telematik.ws.conn.authsignatureservice.wsdl.v7_4.AuthSignatureService;
import telematik.ws.conn.authsignatureservice.wsdl.v7_4.AuthSignatureServicePortType;
import telematik.ws.conn.cardservice.wsdl.v8_1.CardService;
import telematik.ws.conn.cardservice.wsdl.v8_1.CardServicePortType;
import telematik.ws.conn.certificateservice.wsdl.v6_0.CertificateService;
import telematik.ws.conn.certificateservice.wsdl.v6_0.CertificateServicePortType;
import telematik.ws.conn.eventservice.wsdl.v6_1.EventService;
import telematik.ws.conn.eventservice.wsdl.v6_1.EventServicePortType;
import telematik.ws.conn.plus.ObjectFactory;
import telematik.ws.conn.servicedirectory.xsd.v3_1.ConnectorServices;
import telematik.ws.conn.serviceinformation.xsd.v2_0.EndpointType;
import telematik.ws.conn.signatureservice.wsdl.v7_5.SignatureService;
import telematik.ws.conn.signatureservice.wsdl.v7_5.SignatureServicePortType;
import telematik.ws.conn.vsds.vsdservice.wsdl.v5_2.VSDService;
import telematik.ws.conn.vsds.vsdservice.wsdl.v5_2.VSDServicePortType;

/**
 * Implementation of the {@link KonnektorInterfaceAssembly}, which uses Apache CXF to generate
 * client implementations for the Konnektor web services.
 */
@Getter
@Setter
@Accessors(fluent = true)
public class KonnektorInterfacesCxfImpl implements KonnektorInterfaceAssembly {

  public static final String HTTPS_PROTOCOL = "https";

  protected KonnektorConnectionConfiguration configuration;

  private final FileLoader fileLoader;

  @Getter(lazy = true)
  private final LoggingFeature loggingFeature = newLoggingFeature(Level.DEBUG);

  Boolean isTlsPreferred = Boolean.TRUE;

  protected ConnectorServices connectorServices;

  protected EventServicePortType eventService;
  protected CardServicePortType cardService;
  protected CertificateServicePortType certificateService;
  protected SignatureServicePortType signatureService;
  protected VSDServicePortType vsdService;
  protected AuthSignatureServicePortType authSignatureService;

  public KonnektorInterfacesCxfImpl(FileLoader fileLoader) {
    this.fileLoader = fileLoader;
  }

  /**
   * Change the configuration used for establishing connections to the Konnektor webservices<br>
   * Recreates all client proxies for the Konnektor web services, thus making configuration change
   * at runtime possible.
   *
   * @param newConfiguration the new configuration data to be used for connecting to the Konnektor
   * @return Reference to the object itself, so the method can be used in chained method calls.
   */
  public KonnektorInterfacesCxfImpl update(KonnektorConnectionConfiguration newConfiguration) {
    this.configuration = newConfiguration;
    isTlsPreferred = determineIfTlsPreferred();
    connectorServices = sdsApi().getConnectorSds();
    eventService = createEventService();
    cardService = createCardService();
    certificateService = createCertificateService();
    signatureService = createSignatureService();
    vsdService = createVSDService();
    authSignatureService = createAuthSignatureService();
    return this;
  }

  public void unlockSmb(KonnektorContextProvider contextProvider)
      throws InternalServerErrorException {
    try (CardServiceClient cardServiceClient = new CardServiceClient(contextProvider, this)) {
      cardServiceClient.verifySmb();
    } catch (Exception e) {
      throw new InternalServerErrorException("Operation unlockSmb failed with an exception", e);
    }
  }

  /**
   * Get the client implementation of the {@link SdsApi}, for the retrieval of the connection
   * information of the Konnektor web services.<br>
   * The implementation is created using a {@link JAXRSClientFactoryBean} and uses the configured
   * {@link KonnektorConnectionConfigurationDTO} data.<br>
   * Overwrite this method (e.g. for test purposes), if the client implementation should be created
   * another way.
   *
   * @return the client implementation of the SdsApi
   */
  @SneakyThrows
  protected SdsApi sdsApi() {
    var factoryBean = new JAXRSClientFactoryBean();
    factoryBean.setServiceClass(SdsApi.class);
    factoryBean.getFeatures().add(loggingFeature());
    factoryBean.setAddress(configuration.address().createUrl().toString());

    final SdsApi sdsApi = factoryBean.create(SdsApi.class);

    WebClient.client(sdsApi).accept("text/xml", "application/xml");

    var sdsHttpConduit = WebClient.getConfig(sdsApi).getHttpConduit();

    if (isTlsPreferred) {
      configureTls(sdsHttpConduit);
    }

    configureBasicAuthenticationIfEnabled(sdsHttpConduit);

    configureProxyIfEnabled(sdsHttpConduit);

    return sdsApi;
  }

  /**
   * Create the client implementation for the Konnektor Webservices using a {@link
   * JaxWsProxyFactoryBean}.<br>
   * If the way the client implementations are created, is to be changed, e.g. for testing purposes,
   * this method can be overwritten.
   *
   * @param portType the class type, for which the client implementation is to be created
   * @param soapBinding the SOAP Binding to use (see {@link jakarta.xml.ws.soap.SOAPBinding} for
   *     possible values)
   * @param endpointAddress endpoint address of the Konnektor service with which to communicate
   * @param addConf If additional interceptors, features, et cetera are to be configured for the
   *     client, a consumer can be provided here, with the code which does. If this is not
   *     necessary, simply pass null.
   * @param <T> type of the Konnektor service, for which the client implementation is to be created
   * @return T returns the created client implementation of the given class type
   */
  protected <T> T getClientProxyImpl(
      @NonNull final Class<T> portType,
      @NonNull final String soapBinding,
      @NonNull final String endpointAddress,
      final Consumer<JaxWsProxyFactoryBean> addConf) {
    final JaxWsProxyFactoryBean jaxWsProxyFactory =
        getJaxWsProxyFactoryBean(portType, soapBinding, endpointAddress, addConf);

    final T proxy = jaxWsProxyFactory.create(portType);

    var httpConduit = (HTTPConduit) ClientProxy.getClient(proxy).getConduit();

    if (isTlsPreferred) {
      configureTls(httpConduit);
    }

    configureBasicAuthenticationIfEnabled(httpConduit);

    configureProxyIfEnabled(httpConduit);

    return proxy;
  }

  protected TLSClientParameters tlsClientParameters() {
    final TLSClientParameters tlsParams = new TLSClientParameters();
    tlsParams.setDisableCNCheck(true);

    tlsParams.setTrustManagers(
        new TrustManager[] {
          new X509TrustManager() {
            @Override
            @SuppressWarnings("java:S4830")
            public void checkClientTrusted(final X509Certificate[] chain, final String authType) {
              /* We dont have the certifactes of the konnektor, so do nothing here*/
            }

            @Override
            @SuppressWarnings("java:S4830")
            public void checkServerTrusted(final X509Certificate[] chain, final String authType) {
              /* We dont have the certifactes of the konnektor, so do nothing here*/
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
              return new X509Certificate[0];
            }
          }
        });

    return tlsParams;
  }

  private <T> T getClientProxyImpl(final Class<T> portType, final String endpointAddress) {
    return getClientProxyImpl(portType, SOAP11HTTP_BINDING, endpointAddress, null);
  }

  /**
   * Creates the actual client implementation of the Konnektors {@link SignatureServicePortType}
   * interface.<br>
   * Endpoint of the Konnektor Service to talk to is retrieved from the information in the {@link
   * #connectorServices()} bean.
   *
   * @return SignatureServicePortType implementation
   */
  private SignatureServicePortType createSignatureService() {
    return getClientProxyImpl(
        SignatureServicePortType.class,
        SOAP11HTTP_BINDING,
        readServiceEndpoint(SignatureService.SERVICE.getLocalPart(), "7.5", "7"),
        jaxWsProxyFactory ->
            XmlUtils.registerObjectFactory(jaxWsProxyFactory, ObjectFactory.class));
  }

  /**
   * Creates the actual client implementation of the Konnektors {@link EventServicePortType}
   * interface.<br>
   * Endpoint of the Konnektor Service to talk to is retrieved from the information in the {@link
   * #connectorServices()} bean.
   *
   * @return EventServicePortType implementation
   */
  private EventServicePortType createEventService() {
    return getClientProxyImpl(
        EventServicePortType.class, readServiceEndpoint(EventService.SERVICE.getLocalPart(), ""));
  }

  /**
   * Creates the actual client implementation of the Konnektors {@link VSDServicePortType}
   * interface.<br>
   * Endpoint of the Konnektor Service to talk to is retrieved from the information in the {@link
   * #connectorServices()} bean.
   *
   * @return VSDServicePortType implementation
   */
  private VSDServicePortType createVSDService() {
    return getClientProxyImpl(
        VSDServicePortType.class, readServiceEndpoint(VSDService.SERVICE.getLocalPart(), ""));
  }

  /**
   * Creates the actual client implementation of the Konnektors {@link CertificateServicePortType}
   * interface.<br>
   * Endpoint of the Konnektor Service to talk to is retrieved from the information in the {@link
   * #connectorServices()} bean.
   *
   * @return CertificateServicePortType implementation
   */
  private CertificateServicePortType createCertificateService() {
    return getClientProxyImpl(
        CertificateServicePortType.class,
        readServiceEndpoint(CertificateService.SERVICE.getLocalPart(), "6.0.1", "6"));
  }

  /**
   * Creates the actual client implementation of the Konnektors {@link CardServicePortType}
   * interface.<br>
   * Endpoint of the Konnektor Service to talk to is retrieved from the information in the {@link
   * #connectorServices()} bean.
   *
   * @return CardServicePortType implementation
   */
  private CardServicePortType createCardService() {
    return getClientProxyImpl(
        CardServicePortType.class,
        readServiceEndpoint(CardService.SERVICE.getLocalPart(), "8.1.2", "8.1", "8"));
  }

  /**
   * Creates the actual client implementation of the Konnektors {@link AuthSignatureServicePortType}
   * interface.<br>
   * Endpoint of the Konnektor Service to talk to is retrieved from the information in the {@link
   * #connectorServices()} bean.
   *
   * @return AuthSignatureServicePortType implementation
   */
  private AuthSignatureServicePortType createAuthSignatureService() {
    return getClientProxyImpl(
        AuthSignatureServicePortType.class,
        readServiceEndpoint(AuthSignatureService.SERVICE.getLocalPart(), "7.4.1", "7.4", "7"));
  }

  private String readServiceEndpoint(String serviceName, String... serviceVersionStartsWith) {
    for (String svsw : serviceVersionStartsWith) {
      var endpoint = readSingleServiceEndpoint(serviceName, svsw);
      if (Objects.nonNull(endpoint)) {
        return endpoint;
      }
    }

    throw new IllegalArgumentException(
        String.format(
            "No usable service endpoint configuration found for service %s in version %s",
            serviceName, Arrays.toString(serviceVersionStartsWith)));
  }

  private String readSingleServiceEndpoint(String serviceName, String serviceVersionStartsWith) {
    return connectorServices().getServiceInformation().getService().stream()
        .filter(service -> serviceName.equals(service.getName()))
        .flatMap(service -> service.getVersions().getVersion().stream())
        .filter(
            versionedService -> versionedService.getVersion().startsWith(serviceVersionStartsWith))
        .map(
            versionedService ->
                Optional.ofNullable(versionedService.getEndpointTLS())
                    .filter(endpoint -> connectorServices().isTLSMandatory() || isTlsPreferred)
                    .orElse(versionedService.getEndpoint()))
        .map(EndpointType::getLocation)
        .findFirst()
        .orElse(null);
  }

  private Boolean determineIfTlsPreferred() {
    return Optional.ofNullable(configuration)
        .map(KonnektorConnectionConfiguration::address)
        .map(AddressConfig::protocol)
        .map(prtcl -> prtcl.equalsIgnoreCase(HTTPS_PROTOCOL))
        .orElse(Boolean.TRUE);
  }

  @SneakyThrows
  private void configureTls(final HTTPConduit httpConduit) {
    var tlsConfig =
        Objects.requireNonNull(
            configuration.tlsConfig(),
            "No configuration data present for TLS connection to the Konnektor");

    var tlsParams = tlsClientParameters();
    tlsParams.setCipherSuites(tlsConfig.ciphersuites());

    if (tlsConfig.keystoreEnabled()) {
      final KeyStore keyStore =
          KeyStore.getInstance(Objects.requireNonNull(tlsConfig.keystoretype()));

      var keystorePwd =
          Objects.requireNonNull(
                  tlsConfig.keystorepassword(),
                  "No password is set in the TLS configuration for the Konnektor connection")
              .toCharArray();

      keyStore.load(
          tlsConfig.keystorefile().isFilePath()
              ? fileLoader.apply(tlsConfig.keystorefile().getFilePath())
              : new ByteArrayInputStream(tlsConfig.keystorefile().getFileContent().value()),
          keystorePwd);

      final KeyManagerFactory keyFactory =
          KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyFactory.init(keyStore, keystorePwd);

      tlsParams.setKeyManagers(keyFactory.getKeyManagers());
    }

    httpConduit.setTlsClientParameters(tlsParams);
  }

  private void configureBasicAuthenticationIfEnabled(HTTPConduit httpConduit) {
    Optional.ofNullable(configuration.basicAuthentication())
        .filter(BasicAuthenticationConfig::enabled)
        .ifPresent(
            ba -> {
              AuthorizationPolicy authorizationPolicy = new AuthorizationPolicy();
              authorizationPolicy.setUserName(Objects.requireNonNull(ba.username()));
              authorizationPolicy.setPassword(Objects.requireNonNull(ba.password()));
              authorizationPolicy.setAuthorizationType("Basic");

              httpConduit.setAuthorization(authorizationPolicy);
            });
  }

  private void configureProxyIfEnabled(HTTPConduit httpConduit) {
    Optional.ofNullable(configuration.proxyAddress())
        .filter(ProxyAddressConfig::enabled)
        .ifPresent(
            pa -> {
              httpConduit.getClient().setProxyServer(Objects.requireNonNull(pa.address()));
              httpConduit.getClient().setProxyServerPort(pa.port());
            });
  }

  public interface FileLoader extends ThrowingFunction<String, InputStream> {}
}
