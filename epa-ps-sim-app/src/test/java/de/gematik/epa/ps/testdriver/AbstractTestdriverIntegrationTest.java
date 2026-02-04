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
package de.gematik.epa.ps.testdriver;

import de.gematik.epa.api.psTestdriver.dto.Action;
import de.gematik.epa.ps.utils.AbstractIntegrationTest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

@Slf4j
@AutoConfigureRestTestClient
class AbstractTestdriverIntegrationTest extends AbstractIntegrationTest {

  protected static final String MY_HAPPY_LITTLE_KVNR = "1234567890";
  protected static final MediaType psDriverType =
      new MediaType("application", "gematik.psTestdriver.v0.1.0+json");

  @Autowired private RestTestClient restTestClient;
  @LocalServerPort private int port;

  public EntityExchangeResult<@NotNull Action> performTestdriverCall(String url, Object body) {
    return performTestdriverCall(url, body, Action.class, HttpMethod.POST);
  }

  @SneakyThrows
  public <T> EntityExchangeResult<@NotNull T> performTestdriverCall(
      String url, Object body, Class<T> type, HttpMethod method) {
    log(method, url, body);
    var mediaType = new MediaType("application", "gematik.psTestdriver.v0.1.0+json");
    if (body == null) {
      return restTestClient
          .method(method)
          .uri(url)
          .contentType(mediaType)
          .exchange()
          .returnResult(type);
    } else {
      return restTestClient
          .method(method)
          .uri(url)
          .contentType(mediaType)
          .body(body)
          .exchange()
          .returnResult(type);
    }
  }

  @SneakyThrows
  // TODO: How to add headers here?
  public <T> EntityExchangeResult<@NotNull T> performTestdriverCall(
      String url,
      Object body,
      Class<T> type,
      HttpMethod method,
      String headerKey,
      String headerValue) {
    log(method, url, body);
    if (body == null) {
      return restTestClient
          .method(method)
          .uri(url)
          .contentType(psDriverType)
          .header(headerKey, headerValue)
          .exchange()
          .returnResult(type);
    } else {
      return restTestClient
          .method(method)
          .uri(url)
          .contentType(psDriverType)
          .header(headerKey, headerValue)
          .body(body)
          .exchange()
          .returnResult(type);
    }
  }

  @SneakyThrows
  public <T> EntityExchangeResult<@NotNull T> performTestdriverCall(
      String url, Object body, ParameterizedTypeReference<T> typeReference, HttpMethod method) {
    log(method, url, body);
    if (body == null) {
      return restTestClient
          .method(method)
          .uri(url)
          .contentType(psDriverType)
          .exchange()
          .returnResult(typeReference);
    } else {
      return restTestClient
          .method(method)
          .uri(url)
          .contentType(psDriverType)
          .body(body)
          .exchange()
          .returnResult(typeReference);
    }
  }

  private void log(HttpMethod method, String url, Object body) {
    var hostPortUrl = "http://localhost:" + port + url;
    log.info("Performing {} call to {} with body {}", method, hostPortUrl, body);
  }
}
