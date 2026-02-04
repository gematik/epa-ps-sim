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
package de.gematik.epa.authentication;

import static de.gematik.epa.authentication.AuthenticationService.ERROR_IN_LOGIN_PROCESS_NO_ERROR_MESSAGE_AVAILABLE;
import static de.gematik.epa.authentication.AuthenticationService.ERROR_WHILE_GETTING_AUTHORIZATION_CODE_FROM_IDP;
import static de.gematik.epa.authentication.AuthenticationService.ERROR_WHILE_PARSING_REDIRECT_URL;
import static de.gematik.epa.unit.util.TestDataFactory.simulateInbound;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.epa.api.authorization.client.AuthorizationSmcBApi;
import de.gematik.epa.api.authorization.client.dto.ErrorType;
import de.gematik.epa.api.authorization.client.dto.GetNonce200Response;
import de.gematik.epa.api.authorization.client.dto.SendAuthCodeSC200Response;
import de.gematik.epa.api.vau.client.VauApi;
import de.gematik.epa.api.vau.client.dto.VauStatus;
import de.gematik.epa.authentication.exception.TelematikIdNotFoundException;
import de.gematik.epa.client.JaxRsClientWrapper;
import de.gematik.epa.konnektor.CardAuthenticationService;
import de.gematik.epa.konnektor.KonnektorContextProvider;
import de.gematik.epa.konnektor.KonnektorInterfaceAssembly;
import de.gematik.epa.konnektor.SmbInformationProvider;
import de.gematik.epa.unit.util.ResourceLoader;
import de.gematik.epa.unit.util.TestDataFactory;
import de.gematik.epa.utils.CertificateUtils;
import de.gematik.idp.client.AuthorizationCodeResult;
import de.gematik.idp.client.IdpClient;
import de.gematik.idp.client.IdpClientRuntimeException;
import jakarta.ws.rs.core.Response;
import java.security.cert.X509Certificate;
import java.util.function.UnaryOperator;
import lombok.SneakyThrows;
import oasis.names.tc.dss._1_0.core.schema.Base64Signature;
import oasis.names.tc.dss._1_0.core.schema.SignatureObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import telematik.ws.conn.signatureservice.xsd.v7_4.ExternalAuthenticateResponse;

class AuthenticationServiceTest {

  private final AuthorizationSmcBApi authorizationSmcBApi = mock(AuthorizationSmcBApi.class);
  private final IdpClient.IdpClientBuilder idpClientBuilder =
      mock(IdpClient.IdpClientBuilder.class);
  private final JaxRsClientWrapper<VauApi> vauApiJaxRsClientWrapper =
      mock(JaxRsClientWrapper.class);
  private final CardAuthenticationService cardAuthenticationService =
      mock(CardAuthenticationService.class);
  private final String redirectUrl =
      "https://localhost:8082/authz?client_id=ePA&response_type=code&redirect_uri=http%3A%2F%2Ftest-ps.gematik.de%2FePA&state=ABCVDFGHT564&code_challenge=IMevy8z9hyfFsDad6JM0E20MQ_jLInJ6GtE55sGyH3Y&code_challenge_method=S256&scope=openid%20epa&nonce=7721435277f5d0137b17ef8b835ca03cf09dc23926aa1766e4f8132433ff37d6";
  private final String userAgent = "PS_SIM_123";
  private final VauApi vauApiMock = mock(VauApi.class);
  private AuthenticationService authenticationService;

  @BeforeEach
  void setUp() {
    final KonnektorContextProvider konnektorContextProvider =
        TestDataFactory.konnektorContextProvider();
    final KonnektorInterfaceAssembly konnektorInterfaceAssembly =
        TestDataFactory.konnektorInterfaceAssemblyMock();
    final SmbInformationProvider smbInformationProvider =
        new SmbInformationProvider(konnektorContextProvider, konnektorInterfaceAssembly);

    authenticationService =
        new AuthenticationService(
            authorizationSmcBApi,
            idpClientBuilder,
            konnektorContextProvider,
            konnektorInterfaceAssembly,
            smbInformationProvider,
            vauApiJaxRsClientWrapper,
            userAgent);
    when(vauApiJaxRsClientWrapper.getServiceApi()).thenReturn(vauApiMock);

    authenticationService.setCardAuthenticationService(cardAuthenticationService);
  }

