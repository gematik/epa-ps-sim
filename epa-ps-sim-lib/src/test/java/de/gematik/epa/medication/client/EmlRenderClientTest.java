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
package de.gematik.epa.medication.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class EmlRenderClientTest {

  private static final String APPLICATION_FHIR_JSON = "application/fhir+json";
  private WebClient webClientMock;
  private EmlRenderClient emlRenderClient;

  private static Stream<Arguments> provideErrorScenarios() {
    return Stream.of(
        Arguments.of(409, "statusMismatch", "Status Mismatch", "statusMismatch: Status Mismatch"),
        Arguments.of(423, "locked", "Locked", "locked: Locked"));
  }

  private static Stream<Arguments> provideValidParameters() {
    return Stream.of(
        Arguments.of(
            "insurantId", UUID.randomUUID().toString(), "2023-01-01", 10, 0, APPLICATION_FHIR_JSON),
        Arguments.of(
            "insurantId",
            UUID.randomUUID().toString(),
            "2023-01-01",
            null,
            0,
            APPLICATION_FHIR_JSON),
        Arguments.of(
            "insurantId",
            UUID.randomUUID().toString(),
            "2023-01-01",
            10,
            null,
            APPLICATION_FHIR_JSON));
  }

  private static Stream<Arguments> provideInvalidParameters() {
    return Stream.of(
        Arguments.of(
            "insurantId",
            UUID.randomUUID().toString(),
            "invalid-date",
            10,
            0,
            "400, Bad Request",
            APPLICATION_FHIR_JSON),
        Arguments.of(
            "insurantId",
            UUID.randomUUID().toString(),
            "2023-01-01",
            -1,
            0,
            "400, Bad Request",
            APPLICATION_FHIR_JSON),
        Arguments.of(
            "insurantId",
            UUID.randomUUID().toString(),
            "2023-01-01",
            10,
            -1,
            "400, Bad Request",
            APPLICATION_FHIR_JSON));
  }

  @BeforeEach
  void setUp() {
    webClientMock = mock(WebClient.class);
    emlRenderClient =
        new EmlRenderClient(
            "http://localhost:8888",
            "/pdf",
            "/epa/medication/render/v1/emp/pdf",
            "/xhtml",
            "ps-sim",
            "/medication-list",
            "/$add-eml-entry",
            "/$cancel-eml-entry",
            "/$add-emp-entry",
            "/$update-emp-entry",
            "/$medication-plan-log",
            "/$link-emp") {

          @Override
          protected WebClient updateWebclient(String insurantId, String path) {
            return webClientMock;
          }
        };
  }

  @Test
  void shouldReturnEmlAsXhtml() {
    var mockResponse = mock(Response.class);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.get()).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(200);
    when(mockResponse.readEntity(String.class)).thenReturn("test");

    var result = emlRenderClient.getEmlAsXhtml("insurantId");

    assertThat(result.xhtml()).isEqualTo("test");
    verify(webClientMock, times(1)).get();
  }

  @Test
  void shouldReturnEmlAsPdf() {
    var mockResponse = mock(Response.class);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.get()).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(200);
    when(mockResponse.readEntity(byte[].class)).thenReturn(new byte[0]);

    var result = emlRenderClient.getEmlAsPdf("insurantId");

    assertThat(result.pdf()).isEmpty();
    verify(webClientMock, times(1)).get();
  }

  @Test
  void getEmlAsPdfReturnsNoEmlWhenExceptionIsThrown() {
    when(webClientMock.get()).thenThrow(new RuntimeException("error"));

    var result = emlRenderClient.getEmlAsPdf("insurantId");

    assertThat(result.pdf()).isNull();
    assertThat(result.errorMessage()).isNotBlank();
    assertThat(result.httpStatusCode()).isEqualTo(500);
  }

  @Test
  void getEmlAsPdfRelatedStatusCodeWhenWebApplicationExceptionIsThrown() {
    var webApplicationException = new WebApplicationException("error", 503);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.get()).thenThrow(webApplicationException);

    var result = emlRenderClient.getEmlAsPdf("insurantId");

    assertThat(result.httpStatusCode())
        .isEqualTo(webApplicationException.getResponse().getStatus());
    assertThat(result.errorMessage()).isEqualTo(webApplicationException.getMessage());
  }

  @Test
  void getEmlAsPdfNoInsurantIdShouldReturn403() {
    var responseMock = mock(Response.class);
    when(responseMock.getStatus()).thenReturn(403);
    when(responseMock.hasEntity()).thenReturn(true);
    when(responseMock.readEntity(String.class))
        .thenReturn("{\"errorCode\":\"403\", \"errorDetail\":\"Forbidden\"}");

    var webApplicationException = new WebApplicationException("403: Forbidden", responseMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.get()).thenThrow(webApplicationException);

    var result = emlRenderClient.getEmlAsPdf("null");

    assertThat(result.httpStatusCode())
        .isEqualTo(webApplicationException.getResponse().getStatus());
    assertThat(result.errorMessage()).isEqualTo(webApplicationException.getMessage());
  }

  @ParameterizedTest
  @MethodSource("provideErrorScenarios")
  void getEmlAsPdfShouldHandleErrors(
      int statusCode, String errorCode, String errorDetail, String expectedMessage) {
    var responseMock = mock(Response.class);
    when(responseMock.getStatus()).thenReturn(statusCode);
    when(responseMock.hasEntity()).thenReturn(true);
    when(responseMock.readEntity(String.class))
        .thenReturn(
            String.format(
                "{\"errorCode\":\"%s\", \"errorDetail\":\"%s\"}", errorCode, errorDetail));

    var webApplicationException = new WebApplicationException(expectedMessage, responseMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.get()).thenThrow(webApplicationException);

    var result = emlRenderClient.getEmlAsPdf("insurantId");

    assertThat(result.httpStatusCode()).isEqualTo(statusCode);
    assertThat(result.errorMessage()).isEqualTo(expectedMessage);
  }

  @Test
  void getEmlAsPdfWithErrorResponseShouldParseError() {
    var responseMock = mock(Response.class);
    when(responseMock.getStatus()).thenReturn(400);
    when(responseMock.hasEntity()).thenReturn(true);
    when(responseMock.readEntity(String.class))
        .thenReturn("{\"errorCode\":\"400\", \"errorDetail\":\"Bad Request\"}");

    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.get()).thenReturn(responseMock);

    var result = emlRenderClient.getEmlAsPdf("insurantId");

    assertThat(result.httpStatusCode()).isEqualTo(400);
    assertThat(result.errorMessage()).isEqualTo("400, Bad Request");
  }

  @Test
  void getEmlAsXhtmlReturnsRelatedStatusCodeWhenWebApplicationExceptionIsThrown() {
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    var webApplicationException = new WebApplicationException("error", 503);
    when(webClientMock.get()).thenThrow(webApplicationException);

    var result = emlRenderClient.getEmlAsXhtml("insurantId");

    assertThat(result.httpStatusCode())
        .isEqualTo(webApplicationException.getResponse().getStatus());
    assertThat(result.errorMessage()).isEqualTo(webApplicationException.getMessage());
  }

  @Test
  void getEmlAsXhtmlReturnsNoEmlWhenExceptionIsThrown() {
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.get()).thenThrow(new RuntimeException("error"));

    var result = emlRenderClient.getEmlAsXhtml("insurantId");

    assertThat(result.xhtml()).isNull();
    assertThat(result.errorMessage()).isNotBlank();
    assertThat(result.httpStatusCode()).isEqualTo(500);
  }

  @Test
  void getEmlAsXhtmlNoInsurantIdShouldReturn403() {
    var responseMock = mock(Response.class);
    when(responseMock.getStatus()).thenReturn(403);
    when(responseMock.hasEntity()).thenReturn(true);
    when(responseMock.readEntity(String.class))
        .thenReturn("{\"errorCode\":\"403\", \"errorDetail\":\"Forbidden\"}");

    var webApplicationException = new WebApplicationException("403: Forbidden", responseMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.get()).thenThrow(webApplicationException);

    var result = emlRenderClient.getEmlAsXhtml("null");

    assertThat(result.httpStatusCode())
        .isEqualTo(webApplicationException.getResponse().getStatus());
    assertThat(result.errorMessage()).isEqualTo(webApplicationException.getMessage());
  }

  @Test
  void getEmlAsXhtmlWithErrorResponseShouldParseError() {
    var responseMock = mock(Response.class);
    when(responseMock.getStatus()).thenReturn(400);
    when(responseMock.hasEntity()).thenReturn(true);
    when(responseMock.readEntity(String.class))
        .thenReturn("{\"errorCode\":\"400\", \"errorDetail\":\"Bad Request\"}");

    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.get()).thenReturn(responseMock);

    var result = emlRenderClient.getEmlAsXhtml("insurantId");

    assertThat(result.httpStatusCode()).isEqualTo(400);
    assertThat(result.errorMessage()).isEqualTo("400, Bad Request");
  }

  @ParameterizedTest
  @MethodSource("provideErrorScenarios")
  void getEmlAsXhtmlShouldHandleErrors(
      int statusCode, String errorCode, String errorDetail, String expectedMessage) {
    var responseMock = mock(Response.class);
    when(responseMock.getStatus()).thenReturn(statusCode);
    when(responseMock.hasEntity()).thenReturn(true);
    when(responseMock.readEntity(String.class))
        .thenReturn(
            String.format(
                "{\"errorCode\":\"%s\", \"errorDetail\":\"%s\"}", errorCode, errorDetail));

    var webApplicationException = new WebApplicationException(expectedMessage, responseMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.get()).thenThrow(webApplicationException);

    var result = emlRenderClient.getEmlAsXhtml("insurantId");

    assertThat(result.httpStatusCode()).isEqualTo(statusCode);
    assertThat(result.errorMessage()).isEqualTo(expectedMessage);
  }

  @Test
  void shouldCreateWebClient() {
    var result = emlRenderClient.updateWebclient("insurantId", "/pdf");

    assertThat(result).isNotNull();
  }

  @ParameterizedTest
  @MethodSource("provideValidParameters")
  void shouldGetMedicationListSuccessfully(
      String insurantId,
      String requestId,
      String date,
      Integer count,
      Integer offset,
      String format) {
    var mockResponse = mock(Response.class);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.get()).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(200);
    when(mockResponse.readEntity(String.class)).thenReturn("emlFhir");

    var result =
        emlRenderClient.getMedicationList(insurantId, requestId, date, count, offset, format);

    assertThat(result.httpStatusCode()).isEqualTo(200);
    assertThat(result.emlAsFhir()).isEqualTo("emlFhir");
    assertThat(result.errorMessage()).isBlank();
  }

  @ParameterizedTest
  @MethodSource("provideInvalidParameters")
  void shouldHandleInvalidParameters(
      String insurantId,
      String requestId,
      String date,
      Integer count,
      Integer offset,
      String format) {
    var mockResponse = mock(Response.class);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.get()).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(400);
    when(mockResponse.readEntity(String.class))
        .thenReturn("{\"errorCode\":\"400\", \"errorDetail\":\"Bad Request\"}");

    var result =
        emlRenderClient.getMedicationList(insurantId, requestId, date, count, offset, format);

    assertThat(result.httpStatusCode()).isEqualTo(400);
    assertThat(result.errorMessage()).isNotBlank();
  }

  @Test
  void shouldAddEmlEntrySuccessfully() {
    var mockResponse = mock(Response.class);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.post(anyString())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(200);
    when(mockResponse.readEntity(String.class)).thenReturn("success response");

    var result =
        emlRenderClient.addEmlEntry(
            "insurantId",
            UUID.randomUUID().toString(),
            "user-agent",
            "parameters",
            "encodedOrg",
            APPLICATION_FHIR_JSON);

    assertThat(result.httpStatusCode()).isEqualTo(200);
    assertThat(result.empResponse()).isEqualTo("success response");
    assertThat(result.errorMessage()).isBlank();
  }

  @Test
  void shouldAddEmlEntryWithNullFormat() {
    var mockResponse = mock(Response.class);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.post(anyString())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(200);
    when(mockResponse.readEntity(String.class)).thenReturn("success response");

    var result =
        emlRenderClient.addEmlEntry(
            "insurantId",
            UUID.randomUUID().toString(),
            "user-agent",
            "parameters",
            "encodedOrg",
            null);

    assertThat(result.httpStatusCode()).isEqualTo(200);
    assertThat(result.empResponse()).isEqualTo("success response");
  }

  @Test
  void shouldAddEmlEntryWithXmlFormat() {
    var mockResponse = mock(Response.class);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.post(anyString())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(200);
    when(mockResponse.readEntity(String.class)).thenReturn("success response");

    var result =
        emlRenderClient.addEmlEntry(
            "insurantId",
            UUID.randomUUID().toString(),
            "user-agent",
            "parameters",
            "encodedOrg",
            "application/fhir+xml");

    assertThat(result.httpStatusCode()).isEqualTo(200);
    assertThat(result.empResponse()).isEqualTo("success response");
  }

  @Test
  void addEmlEntryShouldHandleOperationOutcomeFor400() {
    var mockResponse = mock(Response.class);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.post(anyString())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(400);
    when(mockResponse.readEntity(String.class))
        .thenReturn("{\"issue\":[{\"severity\":\"error\",\"code\":\"invalid\"}]}");

    var result =
        emlRenderClient.addEmlEntry(
            "insurantId",
            UUID.randomUUID().toString(),
            "user-agent",
            "parameters",
            "encodedOrg",
            APPLICATION_FHIR_JSON);

    assertThat(result.httpStatusCode()).isEqualTo(400);
    assertThat(result.errorMessage()).isNotBlank();
  }

  @Test
  void addEmlEntryShouldHandleErrorResponseFor500() {
    var mockResponse = mock(Response.class);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.post(anyString())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(500);
    when(mockResponse.readEntity(String.class))
        .thenReturn("{\"errorCode\":\"500\", \"errorDetail\":\"Internal Server Error\"}");

    var result =
        emlRenderClient.addEmlEntry(
            "insurantId",
            UUID.randomUUID().toString(),
            "user-agent",
            "parameters",
            "encodedOrg",
            APPLICATION_FHIR_JSON);

    assertThat(result.httpStatusCode()).isEqualTo(500);
    assertThat(result.errorMessage()).isEqualTo("500, Internal Server Error");
  }

  @Test
  void addEmlEntryShouldHandleWebApplicationException() {
    var responseMock = mock(Response.class);
    when(responseMock.getStatus()).thenReturn(503);
    when(responseMock.hasEntity()).thenReturn(false);

    var webApplicationException = new WebApplicationException("Service unavailable", responseMock);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.post(anyString())).thenThrow(webApplicationException);

    var result =
        emlRenderClient.addEmlEntry(
            "insurantId",
            UUID.randomUUID().toString(),
            "user-agent",
            "parameters",
            "encodedOrg",
            APPLICATION_FHIR_JSON);

    assertThat(result.httpStatusCode()).isEqualTo(503);
    assertThat(result.errorMessage()).isEqualTo("Service unavailable");
  }

  @Test
  void addEmlEntryShouldHandleGeneralException() {
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.post(anyString())).thenThrow(new RuntimeException("Unexpected error"));

    var result =
        emlRenderClient.addEmlEntry(
            "insurantId",
            UUID.randomUUID().toString(),
            "user-agent",
            "parameters",
            "encodedOrg",
            APPLICATION_FHIR_JSON);

    assertThat(result.httpStatusCode()).isEqualTo(500);
    assertThat(result.errorMessage()).isEqualTo("Unexpected error");
  }

  @Test
  void shouldCancelEmlEntrySuccessfully() {
    var mockResponse = mock(Response.class);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.post(anyString())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(200);
    when(mockResponse.readEntity(String.class)).thenReturn("success response");

    var result =
        emlRenderClient.cancelEmlEntry(
            "insurantId",
            UUID.randomUUID().toString(),
            "encodedOrg",
            "medicationStatementId",
            "user-agent",
            APPLICATION_FHIR_JSON);

    assertThat(result.httpStatusCode()).isEqualTo(200);
    assertThat(result.empResponse()).isEqualTo("success response");
    assertThat(result.errorMessage()).isBlank();
  }

  @Test
  void shouldCancelEmlEntryWithNullFormat() {
    var mockResponse = mock(Response.class);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.post(anyString())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(200);
    when(mockResponse.readEntity(String.class)).thenReturn("success response");

    var result =
        emlRenderClient.cancelEmlEntry(
            "insurantId",
            UUID.randomUUID().toString(),
            "encodedOrg",
            "medicationStatementId",
            "user-agent",
            null);

    assertThat(result.httpStatusCode()).isEqualTo(200);
    assertThat(result.empResponse()).isEqualTo("success response");
  }

  @Test
  void shouldCancelEmlEntryWithXmlFormat() {
    var mockResponse = mock(Response.class);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.post(anyString())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(200);
    when(mockResponse.readEntity(String.class)).thenReturn("success response");

    var result =
        emlRenderClient.cancelEmlEntry(
            "insurantId",
            UUID.randomUUID().toString(),
            "encodedOrg",
            "medicationStatementId",
            "user-agent",
            "application/fhir+xml");

    assertThat(result.httpStatusCode()).isEqualTo(200);
    assertThat(result.empResponse()).isEqualTo("success response");
  }

  @Test
  void cancelEmlEntryShouldHandleOperationOutcomeFor400() {
    var mockResponse = mock(Response.class);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.post(anyString())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(400);
    when(mockResponse.readEntity(String.class))
        .thenReturn("{\"issue\":[{\"severity\":\"error\",\"code\":\"invalid\"}]}");

    var result =
        emlRenderClient.cancelEmlEntry(
            "insurantId",
            UUID.randomUUID().toString(),
            "encodedOrg",
            "medicationStatementId",
            "user-agent",
            APPLICATION_FHIR_JSON);

    assertThat(result.httpStatusCode()).isEqualTo(400);
    assertThat(result.errorMessage()).isNotBlank();
  }

  @Test
  void cancelEmlEntryShouldHandleErrorResponseFor500() {
    var mockResponse = mock(Response.class);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.post(anyString())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(500);
    when(mockResponse.readEntity(String.class))
        .thenReturn("{\"errorCode\":\"500\", \"errorDetail\":\"Internal Server Error\"}");

    var result =
        emlRenderClient.cancelEmlEntry(
            "insurantId",
            UUID.randomUUID().toString(),
            "encodedOrg",
            "medicationStatementId",
            "user-agent",
            APPLICATION_FHIR_JSON);

    assertThat(result.httpStatusCode()).isEqualTo(500);
    assertThat(result.errorMessage()).isEqualTo("500, Internal Server Error");
  }

  @Test
  void cancelEmlEntryShouldHandleWebApplicationException() {
    var responseMock = mock(Response.class);
    when(responseMock.getStatus()).thenReturn(503);
    when(responseMock.hasEntity()).thenReturn(false);

    var webApplicationException = new WebApplicationException("Service unavailable", responseMock);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.post(anyString())).thenThrow(webApplicationException);

    var result =
        emlRenderClient.cancelEmlEntry(
            "insurantId",
            UUID.randomUUID().toString(),
            "encodedOrg",
            "medicationStatementId",
            "user-agent",
            APPLICATION_FHIR_JSON);

    assertThat(result.httpStatusCode()).isEqualTo(503);
    assertThat(result.errorMessage()).isEqualTo("Service unavailable");
  }

  @Test
  void cancelEmlEntryShouldHandleGeneralException() {
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.post(anyString())).thenThrow(new RuntimeException("Unexpected error"));

    var result =
        emlRenderClient.cancelEmlEntry(
            "insurantId",
            UUID.randomUUID().toString(),
            "encodedOrg",
            "medicationStatementId",
            "user-agent",
            APPLICATION_FHIR_JSON);

    assertThat(result.httpStatusCode()).isEqualTo(500);
    assertThat(result.errorMessage()).isEqualTo("Unexpected error");
  }

  @Test
  void shouldAddEmpEntrySuccessfully() {
    var mockResponse = mock(Response.class);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.post(anyString())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(200);
    when(mockResponse.readEntity(String.class)).thenReturn("success response");

    var result =
        emlRenderClient.addEmpEntry(
            "insurantId",
            UUID.randomUUID().toString(),
            "user-agent",
            "parameters",
            "encodedOrg",
            APPLICATION_FHIR_JSON);

    assertThat(result.httpStatusCode()).isEqualTo(200);
    assertThat(result.empResponse()).isEqualTo("success response");
    assertThat(result.errorMessage()).isBlank();
  }

  @Test
  void shouldAddEmpEntryWithNullFormat() {
    var mockResponse = mock(Response.class);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.post(anyString())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(200);
    when(mockResponse.readEntity(String.class)).thenReturn("success response");

    var result =
        emlRenderClient.addEmpEntry(
            "insurantId",
            UUID.randomUUID().toString(),
            "user-agent",
            "parameters",
            "encodedOrg",
            null);

    assertThat(result.httpStatusCode()).isEqualTo(200);
    assertThat(result.empResponse()).isEqualTo("success response");
  }

  @Test
  void shouldAddEmpEntryWithXmlFormat() {
    var mockResponse = mock(Response.class);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.post(anyString())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(200);
    when(mockResponse.readEntity(String.class)).thenReturn("success response");

    var result =
        emlRenderClient.addEmpEntry(
            "insurantId",
            UUID.randomUUID().toString(),
            "user-agent",
            "parameters",
            "encodedOrg",
            "application/fhir+xml");

    assertThat(result.httpStatusCode()).isEqualTo(200);
    assertThat(result.empResponse()).isEqualTo("success response");
  }

  @Test
  void addEmpEntryShouldHandleOperationOutcomeFor400() {
    var mockResponse = mock(Response.class);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.post(anyString())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(400);
    when(mockResponse.readEntity(String.class))
        .thenReturn("{\"issue\":[{\"severity\":\"error\",\"code\":\"invalid\"}]}");

    var result =
        emlRenderClient.addEmpEntry(
            "insurantId",
            UUID.randomUUID().toString(),
            "user-agent",
            "parameters",
            "encodedOrg",
            APPLICATION_FHIR_JSON);

    assertThat(result.httpStatusCode()).isEqualTo(400);
    assertThat(result.errorMessage()).isNotBlank();
  }

  @Test
  void addEmpEntryShouldHandleErrorResponseFor500() {
    var mockResponse = mock(Response.class);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.post(anyString())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(500);
    when(mockResponse.readEntity(String.class))
        .thenReturn("{\"errorCode\":\"500\", \"errorDetail\":\"Internal Server Error\"}");

    var result =
        emlRenderClient.addEmpEntry(
            "insurantId",
            UUID.randomUUID().toString(),
            "user-agent",
            "parameters",
            "encodedOrg",
            APPLICATION_FHIR_JSON);

    assertThat(result.httpStatusCode()).isEqualTo(500);
    assertThat(result.errorMessage()).isEqualTo("500, Internal Server Error");
  }

  @Test
  void addEmpEntryShouldHandleWebApplicationException() {
    var responseMock = mock(Response.class);
    when(responseMock.getStatus()).thenReturn(503);
    when(responseMock.hasEntity()).thenReturn(false);

    var webApplicationException = new WebApplicationException("Service unavailable", responseMock);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.post(anyString())).thenThrow(webApplicationException);

    var result =
        emlRenderClient.addEmpEntry(
            "insurantId",
            UUID.randomUUID().toString(),
            "user-agent",
            "parameters",
            "encodedOrg",
            APPLICATION_FHIR_JSON);

    assertThat(result.httpStatusCode()).isEqualTo(503);
    assertThat(result.errorMessage()).isEqualTo("Service unavailable");
  }

  @Test
  void shouldReturnEmpAsPdf() {
    var mockResponse = mock(Response.class);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.get()).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(200);
    when(mockResponse.readEntity(byte[].class)).thenReturn(new byte[0]);

    var result = emlRenderClient.getEmpAsPdf("insurantId");

    assertThat(result.pdf()).isEmpty();
    verify(webClientMock, times(1)).get();
  }

  @Test
  void getEmpPdfShouldHandleNon200Status() {
    var mockResponse = mock(Response.class);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.get()).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(400);
    when(mockResponse.readEntity(String.class))
        .thenReturn("{\"errorCode\":\"400\", \"errorDetail\":\"Bad Request\"}");

    var result = emlRenderClient.getEmpAsPdf("insurantId");

    assertThat(result.httpStatusCode()).isNotEqualTo(200);
    assertThat(result.httpStatusCode()).isEqualTo(400);
    assertThat(result.errorMessage()).isEqualTo("400, Bad Request");
    assertThat(result.pdf()).isNull();
    assertThat(result.xhtml()).isNull();
    assertThat(result.emlAsFhir()).isNull();
    assertThat(result.empResponse()).isNull();

    verify(webClientMock, times(1)).get();
  }

  @Test
  void getEmpAsPdfThrowsWebApplicationException() {
    var webApplicationException = new WebApplicationException("Test Error Message", 503);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.get()).thenThrow(webApplicationException);

    var result = emlRenderClient.getEmpAsPdf("insurantId");

    assertThat(result.httpStatusCode())
        .isEqualTo(webApplicationException.getResponse().getStatus());
    assertThat(result.errorMessage()).isEqualTo(webApplicationException.getMessage());

    verify(webClientMock, times(1)).get();
  }

  @Test
  void getEmpAsPdfThrowsRuntimeException() {
    var runtimeException = new RuntimeException("Runtime Exception Message.");
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.get()).thenThrow(runtimeException);

    var result = emlRenderClient.getEmpAsPdf("insurantId");

    assertThat(result.httpStatusCode()).isEqualTo(500);
    assertThat(result.errorMessage()).isEqualTo(runtimeException.getMessage());

    verify(webClientMock, times(1)).get();
  }

  @Test
  void shouldReturnMedicationPlanLogSuccessfully() {
    var mockResponse = mock(Response.class);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.get()).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(200);
    var expectedResult = "{\"resourceType\":\"Bundle\"}";
    when(mockResponse.readEntity(String.class)).thenReturn(expectedResult);

    var result =
        emlRenderClient.getMedicationPlanLogs(
            "insurantId", UUID.randomUUID().toString(), 10, 0, APPLICATION_FHIR_JSON);

    assertThat(result.httpStatusCode()).isEqualTo(200);
    assertThat(result.medicationPlanLogs()).isEqualTo(expectedResult);
    assertThat(result.errorMessage()).isBlank();
  }

  @Test
  void getMedicationPlanLogShouldHandleErrorResponse() {
    var mockResponse = mock(Response.class);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.get()).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(403);
    when(mockResponse.readEntity(String.class))
        .thenReturn("{\"errorCode\":\"403\", \"errorDetail\":\"Internal Server Error\"}");

    var result =
        emlRenderClient.getMedicationPlanLogs(
            "insurantId", UUID.randomUUID().toString(), 10, 0, APPLICATION_FHIR_JSON);
    assertThat(result.httpStatusCode()).isEqualTo(403);
    assertThat(result.errorMessage()).isEqualTo("403, Internal Server Error");
  }

  @Test
  void shouldLinkEmp() {
    var mockResponse = mock(Response.class);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.post(anyString())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(200);
    when(mockResponse.readEntity(String.class)).thenReturn("linking success response");

    var result =
        emlRenderClient.linkEmp(
            "insurantId",
            UUID.randomUUID().toString(),
            "medicationStatementId",
            "user-agent",
            "parameters",
            "encodedOrg",
            APPLICATION_FHIR_JSON);

    assertThat(result.httpStatusCode()).isEqualTo(200);
    assertThat(result.empResponse()).isEqualTo("linking success response");
    assertThat(result.errorMessage()).isBlank();
  }

  @Test
  void linkEmpShouldHandleOperationOutcomeFor400() {
    var mockResponse = mock(Response.class);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.post(anyString())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(400);
    when(mockResponse.readEntity(String.class))
        .thenReturn("{\"issue\":[{\"severity\":\"error\",\"code\":\"invalid\"}]}");

    var result =
        emlRenderClient.linkEmp(
            "insurantId",
            UUID.randomUUID().toString(),
            "parameters",
            "encodedOrg",
            "medicationStatementId",
            "user-agent",
            APPLICATION_FHIR_JSON);

    assertThat(result.httpStatusCode()).isEqualTo(400);
    assertThat(result.errorMessage()).isNotBlank();
  }

  @Test
  void linkEmpShouldHandleErrorResponseFor500() {
    var mockResponse = mock(Response.class);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.post(anyString())).thenReturn(mockResponse);
    when(mockResponse.getStatus()).thenReturn(500);
    when(mockResponse.readEntity(String.class))
        .thenReturn("{\"errorCode\":\"500\", \"errorDetail\":\"Internal Server Error\"}");

    var result =
        emlRenderClient.linkEmp(
            "insurantId",
            UUID.randomUUID().toString(),
            "parameters",
            "encodedOrg",
            "medicationStatementId",
            "user-agent",
            APPLICATION_FHIR_JSON);

    assertThat(result.httpStatusCode()).isEqualTo(500);
    assertThat(result.errorMessage()).isEqualTo("500, Internal Server Error");
  }

  @Test
  void linkEmpShouldHandleWebApplicationException() {
    var responseMock = mock(Response.class);
    when(responseMock.getStatus()).thenReturn(503);
    when(responseMock.hasEntity()).thenReturn(false);

    var webApplicationException = new WebApplicationException("Service unavailable", responseMock);
    when(webClientMock.replaceHeader(anyString(), anyString())).thenReturn(webClientMock);
    when(webClientMock.replaceQueryParam(anyString(), any())).thenReturn(webClientMock);
    when(webClientMock.accept(anyString())).thenReturn(webClientMock);
    when(webClientMock.post(anyString())).thenThrow(webApplicationException);

    var result =
        emlRenderClient.linkEmp(
            "insurantId",
            UUID.randomUUID().toString(),
            "parameters",
            "encodedOrg",
            "medicationStatementId",
            "user-agent",
            APPLICATION_FHIR_JSON);

    assertThat(result.httpStatusCode()).isEqualTo(503);
    assertThat(result.errorMessage()).isEqualTo("Service unavailable");
  }
}
