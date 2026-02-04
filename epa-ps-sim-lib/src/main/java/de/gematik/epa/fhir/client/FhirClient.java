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

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class FhirClient {

  private String serverUrl;
  private FhirContext context;
  private IGenericClient client;

  public FhirClient(String serverUrl, String userAgent) {
    setServerUrl(serverUrl, userAgent);
  }

  public void setServerUrl(String serverUrl, String userAgent) {
    log.debug("Initializing FHIR Client setup for: {}", this.serverUrl);
    this.serverUrl = serverUrl;
    context = FhirContext.forR4();
    context.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
    client = context.newRestfulGenericClient(this.serverUrl);
    client.registerInterceptor(new LoggingInterceptor());
    client.registerInterceptor(new FhirRequestInterceptor(userAgent));
  }

  public void customizeSocketTimeout(int timeoutInMs) {
    this.context.getRestfulClientFactory().setSocketTimeout(timeoutInMs);
    // after changing the socket timeout, we need to re-create the client
    client = context.newRestfulGenericClient(this.serverUrl);
  }
}