  @Test
  @SneakyThrows
  void shouldExtractQueryParams() {
    final var queryParams = authenticationService.getQueryParams(redirectUrl);

    assertThat(queryParams).isNotNull();
    assertThat(queryParams).containsEntry("client_id", "ePA");
    assertThat(queryParams).containsEntry("redirect_uri", "http://test-ps.gematik.de/ePA");
    assertThat(queryParams).containsEntry("scope", "openid epa");
  }

  @Test
  void loginShouldFailWhenNonceRequestFails() {
    getMockVauStatusResponseAuthenticationNone();
    // given
    final var errorInNonce = new ErrorType().errorDetail("error in nonce");
    final var nonceResponse = Response.status(500).entity(errorInNonce).build();
    when(authorizationSmcBApi.getNonce(anyString())).thenReturn(nonceResponse);

    // when
    final var result = authenticationService.login("2-883110000118994", "FQDN");

    // then
    assertThat(result.success()).isFalse();
    assertThat(result.httpStatusCode()).isEqualTo(500);
    assertThat(result.errorMessage()).contains(errorInNonce.getErrorDetail());
  }

  @Test
  void loginShouldFailWhenNonceRequestFailsUnexpected() {
    getMockVauStatusResponseAuthenticationNone();
    // given
    when(authorizationSmcBApi.getNonce(anyString())).thenThrow(IllegalStateException.class);

    // when
    final var result = authenticationService.login("2-883110000118994", "FQDN");

    // then
    assertThat(result.success()).isFalse();
    assertThat(result.errorMessage()).isNotBlank();
  }

  @Test
  void loginShouldFailWhenAuthorizationRequestFails() {
    getMockVauStatusResponseAuthenticationNone();
    // given
    final var nonce = "7721435277f5d0137b17ef8b835ca03cf09dc23926aa1766e4f8132433ff37d6";

    mockNonceHandle(nonce);

    final var error = new ErrorType().errorCode("authorization_failure");
    final var authorizationResponse = Response.status(403).entity(error).build();
    when(authorizationSmcBApi.sendAuthorizationRequestSC(anyString()))
        .thenReturn(authorizationResponse);
    when(cardAuthenticationService.getCardHandle(anyString())).thenReturn("cardHandle");

    // when
    final var result = authenticationService.login("2-883110000118994", "FQDN");

    // then
    assertThat(result.success()).isFalse();
    assertThat(result.httpStatusCode()).isEqualTo(403);
    assertThat(result.errorMessage()).contains(error.getErrorCode());
  }

  @Test
  void loginShouldFailWhenAuthzServerReturnUnexpectedStatusCode() {
    getMockVauStatusResponseAuthenticationNone();
    // given
    final var nonceResponse = Response.status(404).build();
    when(authorizationSmcBApi.getNonce(anyString())).thenReturn(nonceResponse);

    // when
    final var result = authenticationService.login("2-883110000118994", "FQDN");

    // then
    assertThat(result.success()).isFalse();
    assertThat(result.httpStatusCode()).isEqualTo(404);
    assertThat(result.errorMessage()).contains(ERROR_IN_LOGIN_PROCESS_NO_ERROR_MESSAGE_AVAILABLE);
  }

  @Test
  void loginShouldFailWhenRedirectUrlIsInvalid() {
    getMockVauStatusResponseAuthenticationNone();
    // given
    final var nonce = "7721435277f5d0137b17ef8b835ca03cf09dc23926aa1766e4f8132433ff37d6";
    mockNonceHandle(nonce);

    final var authorizationResponse =
        Response.status(302).header("Location", "http://bla.de/bla").build();
    when(authorizationSmcBApi.sendAuthorizationRequestSC(anyString()))
        .thenReturn(authorizationResponse);
    when(cardAuthenticationService.getCardHandle(anyString())).thenReturn("cardHandle");

    // when
    final var result = authenticationService.login("2-883110000118994", "FQDN");

    // then
    assertThat(result.success()).isFalse();
    assertThat(result.errorMessage()).contains(ERROR_WHILE_PARSING_REDIRECT_URL);
  }

