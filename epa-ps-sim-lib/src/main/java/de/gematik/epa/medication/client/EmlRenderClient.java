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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.epa.api.medication_render.client.dto.*;
import de.gematik.epa.client.JaxRsOutgoingRequestInterceptor;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.SneakyThrows;
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
  private static final Set<Integer> HTTP_ERRORS_WITH_BODY = Set.of(403, 409, 423);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String FORMAT_QUERY_PARAM = "_format";
  private static final String COUNT_QUERY_PARAM = "_count";
  private static final String OFFSET_QUERY_PARAM = "_offset";
  private static final String ACCEPT_HEADER = "Accept";
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
  private final String medicationPlanPath;
  private final WebClient webClient;
  private final String linkEmpPath;
  private final String unlinkEmpPath;
  private final String batchEmpPath;

  public EmlRenderClient(final EmlRenderClientConfig config) {
    this.pdfApiPath = config.pdfApiPath();
    this.empPdfApiPath = config.empPdfApiPath();
    this.xhtmlApiPath = config.xhtmlApiPath();
    this.medicationListPath = config.medicationListPath();
    this.addEmlEntryPath = config.addEmlEntryPath();
    this.cancelEmlEntryPath = config.cancelEmlEntryPath();
    this.addEmpEntryPath = config.addEmpEntryPath();
    this.updateEmpEntryPath = config.updateEmpEntryPath();
    this.userAgent = config.userAgent();
    this.webClient = WebClient.create(config.apiUrl(), true);
    this.medicationPlanLogPath = config.medicationPlanLogPath();
    this.medicationPlanPath = config.medicationPlanPath();
    this.linkEmpPath = config.linkEmpPath();
    this.unlinkEmpPath = config.unlinkEmpPath();
    this.batchEmpPath = config.batchEmpPath();

    ClientConfiguration cxfConfig = WebClient.getConfig(webClient);
    cxfConfig.getInInterceptors().add(new LoggingInInterceptor());
    cxfConfig.getOutInterceptors().add(new LoggingOutInterceptor());
    cxfConfig.getOutInterceptors().add(new JaxRsOutgoingRequestInterceptor());
  }

  private static void setContentAndAcceptHeaders(String format, WebClient updatedWebclient) {
    var expectedFormat = StringUtils.isNotBlank(format) ? format : APPLICATION_FHIR_JSON;
    updatedWebclient.replaceHeader(CONTENT_TYPE, expectedFormat);
    updatedWebclient.replaceHeader(ACCEPT_HEADER, expectedFormat);
  }

  public RenderResponse getEmlAsXhtml(String insurantId) {
    return executeRequestWithErrorHandling(
        "getEmlAsXhtml",
        () -> {
          var renderResponse = new RenderResponse();
          var wc =
              updateWebclient(insurantId, xhtmlApiPath)
                  .replaceHeader(X_REQUEST_ID, UUID.randomUUID().toString());
          var response = wc.accept(MediaType.TEXT_HTML).get();
          String responseBody = response.readEntity(String.class);
          if (response.getStatus() == 200) {
            renderResponse.xhtml(responseBody).httpStatusCode(response.getStatus());
          } else {
            handleErrorResponse(responseBody, renderResponse, response);
          }
          return renderResponse;
        });
  }

  public RenderResponse getEmlAsPdf(String insurantId) {
    return getPdfResponse(insurantId, pdfApiPath, "getEmlAsPdf");
  }

  public RenderResponse getMedicationList(
      String insurantId,
      String requestId,
      final String date,
      final Integer count,
      final Integer offset,
      final String format) {
    return executeRequestWithErrorHandling(
        "getMedicationList",
        () -> {
          var renderResponse = new RenderResponse();
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
          return renderResponse;
        });
  }

  public RenderResponse addEmlEntry(
      final String insurantId,
      final String requestId,
      final String userAgent,
      final String parameters,
      String encodedOrganization,
      String format) {
    return executeFHIRPostRequest(
        addEmlEntryPath,
        new PostRequest(insurantId, requestId, userAgent, parameters, encodedOrganization, format));
  }

  public RenderResponse cancelEmlEntry(
      final String insurantId,
      final String requestId,
      String encodedOrganization,
      final String id,
      final String userAgent,
      String format) {
    return executeFHIRPostRequest(
        resolvePath(cancelEmlEntryPath, id),
        new PostRequest(insurantId, requestId, userAgent, "", encodedOrganization, format));
  }

  public RenderResponse addEmpEntry(
      final String insurantId,
      final String requestId,
      final String userAgent,
      final String parameters,
      String encodedOrganization,
      String format) {
    return executeFHIRPostRequest(
        addEmpEntryPath,
        new PostRequest(insurantId, requestId, userAgent, parameters, encodedOrganization, format));
  }

  public RenderResponse updateEmpEntry(
      final String insurantId,
      final String requestId,
      final String useragent,
      final String parameters,
      String encodedOrganization,
      String format) {
    return executeFHIRPostRequest(
        updateEmpEntryPath,
        new PostRequest(insurantId, requestId, useragent, parameters, encodedOrganization, format));
  }

  public RenderResponse batchEmp(
      String insurantId,
      String requestId,
      String useragent,
      String parameters,
      String encodedOrganization,
      String format) {
    return executeFHIRPostRequest(
        batchEmpPath,
        new PostRequest(insurantId, requestId, useragent, parameters, encodedOrganization, format));
  }

  public RenderResponse linkEmp(
      final String insurantId,
      final String requestId,
      final String parameters,
      final String encodedOrganization,
      final String id,
      final String userAgent,
      String format) {
    return executeFHIRPostRequest(
        resolvePath(linkEmpPath, id),
        new PostRequest(insurantId, requestId, userAgent, parameters, encodedOrganization, format));
  }

  public RenderResponse unlinkEmp(
      final String insurantId,
      final String requestId,
      final String parameters,
      final String encodedOrganization,
      final String id,
      final String userAgent,
      String format) {
    return executeFHIRPostRequest(
        resolvePath(unlinkEmpPath, id),
        new PostRequest(insurantId, requestId, userAgent, parameters, encodedOrganization, format));
  }

  public RenderResponse getMedicationPlanLogs(
      final String insurantId,
      final String requestId,
      final Integer count,
      final Integer offset,
      final String format) {
    return executeRequestWithErrorHandling(
        "getMedicationPlanLogs",
        () -> {
          var renderResponse = new RenderResponse();
          var updatedWebclient = updateWebclient(insurantId, medicationPlanLogPath);
          setGivenParameters(requestId, count, offset, format, updatedWebclient);
          final Response response = updatedWebclient.get();
          final String responseBody = response.readEntity(String.class);
          if (response.getStatus() == 200) {
            renderResponse.medicationPlanLogs(responseBody).httpStatusCode(response.getStatus());
          } else {
            handleErrorResponse(responseBody, renderResponse, response);
          }
          return renderResponse;
        });
  }

  public RenderResponse getEmp(
      final String format, final String insurantId, final String requestId, String provenanceId) {
    return executeRequestWithErrorHandling(
        "getMedicationPlan",
        () -> {
          var renderResponse = new RenderResponse();
          var updatedWebclient = updateWebclient(insurantId, medicationPlanPath);
          setGivenParameters(requestId, null, null, format, updatedWebclient);
          if (StringUtils.isNotBlank(provenanceId)) {
            updatedWebclient.replaceQueryParam("provenance", provenanceId);
          }
          final Response response = updatedWebclient.get();
          final String responseBody = response.readEntity(String.class);
          if (response.getStatus() == 200) {
            renderResponse.medicationPlan(responseBody).httpStatusCode(response.getStatus());
          } else {
            handleErrorResponse(responseBody, renderResponse, response);
          }
          return renderResponse;
        });
  }

  public RenderResponse getEmpAsPdf(String insurantId) {
    return getPdfResponse(insurantId, empPdfApiPath, "getEmpAsPdf");
  }

  // DO NOT try with resources this autocloseable!
  protected WebClient updateWebclient(String insurantId, String path) {
    return webClient
        .reset()
        .replacePath(path)
        .replaceHeader(X_INSURANT_ID, insurantId)
        .replaceHeader(X_USERAGENT, this.userAgent);
  }

  private RenderResponse executeRequestWithErrorHandling(
      String methodName, Supplier<RenderResponse> action) {
    var renderResponse = new RenderResponse();
    try {
      return action.get();
    } catch (WebApplicationException e) {
      handleHttpError(e, renderResponse);
    } catch (Exception e) {
      log.error("Error while calling {}(): {}", methodName, e.getMessage());
      renderResponse.httpStatusCode(500).errorMessage(e.getMessage());
    }
    return renderResponse;
  }

  private RenderResponse getPdfResponse(String insurantId, String path, String methodName) {
    return executeRequestWithErrorHandling(
        methodName,
        () -> {
          var renderResponse = new RenderResponse();
          var wc =
              updateWebclient(insurantId, path)
                  .replaceHeader(X_REQUEST_ID, UUID.randomUUID().toString());
          handleResponse(renderResponse, wc);
          return renderResponse;
        });
  }

  private String resolvePath(String pathTemplate, String id) {
    return pathTemplate.replace("{id}", id);
  }

  private void setGivenParameters(
      final String requestId,
      final Integer count,
      final Integer offset,
      final String format,
      final WebClient updatedWebclient) {
    if (count != null) {
      updatedWebclient.replaceQueryParam(COUNT_QUERY_PARAM, count);
    }
    if (offset != null) {
      updatedWebclient.replaceQueryParam(OFFSET_QUERY_PARAM, offset);
    }
    if (StringUtils.isNotBlank(format)) {
      updatedWebclient.replaceQueryParam(FORMAT_QUERY_PARAM, format);
    }

    setContentAndAcceptHeaders(format, updatedWebclient);
    updatedWebclient.replaceHeader(X_REQUEST_ID, requestId);
  }

  private RenderResponse executeFHIRPostRequest(String path, PostRequest request) {
    return executeRequestWithErrorHandling(
        "POST " + path,
        () -> {
          var renderResponse = new RenderResponse();
          var updatedWebclient = preparePostRequest(path, request);
          final Response response = updatedWebclient.post(request.parameters());
          handlePostResponse(response, renderResponse);
          return renderResponse;
        });
  }

  private WebClient preparePostRequest(String path, PostRequest request) {
    var updatedWebclient = updateWebclient(request.insurantId(), path);
    updatedWebclient.replaceHeader(X_REQUEST_ID, request.requestId());

    if (StringUtils.isNotBlank(request.userAgent())) {
      updatedWebclient.replaceHeader(X_USERAGENT, request.userAgent());
    }

    setContentAndAcceptHeaders(request.format(), updatedWebclient);

    if (StringUtils.isNotBlank(request.format())) {
      updatedWebclient.replaceQueryParam(FORMAT_QUERY_PARAM, request.format());
    }

    updatedWebclient.replaceHeader(X_REQUESTING_ORGANIZATION, request.encodedOrganization());
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

  @SneakyThrows
  private EPAOperationOutcome parseOperationOutcome(String responseBody) {
    try {
      return OBJECT_MAPPER.readValue(responseBody, EPAOperationOutcome.class);
    } catch (JsonProcessingException e) {
      log.warn("Reading response as JSON tree!", e);
      var tree = OBJECT_MAPPER.readTree(responseBody);
      return new EPAOperationOutcome()
          .resourceType(tree.at("/resourceType").asText(null))
          .meta(OBJECT_MAPPER.treeToValue(tree.at("/meta"), EPAOperationOutcomeMeta.class))
          .issue(
              Stream.of(tree.at("/issue"))
                  .filter(node -> !node.isEmpty())
                  .map(JsonNode::toString)
                  .toList());
    }
  }

  private ErrorType parseErrorResponse(String responseBody) {
    try {
      return OBJECT_MAPPER.readValue(responseBody, ErrorType.class);
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
    if (HTTP_ERRORS_WITH_BODY.contains(e.getResponse().getStatus())
        && e.getResponse().hasEntity()) {
      String responseBody = e.getResponse().readEntity(String.class);
      ErrorType errorType = parseErrorResponse(responseBody);
      errorMessage = errorType.getErrorCode() + ": " + errorType.getErrorDetail();
    }
    log.error(
        "HTTP Error while calling medication-render-service() : {}", e.getResponse().getStatus());
    renderResponse.httpStatusCode(e.getResponse().getStatus()).errorMessage(errorMessage);
  }

  private void handleResponse(RenderResponse renderResponse, WebClient wc) {
    var response = wc.accept(APPLICATION_PDF_VALUE).get();
    if (response.getStatus() == 200) {
      byte[] pdfBytes = response.readEntity(byte[].class);
      renderResponse.pdf(pdfBytes).httpStatusCode(response.getStatus());
    } else {
      String responseBody = response.readEntity(String.class);
      handleErrorResponse(responseBody, renderResponse, response);
    }
  }

  public record EmlRenderClientConfig(
      String apiUrl,
      String pdfApiPath,
      String xhtmlApiPath,
      String userAgent,
      String medicationListPath,
      String addEmlEntryPath,
      String cancelEmlEntryPath,
      String addEmpEntryPath,
      String updateEmpEntryPath,
      String empPdfApiPath,
      String medicationPlanLogPath,
      String medicationPlanPath,
      String linkEmpPath,
      String unlinkEmpPath,
      String batchEmpPath) {}

  private record PostRequest(
      String insurantId,
      String requestId,
      String userAgent,
      String parameters,
      String encodedOrganization,
      String format) {}
}
