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
package de.gematik.epa.ps.mhd;

import de.gematik.epa.api.mhd.client.DocumentReferenceApi;
import de.gematik.epa.api.mhd.client.RetrieveDocumentApi;
import de.gematik.epa.api.mhd_dev.client.DevReferenceApi;
import de.gematik.epa.client.JaxRsClientWrapper;
import de.gematik.epa.mhd.MhdService;
import de.gematik.epa.ps.config.EpaProxyConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@Accessors(fluent = true)
@EnableConfigurationProperties({MhdConfiguration.class, EpaProxyConfiguration.class})
@RequiredArgsConstructor
public class MhdClientProvider {

  private final MhdConfiguration mhdConfiguration;

  @Bean
  public MhdService mhdService() {
    return new MhdService(
        mhdConfiguration.getUserAgent(),
        createClientWrapper(DocumentReferenceApi.class),
        createClientWrapper(RetrieveDocumentApi.class),
        createClientWrapper(DevReferenceApi.class));
  }

  private <T> JaxRsClientWrapper<T> createClientWrapper(Class<T> apiClass) {
    return new JaxRsClientWrapper<>(
            mhdConfiguration.getServerUrl(), mhdConfiguration.getUserAgent(), apiClass)
        .makeInsecureTlsClient();
  }
}