  @Test
  void loginShouldFailWhenIdpThrowsException() {
    getMockVauStatusResponseAuthenticationNone();
    final AuthenticationService authenticationServiceSpy = Mockito.spy(authenticationService);

    // given
    final var nonce = "7721435277f5d0137b17ef8b835ca03cf09dc23926aa1766e4f8132433ff37d6";

    mockNonceHandle(nonce);

    final var authorizationResponse = Response.status(302).header("Location", redirectUrl).build();
    when(authorizationSmcBApi.sendAuthorizationRequestSC(anyString()))
        .thenReturn(authorizationResponse);

    final var idpClientBuilder = mock(IdpClient.IdpClientBuilder.class);
    when(idpClientBuilder.scopes(anySet())).thenReturn(idpClientBuilder);
    when(idpClientBuilder.redirectUrl(anyString())).thenReturn(idpClientBuilder);
    when(idpClientBuilder.codeChallengeMethod(any())).thenReturn(idpClientBuilder);
    when(idpClientBuilder.clientId(anyString())).thenReturn(idpClientBuilder);

    final var idpClient = mock(IdpClient.class);
    when(idpClientBuilder.build()).thenReturn(idpClient);

    final var cardHandle = "123";
    final var telematikId = "2-883110000118994";

    when(cardAuthenticationService.getCardHandle(telematikId)).thenReturn(cardHandle);
    final UnaryOperator<byte[]> mockedContentSigner = TestDataFactory.getContentSigner(cardHandle);
    doReturn(mockedContentSigner).when(cardAuthenticationService).getContentSigner(cardHandle);

    final X509Certificate x509Certificate =
        CertificateUtils.toX509Certificate(ResourceLoader.autCertificateAsByteArray());
    when(cardAuthenticationService.getX509Certificate(any())).thenReturn(x509Certificate);

    when(idpClient.login(
            x509Certificate,
            mockedContentSigner,
            "IMevy8z9hyfFsDad6JM0E20MQ_jLInJ6GtE55sGyH3Y",
            "ABCVDFGHT564",
            nonce))
        .thenThrow(new IdpClientRuntimeException("error for login"));

    // when
    final var result = authenticationServiceSpy.login(telematikId, "FQDN");

    // then
    assertThat(result.success()).isFalse();
    assertThat(result.errorMessage()).isNotBlank();
  }

  @Test
  void loginShouldFailWhenIdpLoginFails() {
    getMockVauStatusResponseAuthenticationNone();
    final AuthenticationService authenticationServiceSpy = Mockito.spy(authenticationService);

    // given
    final var nonce = "7721435277f5d0137b17ef8b835ca03cf09dc23926aa1766e4f8132433ff37d6";
    final var telematikId = "2-883110000118994";

    mockNonceHandle(nonce);

    final var authorizationResponse = Response.status(302).header("Location", redirectUrl).build();
    when(authorizationSmcBApi.sendAuthorizationRequestSC(anyString()))
        .thenReturn(authorizationResponse);

    when(idpClientBuilder.scopes(anySet())).thenReturn(idpClientBuilder);
    when(idpClientBuilder.redirectUrl(anyString())).thenReturn(idpClientBuilder);
    when(idpClientBuilder.codeChallengeMethod(any())).thenReturn(idpClientBuilder);
    when(idpClientBuilder.clientId(anyString())).thenReturn(idpClientBuilder);

    final var idpClient = mock(IdpClient.class);
    when(idpClientBuilder.build()).thenReturn(idpClient);

    final var cardHandle = "123";
    when(cardAuthenticationService.getCardHandle(telematikId)).thenReturn(cardHandle);

    final X509Certificate x509Certificate =
        CertificateUtils.toX509Certificate(ResourceLoader.autCertificateAsByteArray());
    when(cardAuthenticationService.getX509Certificate(any())).thenReturn(x509Certificate);

    final UnaryOperator<byte[]> mockedContentSigner = TestDataFactory.getContentSigner(cardHandle);
    doReturn(mockedContentSigner).when(cardAuthenticationService).getContentSigner(cardHandle);

    when(idpClient.login(
            x509Certificate,
            mockedContentSigner,
            "IMevy8z9hyfFsDad6JM0E20MQ_jLInJ6GtE55sGyH3Y",
            "ABCVDFGHT564",
            nonce))
        .thenReturn(new AuthorizationCodeResult());

    // when
    final var result = authenticationServiceSpy.login(telematikId, "FQDN");

    // then
    assertThat(result.success()).isFalse();
    assertThat(result.errorMessage()).contains(ERROR_WHILE_GETTING_AUTHORIZATION_CODE_FROM_IDP);
  }

