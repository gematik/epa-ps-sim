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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.SearchTotalModeEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.InvalidResponseException;
import ca.uhn.fhir.rest.gclient.IClientExecutable;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import de.gematik.epa.api.audit_event.client.RenderApiApi;
import de.gematik.epa.api.testdriver.audit.dto.GetAuditEventResponseDTO;
import de.gematik.epa.client.JaxRsClientWrapper;
import de.gematik.epa.fhir.client.FhirClient;
import de.gematik.epa.unit.util.ResourceLoader;
import de.gematik.epa.utils.FhirUtils;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.SneakyThrows;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;

class AuditEventServiceTest {

  public static final String X_INSURANTID = "X12345678";
  public static final String USERAGENT = "PS-SIM";
  public static final String ACTION = "U";
  private final FhirClient fhirClient = mock(FhirClient.class);
  private final FhirContext context = mock(FhirContext.class);
  private final IParser jsonParser = mock(IParser.class);

  private final JaxRsClientWrapper<RenderApiApi> auditRenderClientWrapper =
      mock(JaxRsClientWrapper.class);
  private RenderApiApi renderApi;

  private AuditEventService auditEventService;
  private static String auditEventAsString;

  @SneakyThrows
  @BeforeAll
  static void setupOnce() {
    auditEventAsString =
        ResourceLoader.readFileContentFromResource("src/test/resources/response/auditEvent.json");
  }

  @BeforeEach
  void setup() {
    when(fhirClient.getContext()).thenReturn(context);
    when(context.newJsonParser()).thenReturn(jsonParser);
    FhirUtils.setJsonParser(jsonParser);
    renderApi = mock(RenderApiApi.class);
    when(auditRenderClientWrapper.getServiceApi()).thenReturn(renderApi);
    when(auditRenderClientWrapper.getUserAgent()).thenReturn(USERAGENT);
    auditEventService = new AuditEventService(fhirClient, auditRenderClientWrapper);
  }

  @Test
  void getAuditEventsShouldReturnAuditEvents() {
    // given
    final var executable = mockGetAuditEvents(AuditEvent.class);
    final Bundle resultBundle = new Bundle();
    final Bundle.BundleEntryComponent bc = new Bundle.BundleEntryComponent();
    final AuditEvent auditEvent = new AuditEvent();
    auditEvent.setOutcome(AuditEvent.AuditEventOutcome._0);
    bc.setResource(auditEvent);

    resultBundle.addEntry(bc);
    when(executable.execute()).thenReturn(resultBundle);
    when(jsonParser.encodeResourceToString(auditEvent)).thenReturn(auditEventAsString);

    // when
    final GetAuditEventResponseDTO response =
        auditEventService.getAuditEvents(
            new AuditEventSearch()
                .xInsurantid(X_INSURANTID)
                .xUseragent("PS-SIM")
                .count(1)
                .offset(1)
                .total("accurate")
                .id("id")
                .lastUpdated(OffsetDateTime.now().minusDays(1))
                .date(OffsetDateTime.now())
                .altid("altid")
                .type("type")
                .action("action")
                .entityName("entityName")
                .outcome("0"));

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getAuditEvents()).hasSize(1);
    assertThat(response.getStatusMessage()).isBlank();

    // validate result with real parser
    AuditEvent auditEventFromResponse = parseAuditEventFromResponse(response);

