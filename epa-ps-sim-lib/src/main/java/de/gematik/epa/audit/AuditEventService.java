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
package de.gematik.epa.audit;

import static de.gematik.epa.utils.StringUtils.appendCauses;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import de.gematik.epa.api.testdriver.audit.dto.GetAuditEventListAsPdfAResponseDTO;
import de.gematik.epa.api.testdriver.audit.dto.GetAuditEventResponseDTO;
import de.gematik.epa.audit.client.AuditRenderClient;
import de.gematik.epa.fhir.client.FhirClient;
import de.gematik.epa.utils.FhirUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.Bundle;

@Slf4j
public class AuditEventService {
  private final FhirClient fhirClient;
  private final AuditRenderClient auditRenderClient;

  public AuditEventService(final FhirClient fhirClient, AuditRenderClient auditRenderClient) {
    this.fhirClient = fhirClient;
    this.auditRenderClient = auditRenderClient;
    FhirUtils.setJsonParser(fhirClient.getContext().newJsonParser());
  }

  public GetAuditEventResponseDTO getAuditEvents(AuditEventSearch searchRequest) {
    try {
      final IGenericClient client = fhirClient.getClient();
      final IUntypedQuery<IBaseBundle> search = client.search();

      // set default values
      int count = searchRequest.count() != null ? searchRequest.count() : 10;
      int offset = searchRequest.offset() != null ? searchRequest.offset() : 0;

      final Bundle result =
          search
              .forResource(AuditEvent.class)
              .where(AuditEvent.TYPE.exactly().code(searchRequest.type()))
              .and(AuditEvent.ACTION.exactly().code(searchRequest.action()))
              .and(AuditEvent.OUTCOME.exactly().code(searchRequest.outcome()))
              .count(count)
              .offset(offset)
              .totalMode(FhirUtils.calculateTotalMode(searchRequest.total()))
              .returnBundle(Bundle.class)
              .execute();

      var response = new GetAuditEventResponseDTO();
      if (result.getEntry().isEmpty()) {
        var statusMessage = "No audit events found for search params: " + searchRequest;
        log.warn(statusMessage);
        return response.success(Boolean.TRUE).statusMessage(statusMessage);
      }

      return response.success(Boolean.TRUE).auditEvents(FhirUtils.extractDataAsJson(result));
    } catch (Exception e) {
      log.error("Error occurred during search for audit events", e);
      return fromThrowableToAuditEventResponse(e);
    }
  }

  private static GetAuditEventResponseDTO fromThrowableToAuditEventResponse(
      @NonNull Throwable throwable) {
    var statusMsgBuilder = new StringBuilder().append(throwable);

    return new GetAuditEventResponseDTO()
        .success(false)
        .statusMessage(appendCauses(throwable, statusMsgBuilder).toString());
  }

  public GetAuditEventListAsPdfAResponseDTO getAuditEventsAsPdfA(
      String xInsurantid, Boolean signed) {
    var response = new GetAuditEventListAsPdfAResponseDTO();
    var renderResponse = auditRenderClient.getAuditEventAsPdfA(xInsurantid, signed);
    response
        .success(renderResponse.httpStatusCode() != 500)
        .auditEventAsPdfA(renderResponse.pdf())
        .statusMessage(renderResponse.errorMessage());
    return response;
  }
}
