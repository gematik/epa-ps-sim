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
package de.gematik.epa.audit.client;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuditRenderClientTest {

  private WebClient webClientMock;
  private AuditRenderClient auditRenderClient;

  @BeforeEach
  void setUp() {
    webClientMock = mock(WebClient.class);
    auditRenderClient =
        new AuditRenderClient("http://localhost:8888", "/pdfA", "ps-sim") {

          @Override
          protected WebClient updateWebclient(String insurantId, Boolean signed, String path) {
            return webClientMock;
          }
        };
  }

  @Test
  void shouldCreateWebClient() {
    var result = auditRenderClient.updateWebclient("insurantId", true, "/pdfA");
    Assertions.assertThat(result).isNotNull();
  }

  @Test
  void getAuditEventsAsPdfAShouldReturnAuditEventAsPdfA() {
    var mockResponse = mock(Response.class);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.get()).thenReturn(mockResponse);
    when(mockResponse.readEntity(byte[].class)).thenReturn(new byte[0]);

    var result = auditRenderClient.getAuditEventAsPdfA("X12345678", true);

    assertThat(result.pdf()).isEmpty();
    verify(webClientMock, times(1)).get();
  }

  @Test
  void getAuditEventsAsPdfAShouldReturnError() {
    when(webClientMock.get()).thenThrow(new RuntimeException("error"));
    var result = auditRenderClient.getAuditEventAsPdfA("X12345678", true);
    Assertions.assertThat(result.pdf()).isNull();
    Assertions.assertThat(result.errorMessage()).isNotBlank();
    Assertions.assertThat(result.httpStatusCode()).isEqualTo(500);
  }

  @Test
  void getAuditEventsAsPdfAReturnsNoAuditEventsWhenExceptionIsThrown() {
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.get()).thenThrow(new RuntimeException("error"));

    var result = auditRenderClient.getAuditEventAsPdfA("insurantId", false);

    Assertions.assertThat(result.pdf()).isNull();
    Assertions.assertThat(result.errorMessage()).isNotBlank();
    Assertions.assertThat(result.httpStatusCode()).isEqualTo(500);
  }

  @Test
  void getAuditEventsAsPdfAReturnsRelatedStatusCodeWhenWebApplicationExceptionIsThrown() {
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    var webApplicationException = new WebApplicationException("error", 400);
    when(webClientMock.get()).thenThrow(webApplicationException);

    var result = auditRenderClient.getAuditEventAsPdfA("insurantId", true);

    Assertions.assertThat(result.httpStatusCode())
        .isEqualTo(webApplicationException.getResponse().getStatus());
    Assertions.assertThat(result.errorMessage()).isEqualTo(webApplicationException.getMessage());
  }
}
