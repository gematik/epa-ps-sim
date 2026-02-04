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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static de.gematik.epa.unit.util.TestDataFactory.CONTENT_TYPE_HEADER;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.epa.ps.endpoint.VauApiEndpoint;
import de.gematik.epa.ps.utils.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class VauApiEndpointIntegrationTest extends AbstractIntegrationTest {

  @Autowired VauApiEndpoint vauApiEndpoint;

  @Test
  void contextLoads() {
    assertThat(vauApiEndpoint).isNotNull();
  }

  @Test
  void shouldDestroyVauCid() {
    mockVauProxyServer.stubFor(
        post("/destroy")
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE_HEADER, "application/json")
                    .withBody(
                        """
                    {
                      "statusMessage": "VAU identity destroyed: http://localhost:8080/1742394111252"
                    }
                    """)));

    var responseDTO = vauApiEndpoint.destroyVauCid("2345678");
    assertThat(responseDTO).isNotNull();
    assertThat(responseDTO.getSuccess()).isTrue();
    assertThat(responseDTO.getStatusMessage()).isNotBlank();
  }

  @Test
  void destroyShouldReturn404ForNotExistingVauCid() {
    mockVauProxyServer.stubFor(
        post("/destroy")
            .willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader(CONTENT_TYPE_HEADER, "application/json")
                    .withBody(
                        """
                                        {
                                          "statusMessage": "VAU identity not found"
                                        }
                                        """)));

    var responseDTO = vauApiEndpoint.destroyVauCid("2345678");
    assertThat(responseDTO).isNotNull();
    assertThat(responseDTO.getSuccess()).isFalse();
    assertThat(responseDTO.getStatusMessage()).isNotBlank();
  }
}