  @Test
  @SneakyThrows
  void shouldLogin() {
    getMockVauStatusResponseAuthenticationNone();
    final AuthenticationService authenticationServiceSpy = Mockito.spy(authenticationService);

    // given
    final var nonce = "7721435277f5d0137b17ef8b835ca03cf09dc23926aa1766e4f8132433ff37d6";
    mockNonceHandle(nonce);

    final var authorizationResponse = Response.status(302).header("Location", redirectUrl).build();
    when(authorizationSmcBApi.sendAuthorizationRequestSC(anyString()))
        .thenReturn(authorizationResponse);

    when(authorizationSmcBApi.sendAuthorizationRequestSC(anyString()))
        .thenReturn(authorizationResponse);

    when(idpClientBuilder.scopes(anySet())).thenReturn(idpClientBuilder);
    when(idpClientBuilder.redirectUrl(anyString())).thenReturn(idpClientBuilder);
    when(idpClientBuilder.codeChallengeMethod(any())).thenReturn(idpClientBuilder);
    when(idpClientBuilder.clientId(anyString())).thenReturn(idpClientBuilder);

    final var idpClient = mock(IdpClient.class);
    when(idpClientBuilder.build()).thenReturn(idpClient);

    final var cardHandle = "123";
    final var telematikId = "2-883110000118994";
    when(cardAuthenticationService.getCardHandle(telematikId)).thenReturn(cardHandle);
    final UnaryOperator<byte[]> mockedContentSigner = TestDataFactory.getContentSigner(cardHandle);
    doReturn(mockedContentSigner).when(cardAuthenticationService).getContentSigner(cardHandle);

    final AuthorizationCodeResult authorizationCodeResult =
        new AuthorizationCodeResult("code", "state", "redirectUri");

    final X509Certificate x509Certificate =
        CertificateUtils.toX509Certificate(ResourceLoader.autCertificateAsByteArray());
    when(cardAuthenticationService.getX509Certificate(any())).thenReturn(x509Certificate);
    when(cardAuthenticationService.createSignedJwt(any(), any(), any(), anyString()))
        .thenReturn("jwt");
    when(cardAuthenticationService.determineAlgorithm(any())).thenReturn("algorithm");

    when(idpClient.login(
            x509Certificate,
            mockedContentSigner,
            "IMevy8z9hyfFsDad6JM0E20MQ_jLInJ6GtE55sGyH3Y",
            "ABCVDFGHT564",
            nonce))
        .thenReturn(authorizationCodeResult);

    final var authCodeScResponse =
        simulateInbound(
            Response.status(200).entity(new SendAuthCodeSC200Response().vauNp("vau-np")).build());

    when(authorizationSmcBApi.sendAuthCodeSC(any(), any())).thenReturn(authCodeScResponse);

    // when
    final var result = authenticationServiceSpy.login(telematikId, "FQDN");

    // then
    assertThat(result.success()).isTrue();
    assertThat(result.httpStatusCode()).isEqualTo(200);
    assertThat(result.errorMessage()).isNull();
  }

