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
package de.gematik.epa.api.testdriver.impl;

import de.gematik.epa.api.testdriver.audit.AuditEventApi;
import de.gematik.epa.api.testdriver.audit.dto.GetAuditEventListAsPdfAResponseDTO;
import de.gematik.epa.api.testdriver.audit.dto.GetAuditEventResponseDTO;
import de.gematik.epa.audit.AuditEventSearch;
import de.gematik.epa.audit.AuditEventService;
import de.gematik.epa.audit.client.AuditRenderClient;
import de.gematik.epa.fhir.client.FhirClient;
import java.time.OffsetDateTime;
import lombok.Setter;

@Setter
public class AuditEventApiImpl implements AuditEventApi {

  private AuditEventService auditEventService;

  public AuditEventApiImpl(final FhirClient fhirClient, final AuditRenderClient auditRenderClient) {
    this.auditEventService = new AuditEventService(fhirClient, auditRenderClient);
  }

  @Override
  public GetAuditEventResponseDTO getAuditEventListAsFHIR(
      String xInsurantid,
      String xUseragent,
      Integer count,
      Integer offset,
      String total,
      String id,
      OffsetDateTime lastUpdated,
      OffsetDateTime date,
      String altid,
      String type,
      String action,
      String entityName,
      String outcome) {

    var searchRequest =
        new AuditEventSearch()
            .xInsurantid(xInsurantid)
            .xUseragent(xUseragent)
            .count(count)
            .offset(offset)
            .total(total)
            .id(id)
            .lastUpdated(lastUpdated)
            .date(date)
            .altid(altid)
            .type(type)
            .action(action)
            .entityName(entityName)
            .outcome(outcome);
    return auditEventService.getAuditEvents(searchRequest);
  }

  @Override
  public GetAuditEventListAsPdfAResponseDTO getAuditEventListAsPdfA(
      String xInsurantid, Boolean signed) {
    return auditEventService.getAuditEventsAsPdfA(xInsurantid, signed);
  }
}
