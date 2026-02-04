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
package de.gematik.epa.medication.client;

import static org.springframework.http.MediaType.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.epa.api.medication_render.client.dto.*;
import de.gematik.epa.client.JaxRsOutgoingRequestInterceptor;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;

@Slf4j
public class EmlRenderClient {

  public static final String APPLICATION_FHIR_JSON = "application/fhir+json";
  public static final String APPLICATION_FHIR_XML = "application/fhir+xml";
  public static final List<String> ALLOWED_FHIR_FORMATS =
      List.of(APPLICATION_FHIR_XML, APPLICATION_FHIR_JSON);
  private static final String X_INSURANT_ID = "x-insurantid";
  private static final String X_USERAGENT = "x-useragent";
  private static final String X_REQUEST_ID = "X-Request-ID";
  private static final String X_REQUESTING_ORGANIZATION = "X-Requesting-Organization";
  private static final String CONTENT_TYPE = "Content-Type";
  private final String pdfApiPath;
  private final String empPdfApiPath;
  private final String xhtmlApiPath;
  private final String userAgent;
  private final String medicationListPath;
  private final String addEmlEntryPath;
  private final String cancelEmlEntryPath;
  private final String addEmpEntryPath;
  private final String updateEmpEntryPath;
  private final String medicationPlanLogPath;
  private final WebClient webClient;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private String linkEmpPath;

