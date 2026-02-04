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
package de.gematik.epa.ps.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static de.gematik.epa.unit.AppTestDataFactory.setupFqdnProvider;
import static de.gematik.epa.unit.util.TestDataFactory.*;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import de.gematik.epa.ps.fhir.config.TestFhirClientProvider;
import de.gematik.epa.ps.kob.config.VauProxyConfiguration;
import de.gematik.epa.unit.TestDocumentClientConfiguration;
import de.gematik.epa.unit.TestKonnektorClientConfiguration;
import de.gematik.epa.utils.HealthRecordProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(
    classes = {
      TestKonnektorClientConfiguration.class,
      TestDocumentClientConfiguration.class,
      TestFhirClientProvider.class
    })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Getter
public class AbstractIntegrationTest {

  @RegisterExtension
  public static WireMockExtension mockAuthzServer =
      WireMockExtension.newInstance().options(wireMockConfig().port(8081)).build();

  @RegisterExtension
  public static WireMockExtension mockIdpServer =
      WireMockExtension.newInstance().options(wireMockConfig().port(8082)).build();

  @RegisterExtension
  public static WireMockExtension mockEntitlementServer =
      WireMockExtension.newInstance().options(wireMockConfig().port(8084)).build();

  @RegisterExtension
  public static WireMockExtension mockInformationServer1 =
      WireMockExtension.newInstance().options(wireMockConfig().port(8088)).build();

  @RegisterExtension
  public static WireMockExtension mockInformationServer2 =
      WireMockExtension.newInstance().options(wireMockConfig().port(8089)).build();

  @RegisterExtension
  public static WireMockExtension mockEmlRender =
      WireMockExtension.newInstance().options(wireMockConfig().port(8085)).build();

  @RegisterExtension
  public static WireMockExtension mockVauProxyServer =
      WireMockExtension.newInstance().options(wireMockConfig().port(8099)).build();

  @Autowired private VauProxyConfiguration vauProxyConfiguration;

