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
package de.gematik.epa.ps.entitlement;

import static de.gematik.epa.unit.AppTestDataFactory.HMAC_KEY;
import static de.gematik.epa.unit.AppTestDataFactory.clearFqdnProvider;
import static de.gematik.epa.unit.AppTestDataFactory.getReadVSDResponsePZ2;
import static de.gematik.epa.unit.AppTestDataFactory.getReadVSDResponsePZ2RevokedEgk;
import static de.gematik.epa.unit.AppTestDataFactory.setupFqdnProvider;
import static de.gematik.epa.unit.util.TestDataFactory.KVNR;
import static de.gematik.epa.unit.util.TestDataFactory.SMB_AUT_TELEMATIK_ID;
import static de.gematik.epa.unit.util.TestDataFactory.USER_AGENT;
import static de.gematik.epa.unit.util.TestDataFactory.X_INSURANTID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.gematik.epa.api.entitlement.client.EntitlementsApi;
import de.gematik.epa.api.testdriver.entitlement.dto.PostEntitlementRequestDTO;
import de.gematik.epa.api.testdriver.entitlement.dto.PostEntitlementResponseDTO;
import de.gematik.epa.client.JaxRsClientWrapper;
import de.gematik.epa.entitlement.EntitlementService;
import de.gematik.epa.ps.endpoint.EntitlementApiEndpoint;
import de.gematik.epa.unit.TestDocumentClientConfiguration;
import de.gematik.epa.unit.TestKonnektorClientConfiguration;
import de.gematik.epa.utils.HealthRecordProvider;
import de.gematik.epa.utils.InsurantIdHolder;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(
    classes = {TestDocumentClientConfiguration.class, TestKonnektorClientConfiguration.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureRestTestClient
class IntegrationPZTest {

  private static final int PORT = 8080;
  private static final DockerImageName entitlementDockerImage =
      DockerImageName.parse(
          "europe-west3-docker.pkg.dev/gematik-all-infra-prod/epa/entitlement:latest");
  private final GenericContainer<?> entitlementServer =
      new GenericContainer<>(entitlementDockerImage)
          .withExposedPorts(PORT)
          .withEnv("HMAC_KEY", HMAC_KEY)
          .waitingFor(
              Wait.forHttp("/health")
                  .forResponsePredicate(response -> response.contains("UP"))
                  .forStatusCode(200)
                  .withStartupTimeout(Duration.ofMinutes(5L)));

  @Autowired RestTestClient restTestClient;
  @Autowired EntitlementApiEndpoint entitlementApiEndpoint;
  @Autowired EntitlementService entitlementService;
  @Autowired private TestKonnektorClientConfiguration testKonnektorClientConfiguration;

  private JaxRsClientWrapper<EntitlementsApi> jaxRsClientWrapper;

  @BeforeEach
  void setupEach() {
    testKonnektorClientConfiguration.configureVsdServiceResponse(getReadVSDResponsePZ2());
    setupFqdnProvider(KVNR);
  }

  @AfterEach
  void tearDownEach() {
    clearFqdnProvider(KVNR);
  }

  @BeforeAll
  void setup() {
    entitlementServer.start();
    String serverUrl =
        "http://" + entitlementServer.getHost() + ":" + entitlementServer.getMappedPort(PORT);
    jaxRsClientWrapper = new JaxRsClientWrapper<>(serverUrl, USER_AGENT, EntitlementsApi.class);
    entitlementService.setEntitlementClientWrapper(jaxRsClientWrapper);
  }

  @AfterAll
  void tearDown() {
    entitlementServer.stop();
  }

  @Test
  void testStartService() {
    assertThat(true).isTrue();
  }

  @Test
  void contextLoads() {
    assertThat(entitlementApiEndpoint).isNotNull();
    assertThat(jaxRsClientWrapper).isNotNull();
    assertThat(restTestClient).isNotNull();
  }

  @Test
  void shouldSetEntitlement() {
    var request =
        new PostEntitlementRequestDTO()
            .kvnr(KVNR)
            .telematikId(SMB_AUT_TELEMATIK_ID)
            .testCase(PostEntitlementRequestDTO.TestCaseEnum.VALID_HCV);
    assertDoesNotThrow(
        () -> {
          var response =
              restTestClient
                  .post()
                  .uri("/services/epa/testdriver/api/v1/entitlements")
                  .header(X_INSURANTID, KVNR)
                  .body(request)
                  .exchange()
                  .expectStatus()
                  .isOk()
                  .returnResult(PostEntitlementResponseDTO.class);
          assertThat(response.getResponseBody())
              .satisfies(
                  body -> {
                    assertNotNull(body);
                    assertThat(body.getSuccess()).isTrue();
                    assertThat(body.getStatusMessage()).isBlank();
                    assertThat(body.getValidTo()).isNotNull();
                  });
        });
  }

  @Test
  void setEntitlementWithoutHcvShouldReturnHttp409() {
    var request =
        new PostEntitlementRequestDTO()
            .kvnr(KVNR)
            .telematikId(SMB_AUT_TELEMATIK_ID)
            .testCase(PostEntitlementRequestDTO.TestCaseEnum.NO_HCV);
    var response =
        restTestClient
            .post()
            .uri("/services/epa/testdriver/api/v1/entitlements")
            .header(X_INSURANTID, KVNR)
            .body(request)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(PostEntitlementResponseDTO.class);
    assertThat(response.getResponseBody())
        .satisfies(
            body -> {
              assertNotNull(body);
              assertThat(body.getSuccess()).isFalse();
              assertThat(body.getStatusMessage())
                  .contains("409", "hcvMissing", "hcv-value of jwt does not exist");
              assertThat(body.getValidTo()).isNull();
            });
  }

  @Test
  void setEntitlementInvalidHcvHashShouldReturnHttp403() {
    var request =
        new PostEntitlementRequestDTO()
            .kvnr(KVNR)
            .telematikId(SMB_AUT_TELEMATIK_ID)
            .testCase(PostEntitlementRequestDTO.TestCaseEnum.INVALID_HCV_HASH);
    var response =
        restTestClient
            .post()
            .uri("/services/epa/testdriver/api/v1/entitlements")
            .header(X_INSURANTID, KVNR)
            .body(request)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(PostEntitlementResponseDTO.class);
    assertThat(response.getResponseBody())
        .satisfies(
            body -> {
              assertNotNull(body);
              assertThat(body.getSuccess()).isFalse();
              assertThat(body.getStatusMessage())
                  .contains(
                      "403",
                      "invalidToken",
                      "HCV mismatch: The HCV from JWT does not match the HCV from Pruefziffer");
              assertThat(body.getValidTo()).isNull();
            });
  }

  @Test
  void setEntitlementInvalidHcvStructureShouldReturnHttp400InvalidToken() {
    var request =
        new PostEntitlementRequestDTO()
            .kvnr(KVNR)
            .telematikId(SMB_AUT_TELEMATIK_ID)
            .testCase(PostEntitlementRequestDTO.TestCaseEnum.INVALID_HCV_STRUCTURE);
    var response =
        restTestClient
            .post()
            .uri("/services/epa/testdriver/api/v1/entitlements")
            .header(X_INSURANTID, KVNR)
            .body(request)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(PostEntitlementResponseDTO.class);
    assertThat(response.getResponseBody())
        .satisfies(
            body -> {
              assertNotNull(body);
              assertThat(body.getSuccess()).isFalse();
              assertThat(body.getStatusMessage())
                  .contains("400", "malformedRequest", "HCV in JWT is not a valid base64 string");
              assertThat(body.getValidTo()).isNull();
            });
  }

  @Test
  void testPZ2WithRevokedEgkShouldReturn400() {
    testKonnektorClientConfiguration.configureVsdServiceResponse(getReadVSDResponsePZ2RevokedEgk());

    var request =
        new PostEntitlementRequestDTO()
            .kvnr(KVNR)
            .telematikId(SMB_AUT_TELEMATIK_ID)
            .testCase(PostEntitlementRequestDTO.TestCaseEnum.VALID_HCV);
    var response =
        restTestClient
            .post()
            .uri("/services/epa/testdriver/api/v1/entitlements")
            .header(X_INSURANTID, KVNR)
            .body(request)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(PostEntitlementResponseDTO.class);
    assertThat(response.getResponseBody())
        .satisfies(
            body -> {
              assertNotNull(body);
              assertThat(body.getSuccess()).isFalse();
              assertThat(body.getStatusMessage())
                  .contains("400", "malformedRequest", "eGK is locked");
              assertThat(body.getValidTo()).isNull();
            });
  }

  @Test
  void testPZ2WithWrongKvnrShouldReturn400() {
    String wrongInsurantId = "X110572347";
    InsurantIdHolder.setInsurantId(wrongInsurantId);
    HealthRecordProvider.addHealthRecord(wrongInsurantId, "http://localhost:8080");

    var request =
        new PostEntitlementRequestDTO()
            .kvnr(KVNR)
            .telematikId(SMB_AUT_TELEMATIK_ID)
            .testCase(PostEntitlementRequestDTO.TestCaseEnum.VALID_HCV);
    var response =
        restTestClient
            .post()
            .uri("/services/epa/testdriver/api/v1/entitlements")
            .header(X_INSURANTID, wrongInsurantId)
            .body(request)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(PostEntitlementResponseDTO.class);
    assertThat(response.getResponseBody())
        .satisfies(
            body -> {
              assertNotNull(body);
              assertThat(body.getSuccess()).isFalse();
              assertThat(body.getStatusMessage())
                  .contains("400", "malformedRequest", "Prüfziffer for wrong KVNR (Attack?)");
              assertThat(body.getValidTo()).isNull();
            });

    clearFqdnProvider(wrongInsurantId);
    InsurantIdHolder.clear();
  }
}
