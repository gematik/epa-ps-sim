/*-
 * #%L
 * epa-ps-sim-lib
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
package de.gematik.epa.authentication;

import de.gematik.epa.api.authorization.client.AuthorizationSmcBApi;
import de.gematik.epa.api.authorization.client.dto.ErrorType;
import de.gematik.epa.api.authorization.client.dto.GetNonce200Response;
import de.gematik.epa.api.authorization.client.dto.SendAuthCodeSC200Response;
import de.gematik.epa.api.authorization.client.dto.SendAuthCodeSCtype;
import de.gematik.epa.api.vau.client.VauApi;
import de.gematik.epa.api.vau.client.dto.VauStatus;
import de.gematik.epa.authentication.exception.TelematikIdNotFoundException;
import de.gematik.epa.client.JaxRsClientWrapper;
import de.gematik.epa.konnektor.CardAuthenticationService;
import de.gematik.epa.konnektor.KonnektorContextProvider;
import de.gematik.epa.konnektor.KonnektorInterfaceAssembly;
import de.gematik.epa.konnektor.SmbInformationProvider;
import de.gematik.epa.konnektor.client.AuthSignatureServiceClient;
import de.gematik.epa.utils.TelematikIdHolder;
import de.gematik.idp.client.AuthorizationCodeResult;
import de.gematik.idp.client.IdpClient;
import de.gematik.idp.client.IdpClientRuntimeException;
import de.gematik.idp.field.ClaimName;
import de.gematik.idp.field.CodeChallengeMethod;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwt.JwtClaims;
import telematik.ws.conn.cardservice.xsd.v8_1.CardInfoType;

@Slf4j
public class AuthenticationService {

  protected static final String ERROR_IN_LOGIN_PROCESS_NO_ERROR_MESSAGE_AVAILABLE =
      "Error in login process. No error message available.";
  protected static final String ERROR_WHILE_GETTING_AUTHORIZATION_CODE_FROM_IDP =
      "Error while getting authorization code from IDP";
  protected static final String ERROR_WHILE_PARSING_REDIRECT_URL =
      "Error while parsing redirectUrl";

  private static final List<Integer> AUTHZ_SERVER_ERROR_CODES = List.of(400, 403, 409, 500);
  private final AuthorizationSmcBApi authorizationSmcBApi;
  private final IdpClient.IdpClientBuilder idpClientBuilder;
  private final JaxRsClientWrapper<VauApi> vauApiJaxRsClientWrapper;
  private final String authServiceUserAgent;

  @Setter private CardAuthenticationService cardAuthenticationService;

  public AuthenticationService(
      final AuthorizationSmcBApi authorizationSmcBApi,
      final IdpClient.IdpClientBuilder idpClientBuilder,
      final KonnektorContextProvider contextProvider,
      final KonnektorInterfaceAssembly konnektorInterfaceAssembly,
      final SmbInformationProvider smbInformationProvider,
      final JaxRsClientWrapper<VauApi> vauApiJaxRsClientWrapper,
      final String authServiceUserAgent) {
    this.authorizationSmcBApi = authorizationSmcBApi;
    this.idpClientBuilder = idpClientBuilder;
    this.vauApiJaxRsClientWrapper = vauApiJaxRsClientWrapper;
    this.authServiceUserAgent = authServiceUserAgent;
    this.cardAuthenticationService =
        new CardAuthenticationService(
            smbInformationProvider,
            new AuthSignatureServiceClient(contextProvider, konnektorInterfaceAssembly));
  }

  private static String createErrorMessage(final ErrorType errorType) {
    return "code: " + errorType.getErrorCode() + ", detail:" + errorType.getErrorDetail();
  }

  public LoginResult login(final String telematikId) {
    return login(telematikId, "<unused parameters>");
  }

  public LoginResult login(final String telematikId, final String fqdn) {
    // 0. check /VAU-Status for user-session
    // 1. auhtzclient.getNonce() (A_24881)
    // 2. authzclient.sendAuthzRequest() -> liefert u.a. , scope, codecChallenge, clientID (A_24760)
    // 3. idpclient.login() (A_24760)
    // 4. sign nonce (A_24883)
    // 5. authzclient.sendAuthCode() (A_20668)

    // else -> continue
    final var loginResult = new LoginResult().telematikId(telematikId).fqdn(fqdn).success(true);
    TelematikIdHolder.setTelematikId(telematikId);

    // 0. check /VAU-Status for user-session
    final Response vauResponse =
        vauApiJaxRsClientWrapper.getServiceApi().getVauStatus(this.authServiceUserAgent);
    int vauResponseStatus = vauResponse.getStatus();
    if (vauResponseStatus == 200) {
      var vauStatus = vauResponse.readEntity(VauStatus.class);
      boolean authNotNone = vauStatus.getUserAuthentication().equalsIgnoreCase("none");
      boolean matchingTelematikId = vauStatus.getUserAuthentication().contains(telematikId);
      if (!authNotNone && matchingTelematikId) {
        // if "User-Authentication" != "None" && like telematikId -> no login required
        log.info(
            "VAU-Status, no login required, matching telematikID: {}",
            vauStatus.getUserAuthentication());
        return loginResult.httpStatusCode(200);
      }
    } else {
      log.warn("Unexpected error while getting VAU-Status: {}", vauResponse);
      return loginResult
          .httpStatusCode(vauResponseStatus)
          .success(false)
          .errorMessage("Unexpected error while getting VAU-Status");
    }

    // 1. A_24881
    fetchNonce(loginResult);
    if (!loginResult.success()) {
      return loginResult;
    }

    final String cardHandle = getCardHandle(telematikId, loginResult);
    if (null == cardHandle) {
      return loginResult;
    }

    final CardInfoType cardInfo = new CardInfoType().withCardHandle(cardHandle);

    // 2. A_24760
    final Map<String, String> paramsFromAuthzResponse = new HashMap<>();
    final IdpClient idpClient = createIdpClient(paramsFromAuthzResponse, loginResult);
    if (idpClient == null) {
      return loginResult;
    }

    // 3. A_24760
    final X509Certificate certificate = cardAuthenticationService.getX509Certificate(cardInfo);

    final UnaryOperator<byte[]> contentSigner = getContentSigner(cardHandle, loginResult);
    if (null == contentSigner) {
      return loginResult;
    }

    final String authorizationCode =
        doIdpLogin(idpClient, certificate, contentSigner, paramsFromAuthzResponse, loginResult);
    if (null == authorizationCode) {
      return loginResult;
    }

    // 4. sign nonce A_24883 (create clientAttest)
    final String clientAttest = createSignedJwt(certificate, contentSigner, loginResult);
    if (null == clientAttest) {
      return loginResult;
    }

    // 5. A_24886 und A_20668
    sendAuthCodeSC(authorizationCode, clientAttest, loginResult);
    if (loginResult.success()) {
      log.info("Login successful for telematikId: {}", telematikId);
    }

    return loginResult;
  }

  private void sendAuthCodeSC(
      final String authorizationCode, final String clientAttest, final LoginResult loginResult) {
    try (final Response response =
        this.authorizationSmcBApi.sendAuthCodeSC(
            this.authServiceUserAgent,
            new SendAuthCodeSCtype()
                .authorizationCode(authorizationCode)
                .clientAttest(clientAttest))) {
      try {
        if (response.getStatus() == 200) {
          final SendAuthCodeSC200Response sendAuthCodeSC200Response =
              response.readEntity(SendAuthCodeSC200Response.class);

          if (sendAuthCodeSC200Response.getVauNp() == null) {
            loginResult.errorMessage("VAU-NP is null").success(false);
            logResult(loginResult);
            return;
          }

          loginResult.httpStatusCode(200).success(true);
        } else {
          handleAuthzServerError(response, loginResult);
        }
      } catch (final Exception ex) {
        loginResult.errorMessage("Error while sendAuthCodeSC: " + ex.getMessage()).success(false);
        logResult(loginResult);
      }
    }
  }

  private String createSignedJwt(
      final X509Certificate certificate,
      final UnaryOperator<byte[]> contentSigner,
      final LoginResult loginResult) {
    try {
      final JwtClaims claims = new JwtClaims();
      claims.setClaim(ClaimName.NONCE.getJoseName(), loginResult.nonce());
      claims.setClaim(ClaimName.ISSUED_AT.getJoseName(), ZonedDateTime.now().toEpochSecond());
      claims.setClaim(
          ClaimName.EXPIRES_AT.getJoseName(), ZonedDateTime.now().plusMinutes(20).toEpochSecond());
      return cardAuthenticationService.createSignedJwt(
          claims,
          certificate,
          contentSigner,
          cardAuthenticationService.determineAlgorithm(certificate.getPublicKey()));
    } catch (final Exception e) {
      loginResult
          .errorMessage("Error while creating clientAttest: " + e.getMessage())
          .success(false);
      logResult(loginResult);
    }
    return null;
  }

  @Nullable
  private String doIdpLogin(
      final IdpClient idpClient,
      final X509Certificate certificate,
      final UnaryOperator<byte[]> contentSigner,
      final Map<String, String> paramsFromAuthzResponse,
      final LoginResult loginResult) {

    idpClient.initialize();
    final AuthorizationCodeResult authorizationCodeResult;
    try {
      authorizationCodeResult =
          idpClient.login(
              certificate,
              contentSigner,
              paramsFromAuthzResponse.get("code_challenge"),
              paramsFromAuthzResponse.get("state"),
              paramsFromAuthzResponse.get("nonce"));
    } catch (final IdpClientRuntimeException e) {
      loginResult.errorMessage(e.toString()).success(false);
      logResult(loginResult);
      return null;
    }

    final String authorizationCode = authorizationCodeResult.getAuthorizationCode();
    if (authorizationCode == null) {
      loginResult.errorMessage(ERROR_WHILE_GETTING_AUTHORIZATION_CODE_FROM_IDP).success(false);
      logResult(loginResult);
      return null;
    }

    return authorizationCode;
  }

  private UnaryOperator<byte[]> getContentSigner(
      final String cardHandle, final LoginResult loginResult) {
    try {
      return cardAuthenticationService.getContentSigner(cardHandle);
    } catch (final Exception ex) {
      loginResult.errorMessage(ex.getMessage()).success(false);
      logResult(loginResult);
    }
    return null;
  }

  private IdpClient createIdpClient(
      final Map<String, String> paramsFromAuthzResponse, final LoginResult loginResult) {
    try (final Response result =
        this.authorizationSmcBApi.sendAuthorizationRequestSC(this.authServiceUserAgent)) {

      if (result.getStatus() == 302) {
        final String redirectUrl = result.getHeaderString("Location");
        try {
          paramsFromAuthzResponse.putAll(getQueryParams(redirectUrl));
          final Set<String> scopes =
              Arrays.stream(paramsFromAuthzResponse.get("scope").split("[+]"))
                  .collect(Collectors.toSet());
          this.idpClientBuilder
              .scopes(scopes)
              .redirectUrl(paramsFromAuthzResponse.get("redirect_uri"))
              .codeChallengeMethod(CodeChallengeMethod.S256)
              .clientId(paramsFromAuthzResponse.get("client_id"));
        } catch (final Exception e) {
          log.error(ERROR_WHILE_PARSING_REDIRECT_URL, e);
          loginResult.errorMessage(ERROR_WHILE_PARSING_REDIRECT_URL).success(false);
          logResult(loginResult);
          return null;
        }
      } else {
        handleAuthzServerError(result, loginResult);
        return null;
      }
    }
    return idpClientBuilder.build();
  }

  private String getCardHandle(final String telematikId, final LoginResult loginResult) {
    try {
      return cardAuthenticationService.getCardHandle(telematikId);
    } catch (final TelematikIdNotFoundException ex) {
      loginResult.errorMessage(ex.getMessage()).success(false);
      logResult(loginResult);
    }
    return null;
  }

  private void fetchNonce(final LoginResult loginResult) {
    try {
      final Response nonceResponse = this.authorizationSmcBApi.getNonce(this.authServiceUserAgent);
      if (nonceResponse.getStatus() == 200) {
        final GetNonce200Response nonceResponseEntity =
            nonceResponse.readEntity(GetNonce200Response.class);
        loginResult.nonce(nonceResponseEntity.getNonce());
      } else {
        handleAuthzServerError(nonceResponse, loginResult);
      }
    } catch (final Exception ex) {
      loginResult.errorMessage("Error while getting nonce: " + ex.getMessage()).success(false);
      logResult(loginResult);
    }
  }

  private void handleAuthzServerError(final Response response, final LoginResult loginResult) {
    ErrorType errorType = null;
    if (AUTHZ_SERVER_ERROR_CODES.contains(response.getStatus()) && response.getEntity() != null) {
      try {
        errorType = (ErrorType) response.getEntity();
      } catch (final Exception e) {
        errorType = response.readEntity(ErrorType.class);
      }
    }
    loginResult.httpStatusCode(response.getStatus());
    loginResult.errorMessage(
        errorType != null
            ? createErrorMessage(errorType)
            : ERROR_IN_LOGIN_PROCESS_NO_ERROR_MESSAGE_AVAILABLE);
    loginResult.success(false);
    logResult(loginResult);
  }

  private void logResult(final LoginResult loginResult) {
    log.error("LoginResult: {}", loginResult);
  }

  /**
   * Extracts the query parameters from the given URL e.g. https://idp-dienst.de/authz?
   * client_id=ePA-Aktensystem007& response_type=code&
   * redirect_uri=http%3A%2F%2Ftest-ps.gematik.de%2Ferezept& state=ABCVDFGHT564&
   * code_challenge=asdrtgasdfdf...5ssdfgaydfg& code_challenge_method=S256& scope=openid+ePA&
   * nonce=7721435277f5d0137b17ef8b835ca03cf09dc23926aa1766e4f8132433ff37d6
   *
   * @param url the URL to extract the query parameters from
   * @return a map containing the query parameters
   */
  protected Map<String, String> getQueryParams(final String url) {
    try {
      return Stream.of(new URI(url).getQuery().split("&"))
          .filter(param -> param.contains("="))
          .map(param -> param.split("="))
          .collect(Collectors.toMap(array -> array[0], array -> array[1]));
    } catch (final Exception ex) {
      throw new IllegalArgumentException("No query parameters found in URL");
    }
  }
}
