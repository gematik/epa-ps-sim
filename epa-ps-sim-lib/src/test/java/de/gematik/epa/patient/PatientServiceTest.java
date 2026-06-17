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
package de.gematik.epa.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import de.gematik.epa.api.testdriver.patient.dto.PatientInformationDTO;
import de.gematik.epa.fhir.client.FhirClient;
import de.gematik.epa.utils.FhirUtils;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;

class PatientServiceTest {

  private static final String X_INSURANTID = "X123456789";
  private static final String PATIENT_QUERY_PARAMS =
      "Patient?identifier=http://fhir.de/sid/gkv/kvid-10|" + X_INSURANTID;
  private static final String REQUEST_PATIENT_JSON =
      """
      {
        "resourceType": "Patient",
        "identifier": [
          {
            "system": "http://fhir.de/sid/gkv/kvid-10",
            "value": "X123456789"
          }
        ]
      }
      """;
  private static final String RESPONSE_PATIENT_JSON =
      "{" + "\"resourceType\":\"Patient\",\"id\":\"patient-123\"}";
  private static final String OPERATION_OUTCOME_JSON =
      "{" + "\"resourceType\":\"OperationOutcome\"}";

  private final FhirClient fhirClient = mock(FhirClient.class);
  private final IGenericClient client = mock(IGenericClient.class, Answers.RETURNS_DEEP_STUBS);
  private final IParser jsonParser = mock(IParser.class);

  private PatientService patientService;
  private Patient requestPatient;

  @BeforeEach
  void setUp() {
    when(fhirClient.getClient()).thenReturn(client);
    FhirUtils.setJsonParser(jsonParser);
    patientService = new PatientService(fhirClient);

    requestPatient = new Patient();
    requestPatient.setId("request-patient");
    when(jsonParser.parseResource(REQUEST_PATIENT_JSON)).thenReturn(requestPatient);
  }

  @ParameterizedTest
  @ValueSource(ints = {200, 201})
  void createOrUpdatePatientInformationShouldReturnSuccessForOkAndCreated(int statusCode) {
    var outcome = mock(MethodOutcome.class);
    var returnedPatient = new Patient();
    returnedPatient.setId("patient-123");

    when(client
            .update()
            .resource(eq(requestPatient))
            .conditionalByUrl(eq(PATIENT_QUERY_PARAMS))
            .execute())
        .thenReturn(outcome);
    when(outcome.getResponseStatusCode()).thenReturn(statusCode);
    when(outcome.getResource()).thenReturn(returnedPatient);
    when(jsonParser.encodeResourceToString(returnedPatient)).thenReturn(RESPONSE_PATIENT_JSON);

    var response =
        patientService.createOrUpdatePatientInformation(
            X_INSURANTID, new PatientInformationDTO().patient(REQUEST_PATIENT_JSON));

    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getPatient()).isEqualTo(RESPONSE_PATIENT_JSON);
    assertThat(response.getStatusMessage()).isNull();
  }

  @Test
  void createOrUpdatePatientInformationShouldReturnFailureForNonSuccessStatus() {
    var outcome = mock(MethodOutcome.class);
    var operationOutcome = new OperationOutcome();
    operationOutcome.setId("operation-outcome");

    when(client
            .update()
            .resource(eq(requestPatient))
            .conditionalByUrl(eq(PATIENT_QUERY_PARAMS))
            .execute())
        .thenReturn(outcome);
    when(outcome.getResponseStatusCode()).thenReturn(422);
    when(outcome.getResource()).thenReturn(operationOutcome);
    when(jsonParser.encodeResourceToString(operationOutcome)).thenReturn(OPERATION_OUTCOME_JSON);

    var response =
        patientService.createOrUpdatePatientInformation(
            X_INSURANTID, new PatientInformationDTO().patient(REQUEST_PATIENT_JSON));

    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getPatient()).isNull();
    assertThat(response.getStatusMessage()).contains("\"httpCode\": \"422\"");
    assertThat(response.getStatusMessage()).contains(OPERATION_OUTCOME_JSON);
  }

  @Test
  void createOrUpdatePatientInformationShouldHandleBaseServerResponseException() {
    var exception = mock(BaseServerResponseException.class);

    when(client
            .update()
            .resource(eq(requestPatient))
            .conditionalByUrl(eq(PATIENT_QUERY_PARAMS))
            .execute())
        .thenThrow(exception);
    when(exception.getStatusCode()).thenReturn(409);
    when(exception.getResponseBody()).thenReturn("{\"issue\":\"conflict\"}");

    var response =
        patientService.createOrUpdatePatientInformation(
            X_INSURANTID, new PatientInformationDTO().patient(REQUEST_PATIENT_JSON));

    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getStatusMessage())
        .isEqualTo("{ \"httpCode\": \"409\", \"details\": {\"issue\":\"conflict\"} }");
  }

  @Test
  void createOrUpdatePatientInformationShouldHandleGenericException() {
    when(client
            .update()
            .resource(eq(requestPatient))
            .conditionalByUrl(eq(PATIENT_QUERY_PARAMS))
            .execute())
        .thenThrow(new RuntimeException("boom", new IllegalStateException("cause")));

    var response =
        patientService.createOrUpdatePatientInformation(
            X_INSURANTID, new PatientInformationDTO().patient(REQUEST_PATIENT_JSON));

    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getStatusMessage()).contains("boom");
    assertThat(response.getStatusMessage()).contains("IllegalStateException");
    assertThat(response.getStatusMessage()).contains("cause");
  }
}
