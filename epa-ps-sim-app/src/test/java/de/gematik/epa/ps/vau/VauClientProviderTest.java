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
package de.gematik.epa.ps.vau;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.epa.ps.kob.config.VauProxyConfiguration;
import de.gematik.epa.vau.VauService;
import org.junit.jupiter.api.Test;

class VauClientProviderTest {

  @Test
  void shouldReturnVauServiceSuccessfully() {
    VauProxyConfiguration config = mock(VauProxyConfiguration.class);
    when(config.getVauHostUrl()).thenReturn("http://localhost");
    when(config.getUserAgent()).thenReturn("TestUserAgent");

    VauClientProvider provider = new VauClientProvider(config);
    VauService service = provider.getVauService();

    assertNotNull(service);
  }
}