  @Test
  void loginShouldFailWhenSendAuthCodeFails() {
    getMockVauStatusResponseAuthenticationNone();
    final AuthenticationService authenticationServiceSpy = Mockito.spy(authenticationService);

    // given
    final var nonce = "7721435277f5d0137b17ef8b835ca03cf09dc23926aa1766e4f8132433ff37d6";
    mockNonceHandle(nonce);

    final var authorizationResponse = Response.status(302).header("Location", redirectUrl).build();
    when(authorizationSmcBApi.sendAuthorizationRequestSC(anyString()))
        .thenReturn(authorizationResponse);

    when(authorizationSmcBApi.sendAuthorizationRequestSC(anyString()))
        .thenReturn(authorizationResponse);

    final var idpClientBuilder = mock(IdpClient.IdpClientBuilder.class);
    when(idpClientBuilder.scopes(anySet())).thenReturn(idpClientBuilder);
    when(idpClientBuilder.redirectUrl(anyString())).thenReturn(idpClientBuilder);
    when(idpClientBuilder.codeChallengeMethod(any())).thenReturn(idpClientBuilder);
    when(idpClientBuilder.clientId(anyString())).thenReturn(idpClientBuilder);

    final var idpClient = mock(IdpClient.class);
    when(idpClientBuilder.build()).thenReturn(idpClient);

    final var cardHandle = "123";
    final var telematikId = "2-883110000118994";
    when(cardAuthenticationService.getCardHandle(telematikId)).thenReturn(cardHandle);
    final UnaryOperator<byte[]> mockedContentSigner = TestDataFactory.getContentSigner(cardHandle);
    doReturn(mockedContentSigner).when(cardAuthenticationService).getContentSigner(cardHandle);

    final AuthorizationCodeResult authorizationCodeResult =
        new AuthorizationCodeResult("code", "state", "redirectUri");

    final X509Certificate x509Certificate =
        CertificateUtils.toX509Certificate(ResourceLoader.autCertificateAsByteArray());
    when(cardAuthenticationService.getX509Certificate(any())).thenReturn(x509Certificate);
    when(cardAuthenticationService.createSignedJwt(any(), any(), any(), anyString()))
        .thenReturn("jwt");
    when(cardAuthenticationService.determineAlgorithm(any())).thenReturn("algorithm");

    when(idpClient.login(
            x509Certificate,
            mockedContentSigner,
            "IMevy8z9hyfFsDad6JM0E20MQ_jLInJ6GtE55sGyH3Y",
            "ABCVDFGHT564",
            nonce))
        .thenReturn(authorizationCodeResult);

    when(authorizationSmcBApi.sendAuthCodeSC(any(), any()))
        .thenReturn(Response.status(500).build());

    // when
    final var result = authenticationServiceSpy.login(telematikId, "FQDN");

    // then
    assertThat(result.success()).isFalse();
    assertThat(result.errorMessage()).isNotBlank();
  }

  @Test
  void loginShouldFailWhenSendAuthCodeFailsWithUnexpectedResponse() {
    getMockVauStatusResponseAuthenticationNone();
    final AuthenticationService authenticationServiceSpy = Mockito.spy(authenticationService);

    // given
    final var nonce = "7721435277f5d0137b17ef8b835ca03cf09dc23926aa1766e4f8132433ff37d6";
    mockNonceHandle(nonce);

    final var authorizationResponse = Response.status(302).header("Location", redirectUrl).build();
    when(authorizationSmcBApi.sendAuthorizationRequestSC(anyString()))
        .thenReturn(authorizationResponse);

    when(authorizationSmcBApi.sendAuthorizationRequestSC(anyString()))
        .thenReturn(authorizationResponse);

    final var idpClientBuilder = mock(IdpClient.IdpClientBuilder.class);
    when(idpClientBuilder.scopes(anySet())).thenReturn(idpClientBuilder);
    when(idpClientBuilder.redirectUrl(anyString())).thenReturn(idpClientBuilder);
    when(idpClientBuilder.codeChallengeMethod(any())).thenReturn(idpClientBuilder);
    when(idpClientBuilder.clientId(anyString())).thenReturn(idpClientBuilder);

    final var idpClient = mock(IdpClient.class);
    when(idpClientBuilder.build()).thenReturn(idpClient);

    final var cardHandle = "123";
    final var telematikId = "2-883110000118994";
    when(cardAuthenticationService.getCardHandle(telematikId)).thenReturn(cardHandle);
    final UnaryOperator<byte[]> mockedContentSigner = TestDataFactory.getContentSigner(cardHandle);
    doReturn(mockedContentSigner).when(cardAuthenticationService).getContentSigner(cardHandle);

    final AuthorizationCodeResult authorizationCodeResult =
        new AuthorizationCodeResult("code", "state", "redirectUri");

    final X509Certificate x509Certificate =
        CertificateUtils.toX509Certificate(ResourceLoader.autCertificateAsByteArray());
    when(cardAuthenticationService.getX509Certificate(any())).thenReturn(x509Certificate);
    when(cardAuthenticationService.createSignedJwt(any(), any(), any(), anyString()))
        .thenReturn("jwt");
    when(cardAuthenticationService.determineAlgorithm(any())).thenReturn("algorithm");

    when(idpClient.login(
            x509Certificate,
            mockedContentSigner,
            "IMevy8z9hyfFsDad6JM0E20MQ_jLInJ6GtE55sGyH3Y",
            "ABCVDFGHT564",
            nonce))
        .thenReturn(authorizationCodeResult);

    when(authorizationSmcBApi.sendAuthCodeSC(any(), any()))
        .thenReturn(
            Response.status(500).entity(new ErrorType().errorCode("internalError")).build());

    // when
    final var result = authenticationServiceSpy.login(telematikId, "FQDN");

    // then
    assertThat(result.success()).isFalse();
    assertThat(result.errorMessage()).isNotBlank();
  }

