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
package de.gematik.epa.ps.testdriver;

import de.gematik.epa.api.psTestdriver.dto.Action;
import de.gematik.epa.ps.utils.AbstractIntegrationTest;
import java.net.URI;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

@Slf4j
class AbstractTestdriverIntegrationTest extends AbstractIntegrationTest {

  protected static final String MY_HAPPY_LITTLE_KVNR = "1234567890";

  @Autowired private TestRestTemplate testRestTemplate;
  @LocalServerPort private int port;

  public ResponseEntity<Action> performTestdriverCall(String url, Object body) {
    return performTestdriverCall(url, body, Action.class, HttpMethod.POST);
  }

  @SneakyThrows
  public <T> ResponseEntity<T> performTestdriverCall(
      String url, Object body, Class<T> type, HttpMethod method) {
    log(method, url, body);
    final HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(new MediaType("application", "gematik.psTestdriver.v0.1.0+json"));
    return testRestTemplate.exchange(
        new RequestEntity<>(body, httpHeaders, method, new URI(url)), type);
  }

  @SneakyThrows
  public <T> ResponseEntity<T> performTestdriverCall(
      String url, Object body, Class<T> type, HttpMethod method, HttpHeaders headers) {
    log(method, url, body);
    headers.setContentType(new MediaType("application", "gematik.psTestdriver.v0.1.0+json"));
    return testRestTemplate.exchange(
        new RequestEntity<>(body, headers, method, new URI(url)), type);
  }

  @SneakyThrows
  public <T> ResponseEntity<T> performTestdriverCall(
      String url, Object body, ParameterizedTypeReference<T> typeReference, HttpMethod method) {
    log(method, url, body);
    final HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(new MediaType("application", "gematik.psTestdriver.v0.1.0+json"));
    return testRestTemplate.exchange(
        new RequestEntity<>(body, httpHeaders, method, new URI(url)), typeReference);
  }

  private void log(HttpMethod method, String url, Object body) {
    var hostPortUrl = "http://localhost:" + port + url;
    log.info("Performing {} call to {} with body {}", method, hostPortUrl, body);
  }
}
