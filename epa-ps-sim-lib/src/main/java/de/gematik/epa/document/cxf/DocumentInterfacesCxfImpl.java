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
package de.gematik.epa.document.cxf;

import static de.gematik.epa.utils.LoggingFeatureUtil.newLoggingFeature;
import static de.gematik.epa.utils.XmlUtils.getJaxWsProxyFactoryBean;
import static jakarta.xml.ws.soap.SOAPBinding.SOAP12HTTP_MTOM_BINDING;

import de.gematik.epa.api.testdriver.config.AddressConfig;
import de.gematik.epa.api.testdriver.config.DocumentConnectionConfiguration;
import de.gematik.epa.client.JaxRsOutgoingRequestInterceptor;
import de.gematik.epa.client.JaxRsOutgoingXDSRequestInterceptor;
import de.gematik.epa.config.AppConfig;
import de.gematik.epa.document.DocumentInterfaceAssembly;
import de.gematik.epa.konnektor.cxf.interceptors.MtomConfigOutInterceptor;
import de.gematik.epa.utils.ThrowingFunction;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.slf4j.event.Level;
import xds.document.wsdl.IDocumentManagementPortType;
import xds.document.wsdl.XDSDocumentService;

@Getter
@Setter
@Accessors(fluent = true)
public class DocumentInterfacesCxfImpl implements DocumentInterfaceAssembly {

  protected DocumentConnectionConfiguration configuration;
  protected IDocumentManagementPortType documentService;
  private final FileLoader fileLoader;

  public DocumentInterfacesCxfImpl(FileLoader fileLoader) {
    this.fileLoader = fileLoader;
  }

  @Getter(lazy = true)
  private final LoggingFeature loggingFeature = newLoggingFeature(Level.DEBUG);

  public DocumentInterfacesCxfImpl update(
      DocumentConnectionConfiguration newConfiguration, AppConfig appConfig) {
    this.configuration = newConfiguration;
    AddressConfig addressConfig = newConfiguration.address();
    String serviceUrl = createServiceUrl(addressConfig);
    documentService = createDocumentService(serviceUrl, appConfig);
    return this;
  }

  public String createServiceUrl(AddressConfig addressConfig) {
    try {
      URL url = addressConfig.createUrl();
      return url + XDSDocumentService.IDocumentManagement.getLocalPart();
    } catch (MalformedURLException e) {
      throw new IllegalStateException("Error creating the service URL", e);
    }
  }

  public IDocumentManagementPortType createDocumentService(String serviceUrl, AppConfig appConfig) {
    return getClientProxyImpl(
        IDocumentManagementPortType.class,
        SOAP12HTTP_MTOM_BINDING,
        serviceUrl,
        jaxWsProxyFactory -> {
          jaxWsProxyFactory.getFeatures().add(new WSAddressingFeature());
          jaxWsProxyFactory.getOutInterceptors().add(new MtomConfigOutInterceptor());
          jaxWsProxyFactory.getOutInterceptors().add(new JaxRsOutgoingRequestInterceptor());
          jaxWsProxyFactory
              .getOutInterceptors()
              .add(new JaxRsOutgoingXDSRequestInterceptor(appConfig));
        });
  }

  /**
   * Create the client implementation for the Webservice using a {@link
   * org.apache.cxf.jaxws.JaxWsProxyFactoryBean}.<br>
   *
   * @param portType the class type, for which the client implementation is to be created
   * @param soapBinding the SOAP Binding to use (see {@link jakarta.xml.ws.soap.SOAPBinding} for
   *     possible values)
   * @param endpointAddress endpoint address of the Konnektor service with which to communicate
   * @param addConf If additional interceptors, features, et cetera are to be configured for the
   *     client, a consumer can be provided here, with the code which does. If this is not
   *     necessary, simply pass null.
   * @param <T> type of the service, for which the client implementation is to be created
   * @return T returns the created client implementation of the given class type
   */
  protected <T> T getClientProxyImpl(
      @NonNull final Class<T> portType,
      @NonNull final String soapBinding,
      @NonNull final String endpointAddress,
      final Consumer<JaxWsProxyFactoryBean> addConf) {
    final JaxWsProxyFactoryBean jaxWsProxyFactory =
        getJaxWsProxyFactoryBean(portType, soapBinding, endpointAddress, addConf);

    return jaxWsProxyFactory.create(portType);
  }

  public interface FileLoader extends ThrowingFunction<String, InputStream> {}
}
