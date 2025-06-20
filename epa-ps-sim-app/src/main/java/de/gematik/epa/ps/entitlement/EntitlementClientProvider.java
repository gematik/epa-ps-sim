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
package de.gematik.epa.ps.entitlement;

import de.gematik.epa.api.entitlement.client.EntitlementsApi;
import de.gematik.epa.api.entitlement.client.UserBlockingApi;
import de.gematik.epa.client.JaxRsClientWrapper;
import de.gematik.epa.entitlement.EntitlementService;
import de.gematik.epa.konnektor.KonnektorContextProvider;
import de.gematik.epa.konnektor.KonnektorInterfaceAssembly;
import de.gematik.epa.konnektor.SmbInformationProvider;
import de.gematik.epa.konnektor.client.VSDServiceClient;
import de.gematik.epa.ps.entitlement.config.EntitlementServerConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@Accessors(fluent = true)
@EnableConfigurationProperties({EntitlementServerConfiguration.class})
@RequiredArgsConstructor
public class EntitlementClientProvider {

  private final EntitlementServerConfiguration entitlementServerConfiguration;

  String getServerUrl() {
    return entitlementServerConfiguration.getProtocol()
        + "://"
        + entitlementServerConfiguration.getHost()
        + ":"
        + entitlementServerConfiguration.getPort();
  }

  @Bean
  public EntitlementService entitlementService(
      final KonnektorContextProvider contextProvider,
      final KonnektorInterfaceAssembly konnektorInterfaceAssembly,
      final SmbInformationProvider smbInformationProvider,
      final VSDServiceClient vsdServiceClient) {
    final var entitlementClientWrapper =
        new JaxRsClientWrapper<>(
            getServerUrl(), entitlementServerConfiguration.getUserAgent(), EntitlementsApi.class);
    final var blockingClientWrapper =
        new JaxRsClientWrapper<>(
            getServerUrl(), entitlementServerConfiguration.getUserAgent(), UserBlockingApi.class);
    return new EntitlementService(
        entitlementClientWrapper,
        blockingClientWrapper,
        contextProvider,
        konnektorInterfaceAssembly,
        smbInformationProvider,
        vsdServiceClient);
  }
}
