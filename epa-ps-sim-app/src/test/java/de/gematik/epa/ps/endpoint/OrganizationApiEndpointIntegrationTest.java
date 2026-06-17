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
package de.gematik.epa.ps.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import ca.uhn.fhir.rest.api.MethodOutcome;
import de.gematik.epa.api.testdriver.organization.dto.GetOrganizationResponseDTO;
import de.gematik.epa.fhir.client.FhirClient;
import de.gematik.epa.ps.fhir.config.TestFhirClientProvider;
import de.gematik.epa.unit.TestDocumentClientConfiguration;
import de.gematik.epa.unit.TestKonnektorClientConfiguration;
import de.gematik.epa.utils.FhirUtils;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
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
    classes = {
      TestKonnektorClientConfiguration.class,
      TestDocumentClientConfiguration.class,
      TestFhirClientProvider.class
    })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureRestTestClient
@Slf4j
class OrganizationApiEndpointIntegrationTest {

  private static final int PORT = 8080;
  private static final DockerImageName fhirDockerImage =
      DockerImageName.parse("hapiproject/hapi:latest");
  private final GenericContainer<?> fhirServer =
      new GenericContainer<>(fhirDockerImage)
          .withExposedPorts(PORT)
          .waitingFor(
              Wait.forHttp("/fhir/metadata")
                  .forStatusCode(200)
                  .withStartupTimeout(Duration.ofMinutes(5L)));

  private final List<String> organizationIds = new ArrayList<>();

  @Autowired FhirClient fhirClient;
  @Autowired RestTestClient restTestClient;

  @BeforeAll
  void setUp() {
    fhirServer.start();
    var serverUrl =
        "http://" + fhirServer.getHost() + ":" + fhirServer.getMappedPort(PORT) + "/fhir";
    fhirClient.setServerUrl(serverUrl, "PS-SIM");
    FhirUtils.setJsonParser(fhirClient.getContext().newJsonParser());
  }

  @AfterAll
  void tearDown() {
    fhirServer.stop();
  }

  @Test
  void contextLoads() {
    assertThat(fhirClient).isNotNull();
    assertThat(restTestClient).isNotNull();
  }

  @TestFactory
  List<DynamicTest> shouldGetOrganizationAsExpected() {
    return List.of(
        DynamicTest.dynamicTest(
            "get organizations",
            () ->
                callApi(
                    List.of("organization.json"), "/services/epa/testdriver/api/v1/organizations")),
        DynamicTest.dynamicTest(
            "get organization by ID",
            () ->
                callApi(
                    List.of("organization.json"),
                    "/services/epa/testdriver/api/v1/organizations?id=%s")));
  }

  private void callApi(List<String> data, String url) {
    createTestData(data);

    // in case there is a query parameter in the URI to get organization by ID
    if (url.contains("id=")) {
      url = url.formatted(organizationIds.getFirst());
    }

    var response =
        restTestClient.get().uri(url).exchange().returnResult(GetOrganizationResponseDTO.class);

    assertThat(response.getStatus().value()).isEqualTo(200);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody().getSuccess()).isTrue();
    assertThat(response.getResponseBody().getStatusMessage()).isBlank();
    assertThat(response.getResponseBody().getOrganizations()).isNotEmpty().hasSizeGreaterThan(0);
  }

  private void createTestData(List<String> fileNames) {
    fhirClient.customizeSocketTimeout(30000);
    fileNames.forEach(
        fileName -> {
          try {
            var medicationAsString =
                FileUtils.readFileToString(
                    FileUtils.getFile("src/test/resources/organization/" + fileName),
                    StandardCharsets.UTF_8);
            MethodOutcome outcome =
                fhirClient.getClient().create().resource(medicationAsString).execute();
            assertThat(outcome.getCreated()).isTrue();
            assertThat(outcome.getId()).isNotNull();
            String resourceId = outcome.getId().getIdPart();
            this.organizationIds.add(resourceId);
          } catch (Exception e) {
            fail(e.getMessage());
          }
        });
  }
}
