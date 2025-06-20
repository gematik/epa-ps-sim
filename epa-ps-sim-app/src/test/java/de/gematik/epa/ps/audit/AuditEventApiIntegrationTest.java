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
package de.gematik.epa.ps.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.api.MethodOutcome;
import de.gematik.epa.api.testdriver.audit.dto.GetAuditEventListAsPdfAResponseDTO;
import de.gematik.epa.api.testdriver.audit.dto.GetAuditEventResponseDTO;
import de.gematik.epa.audit.client.AuditRenderClient;
import de.gematik.epa.audit.client.RenderResponse;
import de.gematik.epa.fhir.client.FhirClient;
import de.gematik.epa.ps.endpoint.AuditEventApiEndpoint;
import de.gematik.epa.unit.TestDocumentClientConfiguration;
import de.gematik.epa.unit.TestKonnektorClientConfiguration;
import de.gematik.epa.utils.FhirUtils;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(
    classes = {TestKonnektorClientConfiguration.class, TestDocumentClientConfiguration.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuditEventApiIntegrationTest {

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

  @Autowired AuditEventApiEndpoint auditEventApiEndpoint;
  @Autowired FhirClient fhirClient;
  @Autowired TestRestTemplate testRestTemplate;

  @MockitoBean AuditRenderClient auditRenderClient;

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
    assertThat(auditEventApiEndpoint).isNotNull();
    assertThat(fhirClient).isNotNull();
    assertThat(testRestTemplate).isNotNull();
  }

  @Test
  void getAuditEventsShouldReturnAuditEvents() {
    List<String> samples = new ArrayList<>();
    samples.add("AuditEvent-provideAndRegister.json");
    String url = "/services/epa/testdriver/api/v1/fhir/AuditEvent";
    createFhirResource(samples);
    var response = testRestTemplate.getForEntity(url, GetAuditEventResponseDTO.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getSuccess()).isTrue();
    assertThat(response.getBody().getStatusMessage()).isBlank();
    assertThat(response.getBody().getAuditEvents()).hasSize(1);
  }

  @SneakyThrows
  private void createFhirResource(final List<String> fileNames) {
    fhirClient.customizeSocketTimeout(30000);
    fileNames.forEach(
        fileName -> {
          try {
            var auditEventsAsString =
                FileUtils.readFileToString(
                    FileUtils.getFile("src/test/resources/auditEvent/" + fileName),
                    StandardCharsets.UTF_8);
            MethodOutcome outcome =
                fhirClient.getClient().create().resource(auditEventsAsString).execute();
            assertThat(outcome.getCreated()).isTrue();
            assertThat(outcome.getId()).isNotNull();
          } catch (Exception e) {
            fail(e.getMessage());
          }
        });
  }

  @Test
  void getAuditEventsAsPdfAShouldReturnAuditEvents() {
    String insurantId = "X12345678";
    Boolean signed = true;
    byte[] pdfContent = "dummy pdf content".getBytes();

    RenderResponse renderResponse = new RenderResponse();
    renderResponse.pdf(pdfContent).httpStatusCode(200);

    when(auditRenderClient.getAuditEventAsPdfA(insurantId, signed)).thenReturn(renderResponse);

    GetAuditEventListAsPdfAResponseDTO response =
        auditEventApiEndpoint.getAuditEventListAsPdfA(insurantId, signed);
    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getAuditEventAsPdfA()).isEqualTo(pdfContent);
    assertThat(response.getStatusMessage()).isNull();
  }

  @Test
  void getAuditEventsAsPdfAShouldReturnError() {
    String insurantId = "X12345678";
    Boolean signed = true;
    byte[] pdfContent = "dummy pdf content".getBytes();

    RenderResponse renderResponse = new RenderResponse();
    renderResponse.pdf(pdfContent).httpStatusCode(500).errorMessage("error");

    when(auditRenderClient.getAuditEventAsPdfA(insurantId, signed)).thenReturn(renderResponse);

    GetAuditEventListAsPdfAResponseDTO response =
        auditEventApiEndpoint.getAuditEventListAsPdfA(insurantId, signed);
    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getAuditEventAsPdfA()).isEqualTo(pdfContent);
    assertThat(response.getStatusMessage()).isEqualTo("error");
  }
}