  public EmlRenderClient(
      final String apiUrl,
      final String pdfApiPath,
      final String xhtmlApiPath,
      final String userAgent,
      final String medicationListPath,
      final String addEmlEntryPath,
      final String cancelEmlEntryPath,
      final String addEmpEntryPath,
      final String updateEmpEntryPath,
      final String empPdfApiPath,
      final String medicationPlanLogPath,
      final String linkEmpPath) {
    this.pdfApiPath = pdfApiPath;
    this.empPdfApiPath = empPdfApiPath;
    this.xhtmlApiPath = xhtmlApiPath;
    this.medicationListPath = medicationListPath;
    this.addEmlEntryPath = addEmlEntryPath;
    this.cancelEmlEntryPath = cancelEmlEntryPath;
    this.addEmpEntryPath = addEmpEntryPath;
    this.updateEmpEntryPath = updateEmpEntryPath;
    this.userAgent = userAgent;
    this.webClient = WebClient.create(apiUrl, true);
    this.medicationPlanLogPath = medicationPlanLogPath;
    this.linkEmpPath = linkEmpPath;

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
      log.error("Error while calling getEmlAsXhtml(): {}", e.getMessage());
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
      log.error("Error while calling getEmlAsPdf(): {}", e.getMessage());
      renderResponse.httpStatusCode(500).errorMessage(e.getMessage());
    }
    return renderResponse;
  }

  public RenderResponse getMedicationList(
      String insurantId,
      String requestId,
      final String date,
      final Integer count,
      final Integer offset,
      final String format) {
    var renderResponse = new RenderResponse();
    try {
      var updatedWebclient = updateWebclient(insurantId, medicationListPath);

      if (StringUtils.isNotBlank(date)) {
        updatedWebclient.replaceQueryParam("date", date);
      }

      setGivenParameters(requestId, count, offset, format, updatedWebclient);
      final Response response = updatedWebclient.get();
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
      log.error("Error while calling getMedicationList(): {}", e.getMessage());
      renderResponse.httpStatusCode(500).errorMessage(e.getMessage());
    }
    return renderResponse;
  }

  public RenderResponse addEmlEntry(
      final String insurantId,
      final String requestId,
      final String userAgent,
      final String parameters,
      String encodedOrganization,
      String format) {
    return doPost(
        addEmlEntryPath, requestId, insurantId, userAgent, encodedOrganization, parameters, format);
  }

  public RenderResponse cancelEmlEntry(
      final String insurantId,
      final String requestId,
      String encodedOrganization,
      final String id,
      final String userAgent,
      String format) {
    String path = cancelEmlEntryPath.replace("{id}", id);
    return doPost(path, requestId, insurantId, userAgent, encodedOrganization, "", format);
  }

  public RenderResponse addEmpEntry(
      final String insurantId,
      final String requestId,
      final String userAgent,
      final String parameters,
      String encodedOrganization,
      String format) {
    return doPost(
        addEmpEntryPath, requestId, insurantId, userAgent, encodedOrganization, parameters, format);
  }

  public RenderResponse updateEmpEntry(
      final String insurantId,
      final String requestId,
      final String userAgent,
      final String parameters,
      String encodedOrganization,
      String format) {
    return doPost(
        updateEmpEntryPath,
        requestId,
        insurantId,
        userAgent,
        encodedOrganization,
        parameters,
        format);
  }

  public RenderResponse linkEmp(
      final String insurantId,
      final String requestId,
      final String parameters,
      final String encodedOrganization,
      final String id,
      final String userAgent,
      String format) {
    var path = linkEmpPath.replace("{id}", id);
    return doPost(path, requestId, insurantId, userAgent, encodedOrganization, parameters, format);
  }

  public RenderResponse getMedicationPlanLogs(
      final String insurantId,
      final String requestId,
      final Integer count,
      final Integer offset,
      final String format) {
    var renderResponse = new RenderResponse();
    try {
      var updatedWebclient = updateWebclient(insurantId, medicationPlanLogPath);

      setGivenParameters(requestId, count, offset, format, updatedWebclient);
      final Response response = updatedWebclient.get();
      final String responseBody = response.readEntity(String.class);

      if (response.getStatus() == 200) {
        renderResponse.medicationPlanLogs(responseBody).httpStatusCode(response.getStatus());
      } else {
        handleErrorResponse(responseBody, renderResponse, response);
      }
    } catch (WebApplicationException e) {
      handleHttpError(e, renderResponse);
    } catch (Exception e) {
      log.error("Error while calling getMedicationPlanLogs(): {}", e.getMessage());
      renderResponse.httpStatusCode(500).errorMessage(e.getMessage());
    }
    return renderResponse;
  }

  private void setGivenParameters(
      final String requestId,
      final Integer count,
      final Integer offset,
      final String format,
      final WebClient updatedWebclient) {
    if (count != null) {
      updatedWebclient.replaceQueryParam("_count", count);
    }

    if (offset != null) {
      updatedWebclient.replaceQueryParam("_offset", offset);
    }

    if (StringUtils.isNotBlank(format)) {
      updatedWebclient.replaceQueryParam("_format", format);
      updatedWebclient.accept(format);
    } else {
      updatedWebclient.accept(APPLICATION_FHIR_JSON);
    }

    updatedWebclient.replaceHeader(X_REQUEST_ID, requestId);
  }

  private RenderResponse doPost(
      String path,
      String requestId,
      String insurantId,
      String userAgent,
      String encodedOrganization,
      String parameters,
      String format) {
    var renderResponse = new RenderResponse();
    try {
      var updatedWebclient =
          preparePostRequest(path, requestId, insurantId, userAgent, encodedOrganization, format);

      final Response response = updatedWebclient.post(parameters);
      handlePostResponse(response, renderResponse);

    } catch (WebApplicationException e) {
      handleHttpError(e, renderResponse);
    } catch (Exception e) {
      log.error(
          "Error in POST to {} for insurantId: {}, message: {}", path, insurantId, e.getMessage());
      renderResponse.httpStatusCode(500).errorMessage(e.getMessage());
    }
    return renderResponse;
  }

  private WebClient preparePostRequest(
      String path,
      String requestId,
      String insurantId,
      String userAgent,
      String encodedOrganization,
      String format) {
    var updatedWebclient = updateWebclient(insurantId, path);
    updatedWebclient.replaceHeader(X_REQUEST_ID, requestId);

    if (StringUtils.isNotBlank(userAgent)) {
      updatedWebclient.replaceHeader(X_USERAGENT, userAgent);
    }

    String contentType = StringUtils.isNotBlank(format) ? format : APPLICATION_FHIR_JSON;
    updatedWebclient.replaceHeader(CONTENT_TYPE, contentType);
    updatedWebclient.replaceHeader("Accept", contentType);

    if (StringUtils.isNotBlank(format)) {
      updatedWebclient.replaceQueryParam("_format", format);
    }

    updatedWebclient.replaceHeader(X_REQUESTING_ORGANIZATION, encodedOrganization);
    return updatedWebclient;
  }

  private void handlePostResponse(Response response, RenderResponse renderResponse) {
    String responseBody = response.readEntity(String.class);

    if (response.getStatus() == 200) {
      renderResponse.empResponse(responseBody).httpStatusCode(response.getStatus());
    } else if (response.getStatus() >= 400 && response.getStatus() <= 423) {
      handleClientError(responseBody, renderResponse, response);
    } else {
      handleErrorResponse(responseBody, renderResponse, response);
    }
  }

  private void handleClientError(
      String responseBody, RenderResponse renderResponse, Response response) {
    final EPAOperationOutcome operationOutcome = parseOperationOutcome(responseBody);
    if (operationOutcome.getIssue() == null || operationOutcome.getIssue().isEmpty()) {
      handleErrorResponse(responseBody, renderResponse, response);
    } else {
      renderResponse
          .httpStatusCode(response.getStatus())
          .errorMessage(operationOutcome.getIssue().toString());
    }
  }

  private EPAOperationOutcome parseOperationOutcome(String responseBody) {
    try {
      return objectMapper.readValue(responseBody, EPAOperationOutcome.class);
    } catch (JsonProcessingException e) {
      log.error("Error while parsing operation outcome: {}", e.getMessage());
      return new EPAOperationOutcome();
    }
  }

  private ErrorType parseErrorResponse(String responseBody) {
    try {
      return objectMapper.readValue(responseBody, ErrorType.class);
    } catch (JsonProcessingException e) {
      log.error("Error while parsing error response: {}", e.getMessage());
      return new ErrorType().errorDetail(responseBody).errorCode("Unknown");
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
        "HTTP Error while calling medication-render-service() : {}", e.getResponse().getStatus());
    renderResponse.httpStatusCode(e.getResponse().getStatus()).errorMessage(errorMessage);
  }

  protected WebClient updateWebclient(String insurantId, String path) {
    return webClient
        .reset()
        .replacePath(path)
        .replaceHeader(X_INSURANT_ID, insurantId)
        .replaceHeader(X_USERAGENT, this.userAgent);
  }

  public RenderResponse getEmpAsPdf(String insurantId) {
    RenderResponse renderResp = new RenderResponse();
    try {
      WebClient webClient = updateWebclient(insurantId, empPdfApiPath);
      final Response resp = webClient.accept(APPLICATION_PDF_VALUE).get();
      if (resp.getStatus() == 200) {
        byte[] pdfByteArray = resp.readEntity(byte[].class);
        renderResp.pdf(pdfByteArray).httpStatusCode(resp.getStatus());
      } else {
        String respBody = resp.readEntity(String.class);
        handleErrorResponse(respBody, renderResp, resp);
      }
    } catch (WebApplicationException e) {
      handleHttpError(e, renderResp);
    } catch (Exception e) {
      log.error("getEmpAsPdf() throws error: {}", e.getMessage());
      renderResp.httpStatusCode(500).errorMessage(e.getMessage());
    }
    return renderResp;
  }
}
