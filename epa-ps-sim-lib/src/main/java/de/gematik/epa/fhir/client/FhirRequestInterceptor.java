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

import static de.gematik.epa.utils.MiscUtils.*;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import de.gematik.epa.utils.HealthRecordProvider;
import de.gematik.epa.utils.InsurantIdHolder;
import de.gematik.epa.utils.TelematikIdHolder;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FhirRequestInterceptor implements IClientInterceptor {

  private static final String X_REQUEST_ID = "X-Request-ID";
  private final String userAgent;

  public FhirRequestInterceptor(String userAgent) {
    this.userAgent = userAgent;
  }

  @Override
  public void interceptRequest(IHttpRequest theRequest) {
    var insurantId = InsurantIdHolder.getInsurantId();
    String targetEndpoint = theRequest.getUri();
    String telematikId = TelematikIdHolder.getTelematikId();
    if (insurantId != null && ENDPOINTS_TO_IGNORE.stream().noneMatch(targetEndpoint::contains)) {
      theRequest.addHeader(X_TARGET_FQDN, HealthRecordProvider.getHealthRecordUrl(insurantId));
      theRequest.addHeader(X_INSURANT_ID, insurantId);
    } else {
      log.warn("No insurantId found in FHIR call!");
    }
    theRequest.addHeader(X_USER_AGENT, this.userAgent);
    theRequest.addHeader(X_REQUEST_ID, UUID.randomUUID().toString());

    if (telematikId != null) {
      theRequest.addHeader(X_ACTOR_ID, telematikId);
    }
  }

  @Override
  public void interceptResponse(IHttpResponse theResponse) throws IOException {
    // nothing for now
  }
}