    String actualAction = String.valueOf(auditEventFromResponse.getAction());
    String actualEntity = auditEventFromResponse.getEntity().get(0).getName();
    String actualAltId = auditEventFromResponse.getAgent().get(0).getAltId();
    String actualAgentName = auditEventFromResponse.getAgent().get(0).getName();
    String actualOutcome = String.valueOf(auditEventFromResponse.getOutcome());
    assertEquals("U", actualAction);
    assertEquals("_0", actualOutcome);
    assertEquals("Arztbrief4711", actualEntity);
    assertEquals("1-883110000092404", actualAltId);
    assertEquals("Praxis Dr. John Doe", actualAgentName);
  }

  @Test
  void getAuditEventsWithActionShouldReturnAuditEvents() {
    // given
    final var executable = mockGetAuditEvents(AuditEvent.class);
    final Bundle resultBundle = new Bundle();
    final Bundle.BundleEntryComponent bc = new Bundle.BundleEntryComponent();
    final AuditEvent auditEvent = new AuditEvent();
    auditEvent.setId("123");
    bc.setResource(auditEvent);
    resultBundle.addEntry(bc);
    when(executable.execute()).thenReturn(resultBundle);

    when(jsonParser.encodeResourceToString(auditEvent)).thenReturn(auditEventAsString);
    // when
    final GetAuditEventResponseDTO response =
        auditEventService.getAuditEvents(
            new AuditEventSearch()
                .xInsurantid(X_INSURANTID)
                .xUseragent(USERAGENT)
                .action(ACTION)
                .count(1)
                .offset(0));

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getAuditEvents()).hasSize(1);
    assertThat(response.getStatusMessage()).isBlank();

    AuditEvent auditEventFromResponse = parseAuditEventFromResponse(response);
    String actualAction = String.valueOf(auditEventFromResponse.getAction());
    assertEquals(ACTION, actualAction);
  }

  @Test
  void getAuditEventsShouldReturnNoSuccessWhenFailure() {
    // given
    final var executable = mockGetAuditEvents(AuditEvent.class);
    when(executable.execute()).thenThrow(InvalidResponseException.class);

    // when
    final GetAuditEventResponseDTO response =
        auditEventService.getAuditEvents(
            new AuditEventSearch()
                .xInsurantid(X_INSURANTID)
                .xUseragent(USERAGENT)
                .count(1)
                .offset(1)
                .total("total")
                .id("id")
                .lastUpdated(OffsetDateTime.now().minusDays(1))
                .date(OffsetDateTime.now())
                .altid("altid")
                .type("type")
                .action("action")
                .entityName("entitytName")
                .outcome("outcome"));

    // then
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getAuditEvents()).isEmpty();
    assertThat(response.getStatusMessage()).isNotBlank();
  }

  @Test
  void getAuditEventsShouldReturnSuccessAndStatusMessageWhenNoAuditsFound() {
    // given
    final var executable = mockGetAuditEvents(AuditEvent.class);
    final var resultBundle = new Bundle();
    when(executable.execute()).thenReturn(resultBundle);

    // when
    AuditEventSearch request = new AuditEventSearch().outcome("0").action("E").count(1).offset(0);
    final GetAuditEventResponseDTO response = auditEventService.getAuditEvents(request);

    // then
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getAuditEvents()).isEmpty();
    assertThat(response.getStatusMessage()).isNotBlank();
  }

  @Test
  void shouldGetAuditEventsAsPdfA() {
    // given
    final byte[] pdfA = new byte[] {1, 2, 3, 4, 5};
    doReturn(Response.ok(pdfA).build())
        .when(renderApi)
        .renderAuditEventsToPDFAuditEventSvc(
            eq(X_INSURANTID),
            eq(USERAGENT),
            any(UUID.class),
            eq(MediaType.APPLICATION_PDF_VALUE),
            anyBoolean(),
            isNull(),
            isNull());

    // when
    var actual = auditEventService.getAuditEventsAsPdfA(X_INSURANTID, true, null, null);
    // then
    assertThat(actual.getSuccess()).isTrue();
    assertThat(actual.getAuditEventAsPdfA()).isEqualTo(pdfA);
  }

  @ParameterizedTest
  @CsvSource(
      value = {
        "NOT_FOUND, '{\"errorCode\":\"noEntries\",\"errorDetail\":\"Rendering led to empty list\"}'",
        "INTERNAL_SERVER_ERROR, 'Unexpected  error. Please report to gematik GmbH: Exceeded limit on max bytes to buffer : 262144'",
        "INTERNAL_SERVER_ERROR, 'null'"
      })
  void getAuditEventsAsPdfAndHandleErrors(Status status, String message) {
    String entity = Objects.equals(message, "null") ? null : message;

    String expectedMessage =
        Objects.equals(message, "null")
            ? "{ \"httpCode\": \"%d\" }".formatted(status.getStatusCode())
            : "{ \"httpCode\": \"%d\", \"details\": %s }"
                .formatted(status.getStatusCode(), message);
    // given
    doReturn(Response.status(status).entity(entity).build())
        .when(renderApi)
        .renderAuditEventsToPDFAuditEventSvc(
            eq(X_INSURANTID),
            eq(USERAGENT),
            any(UUID.class),
            eq(MediaType.APPLICATION_PDF_VALUE),
            anyBoolean(),
            isNull(),
            isNull());

    // when
    var actual = auditEventService.getAuditEventsAsPdfA(X_INSURANTID, true, null, null);

    // then
    assertThat(actual.getSuccess()).isFalse();
    assertThat(actual.getStatusMessage()).isEqualTo(expectedMessage);
    assertThat(actual.getAuditEventAsPdfA()).isNull();
  }

  private <T extends IBaseResource> IClientExecutable<?, Bundle> mockGetAuditEvents(
      final Class<T> resourceClass) {
    final IGenericClient client = mock(IGenericClient.class);
    when(fhirClient.getClient()).thenReturn(client);

    final IUntypedQuery<IBaseBundle> search = mock(IUntypedQuery.class);
    when(client.search()).thenReturn(search);

    final IQuery<IBaseBundle> bundleIQuery = mock(IQuery.class);
    when(search.forResource(resourceClass)).thenReturn(bundleIQuery);

    final IQuery<IBaseBundle> where = mock(IQuery.class);
    when(bundleIQuery.where(any(ICriterion.class))).thenReturn(where);
    when(where.and(any(ICriterion.class))).thenReturn(where);
    when(where.count(anyInt())).thenReturn(where);
    when(where.offset(anyInt())).thenReturn(where);
    when(where.totalMode(any(SearchTotalModeEnum.class))).thenReturn(where);

    final IQuery<Bundle> bundleQuery = mock(IQuery.class);
    when(where.returnBundle(Bundle.class)).thenReturn(bundleQuery);
    when(bundleQuery.encodedJson()).thenReturn(bundleQuery);

    when(bundleQuery.execute()).thenReturn(createMockBundle());

    return bundleQuery;
  }

  private Bundle createMockBundle() {
    final Bundle bundle = new Bundle();
    final Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
    final AuditEvent auditEvent = new AuditEvent();

    entry.setResource(auditEvent);
    bundle.addEntry(entry);
    return bundle;
  }

  private static AuditEvent parseAuditEventFromResponse(GetAuditEventResponseDTO response) {
    FhirContext fhirContext = FhirContext.forR4();
    IParser parser = fhirContext.newJsonParser();

    String auditEventString = response.getAuditEvents().get(0);
    return parser.parseResource(AuditEvent.class, auditEventString);
  }
}