  @SneakyThrows
  private static void mockIdpServer(
      String redirectUri,
      String state,
      String codeChallenge,
      String codeChallengeMethod,
      String nonce) {
    mockIdpServer.stubFor(
        get(urlEqualTo("/discoveryDocument"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(Files.readString(Path.of("src/test/resources/jwt.txt")))));

    mockIdpServer.stubFor(
        get(urlEqualTo("/idpEnc/jwk.json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(Files.readString(Path.of("src/test/resources/jwk-cert.json")))));

    mockIdpServer.stubFor(
        get(urlEqualTo("/idpSig/jwk.json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(Files.readString(Path.of("src/test/resources/jwk-sig.json")))));

    mockIdpServer.stubFor(
        get(urlEqualTo(
                "/sign_response?client_id=ePA&response_type=code&redirect_uri="
                    + redirectUri
                    + "&state="
                    + state
                    + "&code_challenge="
                    + codeChallenge
                    + "&code_challenge_method="
                    + codeChallengeMethod
                    + "&scope=openid+epa&nonce="
                    + nonce))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(Files.readString(Path.of("src/test/resources/sign-response.json")))));

    final String signedChallenge = "eyJhbGciOiJFQ";

    final String code =
        "eyJhbGciOiJkaXIiLCJlbmMiOiJBMjU2R0NNIiwiY3R5IjoiTkpXVCIsImV4cCI6MTcxMDI0NzYxMn0..ZOKNbPWvwpLMq1u0.MmxKiiKrlYlwN0tPEDrEeqMpGhO_zbBE9HANFyf4EVxpaRvJ90YIQAGCwuXQJbGZB_D4WgakWUACjHIh6PJC5kq9Fkw4sDuRuBWCq8QKIcflcGQcnIzdlv0bwKlYqvzc1VAoMb5snFPufAFb_dAZWpz5lJ9aSVRf7F04Ygzk3JMt9Yerk_h7iPI-17DgGYd0RQKvq_lOfd3mIzTgIFzlJNdVhSPOD6JrDeEzzQ1Z0K9Ju-nnYHi6F9p3AhG6xl9NDd3h8zmAY2QH3RzlExyTYTGCCY5OgoBYiSuHsI4Cu357km6o173TnNek8HMSm5Y_XEcZs_Lnhmn0PnAzGYxo9SJLfzL0nX4CVzY_dD2cfRBrqNASEagHleHQ4HTI15ZirTq8zFLBmSMmHCN4ufUHnkJOAZRceZYcwPwl2S2KjfmDaPqxW7f-ZdepoUgFh8jV8O0n5GjPeRtVjVx3NM48ofKNzjX5Isaf4GGRChPOylR837693M_LgdZkRnpOlWozoAOuIHP01WpHCK2HyJT10_3HK8f8GrJS80CZuvKXUS6mljGYP4cGmtJ8keSU2Hw3PhIWufzu5JGNwh1egputPcP5AQb8A8nyAl6Og9kptCK13lzg_5Hu3oWd76ow1rn58LuAJCZafvxoe2KylY99RvxLp31Jf_dzDjxWe0lkPNV2LvK32Ffi6w4a0n4v9KOD16_bCZQHNCOF4CioTn-1tCnrEBUpXJqid8WpXZ4HuJgTkdgZnvLaU2boSSWU7snUWr4MQy9JGkIMv_BAkOT-_mEe1u7BCh_6s1Ybo2jDduMdO4AirWvcYmDZQaMWLkeHZzkHufjvHRWNzrSzXcX352JkYp0j3yZsFs6HvSdxh_S3jd_EjRIq9mcfWIk-qKWITMgYOZnOuQnTEDkDKbepEqtcoY1A3yC2An-M405jzEn_d036umZ6_EFUupeT0QetYuh8k_F_jY0182iCOKHy4HyNmIkUciFu8siHzUDII9vFOzCos0CB6KgobcnfKTjg7kpTHYQVEz9E5KME-4i62E7MNGmBjTcaaRlNlt-0tuag7SnKIzCPxD8-rAj-ebBiXgm5OiU.8L_kXaLNHCQZjlDvPSxZ6A";

    mockIdpServer.stubFor(
        post(urlEqualTo("/sign_response"))
            .withHeader(CONTENT_TYPE_HEADER, equalTo("application/x-www-form-urlencoded"))
            .withHeader("User-Agent", equalTo("IdP-Client"))
            .withFormParam("signed_challenge", containing(signedChallenge))
            .willReturn(
                aResponse()
                    .withHeader(
                        "Location",
                        "http://localhost:8082/authz?" + "code=" + code + "&state=" + state)
                    .withStatus(302)));

    mockVauProxyServer.stubFor(post(urlEqualTo("/reset")).willReturn(aResponse().withStatus(200)));
    mockVauStatusWithNoneAuthentication();
  }

  public static void mockAuthzServer(
      String userAgent,
      String contentTypeJson,
      String nonce,
      String redirectUri,
      String state,
      String codeChallenge,
      String codeChallengeMethod) {
    mockAuthzServer.stubFor(
        get(urlEqualTo("/epa/authz/v1/getNonce"))
            .withHeader(X_USERAGENT, equalTo(userAgent))
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE_HEADER, contentTypeJson)
                    .withStatus(200)
                    .withBody("{" + "  \"nonce\": \"" + nonce + "\"\n" + "}")));

    mockAuthzServer.stubFor(
        get(urlEqualTo("/epa/authz/v1/send_authorization_request_sc"))
            .withHeader(X_USERAGENT, equalTo(userAgent))
            .willReturn(
                aResponse()
                    .withHeader(
                        "Location",
                        "http://localhost:8082/authz?"
                            + "client_id=ePA"
                            + "&response_type=code"
                            + "&redirect_uri="
                            + redirectUri
                            + "&state="
                            + state
                            + "&code_challenge="
                            + codeChallenge
                            + "&code_challenge_method="
                            + codeChallengeMethod
                            + "&scope=openid+epa"
                            + "&nonce="
                            + nonce)
                    .withStatus(302)));

    mockAuthzServer.stubFor(
        post(urlEqualTo("/epa/authz/v1/send_authcode_sc"))
            .withHeader(X_USERAGENT, equalTo(userAgent))
            .withHeader(CONTENT_TYPE_HEADER, equalTo(contentTypeJson))
            .withRequestBody(
                matchingJsonPath("$.authorizationCode", containing("eyJ"))
                    .and(matchingJsonPath("$.clientAttest", containing("eyJ"))))
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE_HEADER, contentTypeJson)
                    .withStatus(200)
                    .withBody(
                        "{\"vau-np\": \"4aadc671bf058fb6df5f181ad94f4130b3d3e86f3a7ffd53e6ea6a3f01e65608\"}")));
  }

  @BeforeEach
  public void setUp() {
    // reset all data with health record information
    mockInformationServer1.resetAll();
    mockInformationServer2.resetAll();
    HealthRecordProvider.getAllHealthRecords()
        .forEach((key, value) -> HealthRecordProvider.clearHealthRecord(key));
    setupFqdnProvider(KVNR); // add KVNR as default for all Tests

    mockAuthzServer.resetAll();
    mockIdpServer.resetAll();
    mockEntitlementServer.resetAll();
    mockEmlRender.resetAll();
    mockVauProxyServer.resetAll();

    vauProxyConfiguration.setHost("localhost");
    vauProxyConfiguration.setPort(String.valueOf(mockVauProxyServer.getPort()));

    // given
    final String nonce = "7721435277f5d0137b17ef8b835ca03cf09dc23926aa1766e4f8132433ff37d6";
    final String redirectUri = "http%3A%2F%2Ftest-ps.gematik.de%2FePA";
    final String codeChallenge = "IMevy8z9hyfFsDad6JM0E20MQ_jLInJ6GtE55sGyH3Y";
    final String state = "ABCVDFGHT564";
    final String codeChallengeMethod = "S256";
    final String contentTypeJson = "application/json";

    mockAuthzServer(
        USER_AGENT, contentTypeJson, nonce, redirectUri, state, codeChallenge, codeChallengeMethod);

    mockIdpServer(redirectUri, state, codeChallenge, codeChallengeMethod, nonce);
  }

  @AfterEach
  void tearDown() {
    HealthRecordProvider.getAllHealthRecords()
        .forEach((key, value) -> HealthRecordProvider.clearHealthRecord(key));
  }

  public void stubSuccessfulGetRecordStatus(final WireMockExtension server, int statusCode) {
    server.stubFor(
        get(urlEqualTo("/information/api/v1/ehr"))
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType())
                    .withStatus(statusCode)));
  }

  public void stubFailedGetRecordStatus(
      final WireMockExtension server, int statusCode, final String body) {
    server.stubFor(
        get(urlEqualTo("/information/api/v1/ehr"))
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType())
                    .withStatus(statusCode)
                    .withBody(body)));
  }

  public void stubGetConsentDecisions(
      final WireMockExtension server, int statusCode, final String body) {
    server.stubFor(
        get(urlEqualTo("/information/api/v1/ehr/consentdecisions"))
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType())
                    .withBody(body)
                    .withStatus(statusCode)));
  }

  public static void mockVauStatusWithNoneAuthentication() {
    mockVauProxyServer.stubFor(
        get("/VAU-Status")
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE_HEADER, "application/json")
                    .withBody(
                        String.format(
                            """
                            {
                            "VAU-Type": "epa",
                            "VAU-Version" : "gematik-3.0",
                            "User-Authentication" : "%s",
                            "KeyID": "wiremockKeyId",
                            "Connection-Start": "%s"
                            }""",
                            "none", System.currentTimeMillis()))));
  }
}
