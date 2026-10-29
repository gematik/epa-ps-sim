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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.epa.api.mhd.client.DocumentReferenceApi;
import de.gematik.epa.api.mhd.client.RetrieveDocumentApi;
import de.gematik.epa.api.mhd_dev.client.DevReferenceApi;
import de.gematik.epa.mhd.MhdService;
import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MhdClientProviderTest {

  private static final String SERVER_URL = "https://mhd.example.com:443";
  private static final String USER_AGENT = "TestAgent12345678901/1.0";

  private final MhdConfiguration mhdConfiguration = mock(MhdConfiguration.class);
  private final MhdClientProvider mhdClientProvider = new MhdClientProvider(mhdConfiguration);

  @BeforeEach
  void setup() {
    when(mhdConfiguration.getServerUrl()).thenReturn(SERVER_URL);
    when(mhdConfiguration.getUserAgent()).thenReturn(USER_AGENT);
  }

  @Test
  void shouldCreateMhdService() {
    var service = mhdClientProvider.mhdService();
    assertThat(service).isNotNull().isInstanceOf(MhdService.class);
  }

  @Test
  void shouldCreateMhdServiceWithCorrectUserAgent() {
    var service = mhdClientProvider.mhdService();
    assertThat(service.useragent()).isEqualTo(USER_AGENT);
  }

  @Test
  void shouldCreateDocumentReferenceApiClient() {
    var service = mhdClientProvider.mhdService();
    assertThat(service.documentReferenceApi()).isNotNull();
    assertThat(service.documentReferenceApi().getServiceApi())
        .isInstanceOf(DocumentReferenceApi.class);
  }

  @Test
  void shouldCreateRetrieveDocumentApiClient() {
    var service = mhdClientProvider.mhdService();
    assertThat(service.retrieveDocumentApi()).isNotNull();
    assertThat(service.retrieveDocumentApi().getServiceApi())
        .isInstanceOf(RetrieveDocumentApi.class);
  }

  @Test
  void shouldCreateDevReferenceApiClient() {
    var service = mhdClientProvider.mhdService();
    assertThat(service.devReferenceApi()).isNotNull();
    assertThat(service.devReferenceApi().getServiceApi()).isInstanceOf(DevReferenceApi.class);
  }

  @Test
  void shouldSetFormUrlEncodedContentTypeOnDocRefClient() {
    var service = mhdClientProvider.mhdService();
    var contentType =
        WebClient.client(service.documentReferenceApi().getServiceApi())
            .getHeaders()
            .getFirst("Content-Type");
    assertThat(contentType).isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
  }
}
