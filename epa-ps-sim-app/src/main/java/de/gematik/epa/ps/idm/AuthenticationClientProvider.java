/*-
 * #%L
 * epa-ps-sim-app
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
package de.gematik.epa.ps.idm;

import static de.gematik.epa.utils.LoggingFeatureUtil.newLoggingFeature;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import de.gematik.epa.api.authorization.client.AuthorizationSmcBApi;
import de.gematik.epa.api.vau.client.VauApi;
import de.gematik.epa.authentication.AuthenticationService;
import de.gematik.epa.client.JaxRsClientWrapper;
import de.gematik.epa.client.JaxRsOutgoingRequestInterceptor;
import de.gematik.epa.konnektor.KonnektorContextProvider;
import de.gematik.epa.konnektor.KonnektorInterfaceAssembly;
import de.gematik.epa.konnektor.SmbInformationProvider;
import de.gematik.epa.ps.config.EpaProxyConfiguration;
import de.gematik.epa.ps.idm.config.AuthServerConfiguration;
import de.gematik.epa.ps.idm.config.IdpServerConfiguration;
import de.gematik.epa.ps.kob.config.VauProxyConfiguration;
import de.gematik.idp.client.AuthenticatorClient;
import de.gematik.idp.client.IdpClient;
import kong.unirest.core.Proxy;
import kong.unirest.core.Unirest;
import kong.unirest.core.UnirestInstance;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.event.Level;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@Accessors(fluent = true)
@EnableConfigurationProperties({
  AuthServerConfiguration.class,
  IdpServerConfiguration.class,
  EpaProxyConfiguration.class
})
public class AuthenticationClientProvider {

  private final AuthServerConfiguration authServerConfiguration;
  private final IdpServerConfiguration idpServerConfiguration;
  private final EpaProxyConfiguration epaProxyConfiguration;
  private final VauProxyConfiguration vauProxyConfiguration;
  private final IdpClient.IdpClientBuilder idpClientBuilder;
  private final AuthorizationSmcBApi authorizationSmcBApi;
  private final LoggingFeature loggingFeature = newLoggingFeature(Level.INFO);

  public AuthenticationClientProvider(
      final AuthServerConfiguration authServerConfiguration,
      final IdpServerConfiguration idpServerConfiguration,
      final EpaProxyConfiguration epaProxyConfiguration,
      final VauProxyConfiguration vauProxyConfiguration) {
    this.authServerConfiguration = authServerConfiguration;
    this.idpServerConfiguration = idpServerConfiguration;
    this.epaProxyConfiguration = epaProxyConfiguration;
    this.vauProxyConfiguration = vauProxyConfiguration;
    this.idpClientBuilder =
        IdpClient.builder()
            .discoveryDocumentUrl(getIdmServerUrl())
            .authenticatorClient(new AuthenticatorClient(getIdpUnirestInstance()));
    this.authorizationSmcBApi = initializeApiClient(getAuthServerUrl());
  }

  String getAuthServerUrl() {
    return authServerConfiguration.getProtocol()
        + "://"
        + authServerConfiguration.getHost()
        + ":"
        + authServerConfiguration.getPort();
  }

  String getIdmServerUrl() {
    return idpServerConfiguration.getProtocol()
        + "://"
        + idpServerConfiguration.getHost()
        + ":"
        + idpServerConfiguration.getPort()
        + "/"
        + idpServerConfiguration.getPath();
  }

  UnirestInstance getIdpUnirestInstance() {
    UnirestInstance unirestInstance = Unirest.spawnInstance();
    // for idpClient to use proxy
    if (StringUtils.isNotBlank(epaProxyConfiguration.getHost())
        && StringUtils.isNotBlank(epaProxyConfiguration.getPort())) {
      unirestInstance
          .config()
          .proxy(
              new Proxy(
                  epaProxyConfiguration.getHost(),
                  Integer.parseInt(epaProxyConfiguration.getPort())));
    }

    // for idpClient to ignore ssl verification
    if (!idpServerConfiguration.isVerifySsl()) {
      unirestInstance.config().verifySsl(false);
    }

    unirestInstance.config().followRedirects(idpServerConfiguration.isFollowRedirects());
    return unirestInstance;
  }

  protected AuthorizationSmcBApi initializeApiClient(final String serverUrl) {
    var factoryBean = new JAXRSClientFactoryBean();
    factoryBean.setServiceClass(AuthorizationSmcBApi.class);
    factoryBean.setAddress(serverUrl);
    factoryBean.setProvider(new JacksonJsonProvider());
    factoryBean.getFeatures().add(loggingFeature);
    var api = factoryBean.create(AuthorizationSmcBApi.class);
    WebClient.client(api).accept("application/json");
    // don't follow redirects
    ClientConfiguration config = WebClient.getConfig(api);
    config.getRequestContext().put("http.redirect.relative.uri", "false");
    config.getOutInterceptors().add(new JaxRsOutgoingRequestInterceptor());
    return api;
  }

  @Bean
  public AuthenticationService authenticationService(
      final KonnektorContextProvider contextProvider,
      final KonnektorInterfaceAssembly konnektorInterfaceAssembly,
      final SmbInformationProvider smbInformationProvider) {
    return new AuthenticationService(
        this.authorizationSmcBApi,
        this.idpClientBuilder,
        contextProvider,
        konnektorInterfaceAssembly,
        smbInformationProvider,
        new JaxRsClientWrapper<>(
            vauProxyConfiguration.getVauHostUrl(),
            vauProxyConfiguration.getUserAgent(),
            VauApi.class),
        this.authServerConfiguration.getUserAgent());
  }
}
