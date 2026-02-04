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
package de.gematik.epa.ps.medication.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static de.gematik.epa.unit.util.TestDataFactory.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

import de.gematik.epa.ps.endpoint.MedicationApiEndpoint;
import de.gematik.epa.ps.utils.AbstractIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EmlRenderClientApiEndpointIntegrationTest extends AbstractIntegrationTest {
  @Autowired MedicationApiEndpoint medicationApiEndpoint;

  @Test
  void contextLoads() {
    assertThat(medicationApiEndpoint).isNotNull();
  }

  @Test
  void shouldReturnEmlAsXhtml() {
    stubSuccessfulGetEmlAsXhtml();
    var response = medicationApiEndpoint.getMedicationListAsXhtml(KVNR);

    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getStatusMessage()).isBlank();
    assertThat(response.getEml()).isNotEmpty();
    assertThat(response.getEml()).contains("<title>Medikationsliste</title>");
  }

  @Test
  void getEmlAsXhtmlShouldReturnNotSuccessWhenApiReturns403() {
    stubHttp403GetEmlAsXhtml();
    var response = medicationApiEndpoint.getMedicationListAsXhtml(KVNR);

    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getStatusMessage()).isNotEmpty();
    assertThat(response.getStatusMessage()).contains("invalidParam");
    assertThat(response.getStatusMessage()).contains("Missing x-insurantid");
  }

  @Test
  void shouldReturnEmlAsPdf() {
    stubSuccessfulGetEmlAsPdf();
    var response = medicationApiEndpoint.getMedicationListAsPdf(KVNR);

    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getStatusMessage()).isBlank();
    assertThat(response.getEml()).isNotEmpty();

    byte[] emlBytes = response.getEml();
    String pdfHeader = new String(emlBytes, 0, 4);
    assertThat(pdfHeader).isEqualTo("%PDF");
  }

  @Test
  void getEmlAsPdfShouldReturnNotSuccessWhenApiReturns403() {
    stubHttp403GetEmlAsPdf();
    var response = medicationApiEndpoint.getMedicationListAsPdf(KVNR);

    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getStatusMessage()).isNotEmpty();
    assertThat(response.getStatusMessage()).contains("invalidParam");
    assertThat(response.getStatusMessage()).contains("Missing x-insurantid");
  }

  @Test
  void shouldReturnEmlAsFhir() {
    var requestId = UUID.randomUUID();
    var date = "ge2024-01-01";
    var count = 10;
    var offset = 0;
    stubSuccessfulGetEmlAsFhir(requestId, date, count, offset);
    var response = medicationApiEndpoint.getEmlAsFhir(KVNR, requestId, date, count, offset, null);

    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getStatusMessage()).isBlank();
    assertThat(response.getEml()).isNotEmpty();
  }

  @Test
  void getEmlAsPdfShouldReturnNotSuccessWhenApiReturns404() {
    stubHttp404GetEmlAsPdf();
    var response = medicationApiEndpoint.getMedicationListAsPdf(KVNR);

    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getStatusMessage()).isNotEmpty();
    assertThat(response.getStatusMessage()).contains("noEntries");
  }

  @Test
  void getEmlAsXhtmlShouldReturnNotSuccessWhenApiReturns404() {
    stubHttp404GetEmlAsXhtml();
    var response = medicationApiEndpoint.getMedicationListAsXhtml(KVNR);

    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getStatusMessage()).isNotEmpty();
    assertThat(response.getStatusMessage()).contains("noEntries");
  }

  private void stubSuccessfulGetEmlAsXhtml() {
    mockEmlRender.stubFor(
        get(urlPathEqualTo("/epa/medication/render/v1/eml/xhtml"))
            .withHeader(X_INSURANTID, equalTo(KVNR))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE_HEADER, ContentType.TEXT_HTML.getMimeType())
                    .withStatus(200)
                    .withBody(
                        """
                            <?xml version="1.0" encoding="UTF-8"?>
                              <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
                              <html xmlns="http://www.w3.org/1999/xhtml">
                              <head>
                                <title>Medikationsliste</title>
                              </head>
                              <body>
                                <h1>Medikationsliste</h1>
                                <p>Dies ist eine Medikationsliste.</p>
                              </body>
                              </html>
                            """)));
  }

  private void stubHttp403GetEmlAsXhtml() {
    mockEmlRender.stubFor(
        get(urlPathEqualTo("/epa/medication/render/v1/eml/xhtml"))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType())
                    .withStatus(403)
                    .withBody(
"""
{"errorCode":"invalidParam","errorDetail":"Missing x-insurantid"}""")));
  }

  private void stubHttp404GetEmlAsXhtml() {
    mockEmlRender.stubFor(
        get(urlPathEqualTo("/epa/medication/render/v1/eml/xhtml"))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType())
                    .withStatus(403)
                    .withBody(
                        """
                                            {"errorCode":"noEntries","errorDetail":"Rendering led to empty list"}""")));
  }

  private void stubSuccessfulGetEmlAsPdf() {
    byte[] pdfBody = new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34};
    mockEmlRender.stubFor(
        get(urlPathEqualTo("/epa/medication/render/v1/eml/pdf"))
            .withHeader(X_INSURANTID, equalTo(KVNR))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE_HEADER, APPLICATION_PDF_VALUE)
                    .withStatus(200)
                    .withBody(pdfBody)));
  }

  private void stubHttp403GetEmlAsPdf() {
    mockEmlRender.stubFor(
        get(urlPathEqualTo("/epa/medication/render/v1/eml/pdf"))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType())
                    .withStatus(403)
                    .withBody(
                        """
                    {"errorCode":"invalidParam","errorDetail":"Missing x-insurantid"}""")));
  }

  private void stubHttp404GetEmlAsPdf() {
    mockEmlRender.stubFor(
        get(urlPathEqualTo("/epa/medication/render/v1/eml/pdf"))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType())
                    .withStatus(404)
                    .withBody(
                        """
                                        {"errorCode":"noEntries","errorDetail":"Rendering led to empty list"}""")));
  }

  @SneakyThrows
  private void stubSuccessfulGetEmlAsFhir(UUID requestId, String date, int count, int offset) {
    var eml =
        FileUtils.readFileToString(
            FileUtils.getFile("src/test/resources/eml-as-fhir.json"), StandardCharsets.UTF_8);
    mockEmlRender.stubFor(
        get(urlPathEqualTo("/epa/medication/api/v1/fhir/$medication-list"))
            .withHeader(X_INSURANTID, equalTo(KVNR))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .withHeader("X-Request-ID", equalTo(requestId.toString()))
            .withQueryParam("date", equalTo(date))
            .withQueryParam("_count", equalTo(String.valueOf(count)))
            .withQueryParam("_offset", equalTo(String.valueOf(offset)))
            .willReturn(aResponse().withStatus(200).withBody(eml)));
  }

  private void stubSuccessfulGetEmpAsPdf() {
    byte[] pdfBody = new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34};
    mockEmlRender.stubFor(
        get(urlPathEqualTo("/epa/medication/render/v1/emp/pdf"))
            .withHeader(X_INSURANTID, equalTo(KVNR))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE_HEADER, APPLICATION_PDF_VALUE)
                    .withStatus(200)
                    .withBody(pdfBody)));
  }

  @Test
  void shouldReturnEmpAsPdf() {
    stubSuccessfulGetEmpAsPdf();
    var response = medicationApiEndpoint.getMedicationPlanAsPdf(KVNR);

    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getStatusMessage()).isBlank();

    byte[] emlBytes = response.getEmp();
    String pdfHeader = new String(emlBytes, 0, 4);

    assertThat(pdfHeader).isEqualTo("%PDF");
  }

  private void stubHttp403GetEmpAsPdf(String errorCode, String errorDetail) {
    mockEmlRender.stubFor(
        get(urlPathEqualTo("/epa/medication/render/v1/emp/pdf"))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .willReturn(
                aResponse()
                    .withHeader(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType())
                    .withStatus(403)
                    .withBody(errorCode + ", " + errorDetail)));
  }

  @Test
  void getEmpAsPdfShouldReturnNotSuccessWhenApiReturns403() {
    String errorCode = "stub error Code";
    String errorDetail = "stub error Detail";
    stubHttp403GetEmpAsPdf(errorCode, errorDetail);
    var response = medicationApiEndpoint.getMedicationPlanAsPdf(KVNR);

    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getStatusMessage()).isNotEmpty();
    assertThat(response.getStatusMessage()).contains("stub error Code");
    assertThat(response.getStatusMessage()).contains("stub error Detail");
    assertThat(response.getEmp()).isNull();

    assertThat(response.getStatusMessage()).doesNotContain("Wrong Error Code / Detail Example");
  }
}
