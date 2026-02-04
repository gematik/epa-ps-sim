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
package de.gematik.epa.utils;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.SearchTotalModeEnum;
import java.util.List;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

@UtilityClass
public class FhirUtils {

  private static final String APPLICATION_FHIR_XML = "application/fhir+xml";
  @Setter private IParser jsonParser;
  @Setter private IParser xmlParser;

  public SearchTotalModeEnum calculateTotalMode(final String total) {
    if (total == null) {
      return SearchTotalModeEnum.NONE;
    } else if (total.equals("accurate")) {
      return SearchTotalModeEnum.ACCURATE;
    } else if (total.equals("estimated")) {
      return SearchTotalModeEnum.ESTIMATED;
    } else {
      return SearchTotalModeEnum.NONE;
    }
  }

  public List<String> extractDataAsJson(Bundle result) {
    return result.getEntry().stream()
        .map(Bundle.BundleEntryComponent::getResource)
        .map(FhirUtils::asJson)
        .toList();
  }

  public List<String> extractDataAsXml(Bundle result) {
    return result.getEntry().stream()
        .map(Bundle.BundleEntryComponent::getResource)
        .map(FhirUtils::asXml)
        .toList();
  }

  public List<String> extractData(Bundle result, String format) {
    if (APPLICATION_FHIR_XML.equals(format)) {
      return extractDataAsXml(result);
    }
    return extractDataAsJson(result);
  }

  public String asJson(Resource resource) {
    return jsonParser.encodeResourceToString(resource);
  }

  public String asXml(Resource resource) {
    return xmlParser.encodeResourceToString(resource);
  }

  public IBaseResource fromString(String resource) {
    return jsonParser.parseResource(resource);
  }
}
