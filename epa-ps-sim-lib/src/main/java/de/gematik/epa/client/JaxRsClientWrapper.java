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
package de.gematik.epa.client;

import static org.apache.cxf.transport.https.InsecureTrustManager.getNoOpX509TrustManagers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import de.gematik.epa.utils.LoggingFeatureUtil;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.net.ssl.SSLContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.event.Level;

@Slf4j
@Getter
public class JaxRsClientWrapper<T> {
  public static final String ERROR_MESSAGE = "serverUrl for underlying API must not be null";
  private static final long TIMEOUT_IN_MILLISECONDS = 30000L;
  private final T serviceApi;
  private final LoggingFeature loggingFeature = LoggingFeatureUtil.newLoggingFeature(Level.INFO);
  private final String userAgent;
  private final String url;

  public JaxRsClientWrapper(final String serverUrl, final String userAgent, Class<T> serviceClass) {
    this(serverUrl, userAgent, serviceClass, null, null);
  }

  public JaxRsClientWrapper(
      final String serverUrl,
      final String userAgent,
      Class<T> serviceClass,
      String proxyHost,
      String proxyPort) {
    log.debug(
        "Initializing ServiceApi for API {} and URL {}", serviceClass.getSimpleName(), serverUrl);
    this.serviceApi = initialize(serverUrl, serviceClass, proxyHost, proxyPort);
    this.userAgent = userAgent;
    this.url = serverUrl;
  }

  private T initialize(
      final String serverUrl, Class<T> serviceClass, String proxyHost, String proxyPort) {
    if (serverUrl == null) throw new IllegalArgumentException(ERROR_MESSAGE);

    var factoryBean = new JAXRSClientFactoryBean();
    factoryBean.setServiceClass(serviceClass);
    factoryBean.setAddress(serverUrl);

    var provider = new JacksonJsonProvider();
    var om = new ObjectMapper();
    om.registerModule(new JavaTimeModule());
    om.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
    provider.setMapper(om);
    provider.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    provider.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    factoryBean.setProvider(provider);
    factoryBean.getFeatures().add(loggingFeature);
    factoryBean.getOutInterceptors().add(new JaxRsOutgoingRequestInterceptor());

    var api = factoryBean.create(serviceClass);
    WebClient.client(api).accept("application/json");
    WebClient.getConfig(api)
        .getHttpConduit()
        .getClient()
        .setConnectionTimeout(TIMEOUT_IN_MILLISECONDS);
    WebClient.getConfig(api)
        .getHttpConduit()
        .getClient()
        .setReceiveTimeout(TIMEOUT_IN_MILLISECONDS);

    if (StringUtils.isNotBlank(proxyHost) && StringUtils.isNotBlank(proxyPort)) {
      WebClient.getConfig(api).getHttpConduit().getClient().setProxyServer(proxyHost);
      WebClient.getConfig(api)
          .getHttpConduit()
          .getClient()
          .setProxyServerPort(Integer.parseInt(proxyPort));
    }

    return api;
  }

  public JaxRsClientWrapper<T> makeInsecureTlsClient() {
    // disable SSL chain validation to not maintain a truststore here (workaround)
    WebClient.getConfig(this.serviceApi)
        .getHttpConduit()
        .setTlsClientParameters(getInsecureTlsClientParameters());
    return this;
  }

  private static TLSClientParameters getInsecureTlsClientParameters() {
    TLSClientParameters tls = new TLSClientParameters();
    try {
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, getNoOpX509TrustManagers(), new SecureRandom());
      tls.setSSLSocketFactory(sslContext.getSocketFactory());
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      throw new RuntimeException("Failed to set SSL configuration with InsecureTrustManager", e);
    }
    return tls;
  }
}
