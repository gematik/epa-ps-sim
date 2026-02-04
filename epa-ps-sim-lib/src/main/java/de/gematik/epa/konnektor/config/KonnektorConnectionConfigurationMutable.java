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
package de.gematik.epa.konnektor.config;

import de.gematik.epa.api.testdriver.config.AddressConfig;
import de.gematik.epa.api.testdriver.config.BasicAuthenticationConfig;
import de.gematik.epa.api.testdriver.config.KonnektorConnectionConfiguration;
import de.gematik.epa.api.testdriver.config.ProxyAddressConfig;
import de.gematik.epa.api.testdriver.config.TlsConfig;
import java.util.Optional;

public interface KonnektorConnectionConfigurationMutable extends KonnektorConnectionConfiguration {

  KonnektorConnectionConfigurationMutable address(AddressConfig addressConfig);

  KonnektorConnectionConfigurationMutable tlsConfig(TlsConfig tlsConfig);

  KonnektorConnectionConfigurationMutable proxyAddress(ProxyAddressConfig proxyAddressConfig);

  KonnektorConnectionConfigurationMutable basicAuthentication(
      BasicAuthenticationConfig basicAuthenticationConfig);

  default KonnektorConnectionConfigurationMutable replace(
      KonnektorConnectionConfiguration replaceData) {
    return this.address(replaceData.address())
        .basicAuthentication(replaceData.basicAuthentication())
        .proxyAddress(replaceData.proxyAddress())
        .tlsConfig(replaceData.tlsConfig());
  }

  default KonnektorConnectionConfigurationMutable update(
      KonnektorConnectionConfiguration updateData) {
    Optional.ofNullable(updateData.address()).ifPresent(this::address);
    Optional.ofNullable(updateData.tlsConfig()).ifPresent(this::tlsConfig);
    Optional.ofNullable(updateData.proxyAddress()).ifPresent(this::proxyAddress);
    Optional.ofNullable(updateData.basicAuthentication()).ifPresent(this::basicAuthentication);

    return this;
  }
}
