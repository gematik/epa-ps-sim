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

import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

import de.gematik.epa.client.JaxRsOutgoingRequestInterceptor;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;

@Slf4j
public class AuditRenderClient {

  private static final String X_INSURANT_ID = "x-insurantid";
  private static final String X_USERAGENT = "x-useragent";
  private final String pdfAapiPath;
  private final String userAgent;
  private final WebClient webClient;

  public AuditRenderClient(final String apiUrl, final String pdfAapiPath, final String userAgent) {
    this.pdfAapiPath = pdfAapiPath;
    this.userAgent = userAgent;
    this.webClient = WebClient.create(apiUrl, true);

    ClientConfiguration config = WebClient.getConfig(webClient);
    config.getInInterceptors().add(new LoggingInInterceptor());
    config.getOutInterceptors().add(new LoggingOutInterceptor());
    config.getOutInterceptors().add(new JaxRsOutgoingRequestInterceptor());
  }

  public RenderResponse getAuditEventAsPdfA(String insurantId, Boolean signed) {
    var renderResponse = new RenderResponse();
    try {
      var updatedWebClient = updateWebclient(insurantId, signed, pdfAapiPath);
      final Response response = updatedWebClient.accept(APPLICATION_PDF_VALUE).get();
      renderResponse.pdf(response.readEntity(byte[].class)).httpStatusCode(response.getStatus());
    } catch (WebApplicationException e) {
      log.error("HTTP Error while calling getAuditAsPdfA() : " + e.getResponse().getStatus());
      renderResponse.httpStatusCode(e.getResponse().getStatus()).errorMessage(e.getMessage());
    } catch (Exception e) {
      log.error("Error while calling getAuditAsPdfA(): " + e.getMessage());
      renderResponse.httpStatusCode(500).errorMessage(e.getMessage());
    }
    return renderResponse;
  }

  protected WebClient updateWebclient(String insurantId, Boolean signed, String path) {
    return webClient
        .reset()
        .replacePath(path)
        .replaceQuery(String.valueOf(signed))
        .replaceHeader(X_INSURANT_ID, insurantId)
        .replaceHeader(X_USERAGENT, this.userAgent);
  }
}