  @Test
  @SneakyThrows
  void loginShouldFailWhenVauNPIsNull() {
    getMockVauStatusResponseAuthenticationNone();
    final AuthenticationService authenticationServiceSpy = Mockito.spy(authenticationService);

    // given
    final var nonce = "7721435277f5d0137b17ef8b835ca03cf09dc23926aa1766e4f8132433ff37d6";
    mockNonceHandle(nonce);

    final var authorizationResponse = Response.status(302).header("Location", redirectUrl).build();
    when(authorizationSmcBApi.sendAuthorizationRequestSC(anyString()))
        .thenReturn(authorizationResponse);

    when(authorizationSmcBApi.sendAuthorizationRequestSC(anyString()))
        .thenReturn(authorizationResponse);

    final var idpClientBuilder = mock(IdpClient.IdpClientBuilder.class);
    when(idpClientBuilder.scopes(anySet())).thenReturn(idpClientBuilder);
    when(idpClientBuilder.redirectUrl(anyString())).thenReturn(idpClientBuilder);
    when(idpClientBuilder.codeChallengeMethod(any())).thenReturn(idpClientBuilder);
    when(idpClientBuilder.clientId(anyString())).thenReturn(idpClientBuilder);

    final var idpClient = mock(IdpClient.class);
    when(idpClientBuilder.build()).thenReturn(idpClient);

    final var cardHandle = "123";
    final var telematikId = "2-883110000118994";
    when(cardAuthenticationService.getCardHandle(telematikId)).thenReturn(cardHandle);
    final UnaryOperator<byte[]> mockedContentSigner = TestDataFactory.getContentSigner(cardHandle);
    doReturn(mockedContentSigner).when(cardAuthenticationService).getContentSigner(cardHandle);

    final AuthorizationCodeResult authorizationCodeResult =
        new AuthorizationCodeResult("code", "state", "redirectUri");

    final X509Certificate x509Certificate =
        CertificateUtils.toX509Certificate(ResourceLoader.autCertificateAsByteArray());
    when(cardAuthenticationService.getX509Certificate(any())).thenReturn(x509Certificate);
    when(cardAuthenticationService.createSignedJwt(any(), any(), any(), anyString()))
        .thenReturn("jwt");
    when(cardAuthenticationService.determineAlgorithm(any())).thenReturn("algorithm");

    when(idpClient.login(
            x509Certificate,
            mockedContentSigner,
            "IMevy8z9hyfFsDad6JM0E20MQ_jLInJ6GtE55sGyH3Y",
            "ABCVDFGHT564",
            nonce))
        .thenReturn(authorizationCodeResult);

    final var authCodeScResponse =
        simulateInbound(Response.status(200).entity(new SendAuthCodeSC200Response()).build());

    when(authorizationSmcBApi.sendAuthCodeSC(any(), any())).thenReturn(authCodeScResponse);

    // when
    final var result = authenticationServiceSpy.login(telematikId, "FQDN");

    // then
    assertThat(result.success()).isFalse();
    assertThat(result.errorMessage()).isNotBlank();
  }

