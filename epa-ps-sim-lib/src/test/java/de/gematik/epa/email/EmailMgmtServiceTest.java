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
package de.gematik.epa.email;

import static de.gematik.epa.unit.util.TestDataFactory.readEntity;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import de.gematik.epa.api.email.client.EmailManagementApi;
import de.gematik.epa.api.email.client.dto.EmailRequestType;
import de.gematik.epa.api.email.client.dto.EmailResponseType;
import de.gematik.epa.api.email.client.dto.ErrorType;
import de.gematik.epa.api.testdriver.email.dto.GetEmailAddressResponseDTO;
import de.gematik.epa.api.testdriver.email.dto.PutEmailRequestDTO;
import de.gematik.epa.api.testdriver.email.dto.PutEmailResponseDTO;
import de.gematik.epa.client.JaxRsClientWrapper;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmailMgmtServiceTest {

  private final JaxRsClientWrapper<EmailManagementApi> emailClient = mock(JaxRsClientWrapper.class);
  private final EmailManagementApi emailManagementApi = mock(EmailManagementApi.class);
  private final EmailMgmtService emailMgmtService = new EmailMgmtService(emailClient);

  @BeforeAll
  void setUp() {
    when(emailClient.getServiceApi()).thenReturn(emailManagementApi);
    when(emailClient.getUserAgent()).thenReturn("Test agent");
  }

  @Test
  void getEmailAddressShouldReturn200Success() {
    String email = "test@email.de";
    String actor = "Test actor";
    String createdAt = "2021-08-01T12:00Z";
    EmailResponseType emailResponseType = new EmailResponseType();
    emailResponseType.setEmail(email);
    emailResponseType.setActor(actor);
    emailResponseType.setCreatedAt(OffsetDateTime.parse(createdAt));

    Response response = mock(Response.class);
    when(response.getStatus()).thenReturn(200);
    when(response.readEntity(EmailResponseType.class)).thenReturn(emailResponseType);
    when(emailManagementApi.getEmailAddress(anyString(), anyString())).thenReturn(response);

    GetEmailAddressResponseDTO result = emailMgmtService.getEmailAddress("insurantId");
    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getEmail()).isEqualTo(email);
    assertThat(result.getActor()).isEqualTo(actor);
    assertThat(result.getCreatedAt()).isEqualTo(createdAt);
  }

  static Stream<Object[]> provideErrorCases() {
    return Stream.of(
        new Object[] {400, "malformedRequest"},
        new Object[] {403, "invalidOid"},
        new Object[] {404, "noResource"},
        new Object[] {409, "requestMismatch"},
        new Object[] {500, "internalError"});
  }

  @ParameterizedTest
  @MethodSource("provideErrorCases")
  void getEmailAddressShouldReturnError(int statusCode, String errorCode) {
    ErrorType errorType = new ErrorType();
    errorType.setErrorCode(errorCode);

    Response response = mock(Response.class);
    when(response.getStatus()).thenReturn(statusCode);
    when(response.hasEntity()).thenReturn(true);
    when(response.readEntity(ErrorType.class)).thenReturn(errorType);
    when(emailManagementApi.getEmailAddress(anyString(), anyString())).thenReturn(response);

    GetEmailAddressResponseDTO result = emailMgmtService.getEmailAddress("insurantId");
    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).isEqualTo(errorCode);
  }

  @Test
  void getEmailAddressShouldReturnUnknownErrorForException() {
    when(emailManagementApi.getEmailAddress(anyString(), anyString()))
        .thenThrow(new RuntimeException("Test exception"));
    GetEmailAddressResponseDTO result = emailMgmtService.getEmailAddress("insurantId");
    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).isEqualTo("Unknown error");
  }

  @Test
  void getEmailAddressShouldReturnUnknownErrorForUnhandledStatus() {
    Response response = mock(Response.class);
    when(response.getStatus()).thenReturn(418); // Unhandled status code
    when(emailManagementApi.getEmailAddress(anyString(), anyString())).thenReturn(response);

    GetEmailAddressResponseDTO result = emailMgmtService.getEmailAddress("insurantId");
    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).isEqualTo("Unknown error");
  }

  @Test
  void replaceEmailAddressShouldReturn200Success() {
    String actor = "Test actor";
    String createdAt = "2021-08-01T12:00Z";
    PutEmailRequestDTO requestDTO = new PutEmailRequestDTO();
    String newEmail = "new@email.de";
    requestDTO.setEmail(newEmail);

    EmailResponseType emailResponseType = new EmailResponseType();
    emailResponseType.setEmail(newEmail);
    emailResponseType.setActor(actor);
    emailResponseType.setCreatedAt(OffsetDateTime.parse(createdAt));

    Response response = mock(Response.class);
    when(response.getStatus()).thenReturn(200);
    when(emailManagementApi.replaceEmailAddress(
            anyString(), any(EmailRequestType.class), anyString()))
        .thenReturn(response);
    System.out.println("Response: " + response);
    when(response.readEntity(EmailResponseType.class)).thenReturn(emailResponseType);

    PutEmailResponseDTO result = emailMgmtService.replaceEmail("insurantId", requestDTO);
    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getEmail()).isEqualTo(newEmail);
    assertThat(result.getActor()).isEqualTo(actor);
    assertThat(result.getCreatedAt()).isEqualTo(createdAt);
  }

  @Test
  void replaceEmailAddressShouldReturn400() {
    var errorType = new ErrorType();
    var errorCode = "malformedRequest";
    errorType.setErrorCode(errorCode);

    var response = simulateInbound(Response.status(400).entity(errorType).build());
    when(emailManagementApi.replaceEmailAddress(
            anyString(), any(EmailRequestType.class), anyString()))
        .thenReturn(response);

    var result = emailMgmtService.replaceEmail("insurant", new PutEmailRequestDTO());
    assertThat(result.getStatusMessage()).isEqualTo(errorCode);
  }

  @Test
  void replaceEmailAddressInsurantIdMissingShouldReturn403() {
    var errorType = new ErrorType();
    var errorCode = "invalidParam";
    errorType.setErrorCode(errorCode);

    var response = simulateInbound(Response.status(403).entity(errorType).build());
    when(emailManagementApi.replaceEmailAddress(
            anyString(), any(EmailRequestType.class), ArgumentMatchers.isNull()))
        .thenReturn(response);

    var result = emailMgmtService.replaceEmail(null, new PutEmailRequestDTO());
    assertThat(result.getStatusMessage()).isEqualTo(errorCode);
  }

  public static Response simulateInbound(Response asOutbound) {
    Response toReturn = spy(asOutbound);
    doAnswer(answer((Class<?> type) -> readEntity(toReturn, type)))
        .when(toReturn)
        .readEntity(ArgumentMatchers.<Class<?>>any());
    return toReturn;
  }

  @Test
  void replaceEmailAddressShouldReturnUnknownErrorForException() {
    when(emailManagementApi.replaceEmailAddress(
            anyString(), any(EmailRequestType.class), anyString()))
        .thenThrow(new RuntimeException("I am expected exception"));
    PutEmailResponseDTO result =
        emailMgmtService.replaceEmail("insurantId", new PutEmailRequestDTO());
    assertThat(result).isNotNull();
    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage())
        .isEqualTo("Error while replacing email: I am expected exception");
  }
}
