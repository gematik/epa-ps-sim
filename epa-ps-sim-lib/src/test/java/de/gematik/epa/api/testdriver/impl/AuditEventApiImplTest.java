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
package de.gematik.epa.api.testdriver.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.epa.api.testdriver.audit.dto.GetAuditEventListAsPdfAResponseDTO;
import de.gematik.epa.api.testdriver.audit.dto.GetAuditEventResponseDTO;
import de.gematik.epa.audit.AuditEventService;
import de.gematik.epa.audit.client.AuditRenderClient;
import de.gematik.epa.fhir.client.FhirClient;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AuditEventApiImplTest {

  private final FhirClient fhirClient = mock(FhirClient.class);
  private final FhirContext fhirContext = mock(FhirContext.class);
  private final AuditRenderClient auditRenderClient = mock(AuditRenderClient.class);

  private AuditEventApiImpl auditEventApi;
  private AuditEventService auditEventService;

  @BeforeEach
  public void setup() {
    when(fhirClient.getContext()).thenReturn(fhirContext);
    auditEventService = mock(AuditEventService.class);
    auditEventApi = new AuditEventApiImpl(fhirClient, auditRenderClient);
    auditEventApi.setAuditEventService(auditEventService);
  }

  @Test
  void getAuditEventListAsFHIRTest() {
    // given
    final String auditEvent = "{\n" + "  \"resourceType\": \"AuditEvent\"}";
    final GetAuditEventResponseDTO expectedResponse =
        new GetAuditEventResponseDTO().auditEvents(List.of(auditEvent)).success(true);

    when(auditEventService.getAuditEvents(Mockito.any())).thenReturn(expectedResponse);

    // when
    final GetAuditEventResponseDTO response =
        auditEventApi.getAuditEventListAsFHIR(
            "insurantId", null, null, null, null, null, null, null, null, null, null, null, null);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getAuditEvents()).hasSize(1);
    assertThat(response.getAuditEvents().get(0)).isEqualTo(auditEvent);
    assertThat(response.getStatusMessage()).isBlank();
    assertThat(response).isEqualTo(expectedResponse);
  }

  // TODO:add more tests

  @Test
  void shouldGetAuditEventListAsPdfA() {
    // given
    final byte[] pdf = new byte[0];
    when(auditEventService.getAuditEventsAsPdfA("recordId", true))
        .thenReturn(new GetAuditEventListAsPdfAResponseDTO().auditEventAsPdfA(pdf).success(true));

    // when
    var response = auditEventApi.getAuditEventListAsPdfA("recordId", true);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getAuditEventAsPdfA()).isEqualTo(pdf);
  }
}