  @Test
  void loginShouldFailWhenCardHandleFails() {
    getMockVauStatusResponseAuthenticationNone();
    // given
    final var telematikId = "2-883110000118994";
    final var nonce = "7721435277f5d0137b17ef8b835ca03cf09dc23926aa1766e4f8132433ff37d6";
    final var nonceResponse =
        simulateInbound(
            Response.status(200).entity(new GetNonce200Response().nonce(nonce)).build());
    when(authorizationSmcBApi.getNonce(userAgent)).thenReturn(nonceResponse);
    when(cardAuthenticationService.getCardHandle(telematikId))
        .thenThrow(
            new TelematikIdNotFoundException(
                "TelematikId " + telematikId + " was not found in connector slots"));
    // when + then
    try {
      authenticationService.login(telematikId, "FQDN");
    } catch (final TelematikIdNotFoundException e) {
      assertThat(e.getMessage())
          .contains("TelematikId " + telematikId + " was not found in connector slots");
    }
  }

  @Test
  void loginShouldSkipAuthFlowBecauseVauStatusOk() {
    String telematikId = "2-883110000118994";
    var mockVauStatus =
        new VauStatus()
            .vaUType("ePA")
            .vaUVersion("1.0")
            .userAuthentication(telematikId)
            .keyID("devKeyId")
            .connectionStart(String.valueOf(System.currentTimeMillis()));
    var simulatedResponse = simulateInbound(Response.status(200).entity(mockVauStatus).build());
    when(vauApiMock.getVauStatus(userAgent)).thenReturn(simulatedResponse);

    // when
    final var result = authenticationService.login(telematikId, "FQDN");

    // then
    assertThat(result.success()).isTrue();
    assertThat(result.httpStatusCode()).isEqualTo(200);
    assertThat(result.errorMessage()).isBlank();

    verify(idpClientBuilder, never()).build();
    verify(authorizationSmcBApi, never()).getNonce(userAgent);
    verify(authorizationSmcBApi, never()).sendAuthorizationRequestSC(anyString());
    verify(authorizationSmcBApi, never()).sendAuthCodeSC(any(), any());
  }

  @Test
  void loginShouldFailBecauseVauStatusReturns403() {
    var telematikId = "2-883110000118994";
    var mockVauStatus =
        new VauStatus()
            .vaUType("ePA")
            .vaUVersion("1.0")
            .userAuthentication(telematikId)
            .keyID("devKeyId")
            .connectionStart(String.valueOf(System.currentTimeMillis()));
    var simulatedResponse = simulateInbound(Response.status(403).entity(mockVauStatus).build());
    when(vauApiMock.getVauStatus(userAgent)).thenReturn(simulatedResponse);

    // when
    final var result = authenticationService.login(telematikId, "FQDN");

    // then
    assertThat(result.success()).isFalse();
    assertThat(result.httpStatusCode()).isEqualTo(403);
    assertThat(result.errorMessage()).isNotBlank();
  }

  private void getMockVauStatusResponseAuthenticationNone() {
    var mockVauStatus =
        new VauStatus()
            .vaUType("ePA")
            .vaUVersion("1.0")
            .userAuthentication("none")
            .keyID("keyId")
            .connectionStart(String.valueOf(System.currentTimeMillis()));
    var simulatedResponse = simulateInbound(Response.status(200).entity(mockVauStatus).build());
    when(vauApiMock.getVauStatus(userAgent)).thenReturn(simulatedResponse);
  }

  private void mockNonceHandle(final String nonce) {
    final var nonceResponse =
        simulateInbound(
            Response.status(200).entity(new GetNonce200Response().nonce(nonce)).build());
    when(authorizationSmcBApi.getNonce(userAgent)).thenReturn(nonceResponse);

    final ExternalAuthenticateResponse mockResponse = mock(ExternalAuthenticateResponse.class);
    final Base64Signature mockSignature = mock(Base64Signature.class);

    when(mockResponse.getSignatureObject())
        .thenReturn(new SignatureObject().withBase64Signature(mockSignature));
    when(cardAuthenticationService.externalAuthenticate(anyString(), any()))
        .thenReturn(mockSignature);
  }
}
