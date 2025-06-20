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
package de.gematik.epa.medication.client;

import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.epa.api.medication_render.client.dto.EPAOperationOutcome;
import de.gematik.epa.api.medication_render.client.dto.ErrorType;
import de.gematik.epa.client.JaxRsOutgoingRequestInterceptor;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;

@Slf4j
public class EmlRenderClient {

  private static final String X_INSURANT_ID = "x-insurantid";
  private static final String X_USERAGENT = "x-useragent";
  private static final String X_REQUEST_ID = "X-Request-ID";
  private final String pdfApiPath;
  private final String xhtmlApiPath;
  private final String userAgent;
  private final String medicationListPath;
  private final WebClient webClient;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public EmlRenderClient(
      final String apiUrl,
      final String pdfApiPath,
      final String xhtmlApiPath,
      final String userAgent,
      final String medicationListPath) {
    this.pdfApiPath = pdfApiPath;
    this.xhtmlApiPath = xhtmlApiPath;
    this.medicationListPath = medicationListPath;
    this.userAgent = userAgent;
    this.webClient = WebClient.create(apiUrl, true);

    ClientConfiguration config = WebClient.getConfig(webClient);
    config.getInInterceptors().add(new LoggingInInterceptor());
    config.getOutInterceptors().add(new LoggingOutInterceptor());
    config.getOutInterceptors().add(new JaxRsOutgoingRequestInterceptor());
  }

  public RenderResponse getEmlAsXhtml(String insurantId) {
    var renderResponse = new RenderResponse();
    try {
      var webClient = updateWebclient(insurantId, xhtmlApiPath);
      final Response response = webClient.accept(MediaType.TEXT_HTML).get();
      String responseBody = response.readEntity(String.class);

      if (response.getStatus() == 200) {
        renderResponse.xhtml(responseBody).httpStatusCode(response.getStatus());
      } else {
        handleErrorResponse(responseBody, renderResponse, response);
      }
    } catch (WebApplicationException e) {
      handleHttpError(e, renderResponse);
    } catch (Exception e) {
      log.error("Error while calling getEmlAsXhtml(): " + e.getMessage());
      renderResponse.httpStatusCode(500).errorMessage(e.getMessage());
    }
    return renderResponse;
  }

  public RenderResponse getEmlAsPdf(String insurantId) {
    var renderResponse = new RenderResponse();
    try {
      var webClient = updateWebclient(insurantId, pdfApiPath);
      final Response response = webClient.accept(APPLICATION_PDF_VALUE).get();

      if (response.getStatus() == 200) {
        byte[] pdfBytes = response.readEntity(byte[].class);
        renderResponse.pdf(pdfBytes).httpStatusCode(response.getStatus());
      } else {
        String responseBody = response.readEntity(String.class);
        handleErrorResponse(responseBody, renderResponse, response);
      }

    } catch (WebApplicationException e) {
      handleHttpError(e, renderResponse);
    } catch (Exception e) {
      log.error("Error while calling getEmlAsPdf(): " + e.getMessage());
      renderResponse.httpStatusCode(500).errorMessage(e.getMessage());
    }
    return renderResponse;
  }

  public RenderResponse getMedicationList(
      String insurantId,
      String requestId,
      final String date,
      final Integer count,
      final Integer offset) {
    var renderResponse = new RenderResponse();
    try {
      var updatedWebclient = updateWebclient(insurantId, medicationListPath);

      if (StringUtils.isNotBlank(date)) {
        updatedWebclient.replaceQueryParam("date", date);
      }

      if (count != null) {
        updatedWebclient.replaceQueryParam("_count", count);
      }

      if (offset != null) {
        updatedWebclient.replaceQueryParam("_offset", offset);
      }

      final Response response =
          updatedWebclient
              .accept("application/fhir+json")
              .replaceHeader(X_REQUEST_ID, requestId)
              .get();

      String responseBody = response.readEntity(String.class);

      if (response.getStatus() == 200) {
        renderResponse.emlAsFhir(responseBody).httpStatusCode(response.getStatus());
      } else {
        final EPAOperationOutcome operationOutcome = parseOperationOutcome(responseBody);
        renderResponse
            .httpStatusCode(response.getStatus())
            .errorMessage(
                operationOutcome.getIssue() != null
                    ? operationOutcome.getIssue().toString()
                    : "Unknown error");
      }
    } catch (WebApplicationException e) {
      handleHttpError(e, renderResponse);
    } catch (Exception e) {
      log.error("Error while calling getMedicationList(): " + e.getMessage());
      renderResponse.httpStatusCode(500).errorMessage(e.getMessage());
    }
    return renderResponse;
  }

  private EPAOperationOutcome parseOperationOutcome(String responseBody) {
    try {
      return objectMapper.readValue(responseBody, EPAOperationOutcome.class);
    } catch (JsonProcessingException e) {
      log.error("Error while parsing operation outcome: " + e.getMessage());
      return new EPAOperationOutcome();
    }
  }

  private ErrorType parseErrorResponse(String responseBody) {
    try {
      return objectMapper.readValue(responseBody, ErrorType.class);
    } catch (JsonProcessingException e) {
      log.error("Error while parsing error response: " + e.getMessage());
      return new ErrorType();
    }
  }

  private void handleErrorResponse(
      String responseBody, RenderResponse renderResponse, Response response) {
    ErrorType errorType = parseErrorResponse(responseBody);
    renderResponse
        .httpStatusCode(response.getStatus())
        .errorMessage(errorType.getErrorCode() + ", " + errorType.getErrorDetail());
  }

  private void handleHttpError(WebApplicationException e, RenderResponse renderResponse) {
    String errorMessage = e.getMessage();
    if ((e.getResponse().getStatus() == 403
            || e.getResponse().getStatus() == 409
            || e.getResponse().getStatus() == 423)
        && e.getResponse().hasEntity()) {
      String responseBody = e.getResponse().readEntity(String.class);
      ErrorType errorType = parseErrorResponse(responseBody);
      errorMessage = errorType.getErrorCode() + ": " + errorType.getErrorDetail();
    }
    log.error(
        "HTTP Error while calling medication-render-service() : " + e.getResponse().getStatus());
    renderResponse.httpStatusCode(e.getResponse().getStatus()).errorMessage(errorMessage);
  }

  protected WebClient updateWebclient(String insurantId, String path) {
    return webClient
        .replacePath(path)
        .replaceHeader(X_INSURANT_ID, insurantId)
        .replaceHeader(X_USERAGENT, this.userAgent);
  }
}
