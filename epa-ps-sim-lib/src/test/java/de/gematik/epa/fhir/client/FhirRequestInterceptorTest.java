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
package de.gematik.epa.fhir.client;

import static de.gematik.epa.utils.MiscUtils.X_ACTOR_ID;
import static de.gematik.epa.utils.MiscUtils.X_INSURANT_ID;
import static de.gematik.epa.utils.MiscUtils.X_TARGET_FQDN;
import static org.mockito.Mockito.*;

import ca.uhn.fhir.rest.client.api.IHttpRequest;
import de.gematik.epa.api.information.client.AccountInformationApi;
import de.gematik.epa.client.JaxRsClientWrapper;
import de.gematik.epa.utils.HealthRecordProvider;
import de.gematik.epa.utils.InsurantIdHolder;
import de.gematik.epa.utils.TelematikIdHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FhirRequestInterceptorTest {

  private final IHttpRequest request = mock(IHttpRequest.class);
  private final String userAgent = "PS-SIM";
  private final JaxRsClientWrapper client =
      new JaxRsClientWrapper<>("http://localhost:8080", userAgent, AccountInformationApi.class);
  private final FhirRequestInterceptor interceptor = new FhirRequestInterceptor(userAgent);

  @BeforeEach
  void setup() {
    TelematikIdHolder.clearTelematikId();
  }

  @Test
  void shouldAddInsurantIdToHeadersWhenPresent() {
    when(request.getUri()).thenReturn("someEndpoint");

    var insurantId = "12345";
    InsurantIdHolder.setInsurantId(insurantId);
    HealthRecordProvider.addHealthRecord(insurantId, client.getUrl());
    interceptor.interceptRequest(request);

    verify(request).addHeader(X_TARGET_FQDN, HealthRecordProvider.getHealthRecordUrl(insurantId));
  }

  @Test
  void shouldAddTelematikIdToHeadersWhenPresent() {
    when(request.getUri()).thenReturn("someEndpoint");

    var telematikId = "12345";
    TelematikIdHolder.setTelematikId(telematikId);
    interceptor.interceptRequest(request);

    verify(request).addHeader(X_ACTOR_ID, TelematikIdHolder.getTelematikId());
    TelematikIdHolder.clearTelematikId();
  }

  @Test
  void shouldOnlySetRelevantHeaders() {
    when(request.getUri()).thenReturn("someEndpoint");

    InsurantIdHolder.setInsurantId(null);
    interceptor.interceptRequest(request);

    verify(request, times(0)).addHeader(X_INSURANT_ID, InsurantIdHolder.getInsurantId());
    verify(request, times(0)).addHeader(X_ACTOR_ID, TelematikIdHolder.getTelematikId());
    verify(request, times(0)).addHeader(eq(X_TARGET_FQDN), any());
  }
}
