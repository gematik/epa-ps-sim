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
package de.gematik.epa.medication;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.MedicationStatement;

@UtilityClass
public class SearchUtils {

  public static IQuery<IBaseBundle> addContext(String context, IQuery<IBaseBundle> baseQuery) {
    if (StringUtils.isNotEmpty(context)) {
      baseQuery = baseQuery.and(new TokenClientParam("context").exactly().code(context));
    }
    return baseQuery;
  }

  public static IQuery<IBaseBundle> addInclude(String include, IQuery<IBaseBundle> baseQuery) {
    if (StringUtils.isNotEmpty(include)) {
      String[] includes = include.trim().split(",");
      for (String inc : includes) {
        baseQuery = baseQuery.include(new Include(inc.trim()));
      }
    }
    return baseQuery;
  }

  public static IQuery<IBaseBundle> addRevInclude(
      String revinclude, IQuery<IBaseBundle> baseQuery) {
    if (StringUtils.isNotEmpty(revinclude)) {
      String[] revIncludes = revinclude.trim().split(",");
      for (String inc : revIncludes) {
        baseQuery = baseQuery.revInclude(new Include(inc.trim()));
      }
    }
    return baseQuery;
  }

  public static IQuery<IBaseBundle> addLastUpdated(
      String lastUpdated, IQuery<IBaseBundle> baseQuery) {
    if (StringUtils.isNotEmpty(lastUpdated)) {
      baseQuery = baseQuery.lastUpdated(new DateRangeParam(new DateParam(lastUpdated)));
    }
    return baseQuery;
  }

  public static IQuery<IBaseBundle> addRxPrescription(
      String prescription, IQuery<IBaseBundle> baseQuery) {
    if (StringUtils.isNotEmpty(prescription)) {
      if (prescription.contains("|")) {
        String[] parts = prescription.split("\\|", 2);
        baseQuery =
            baseQuery.and(
                new TokenClientParam("rx-prescription")
                    .exactly()
                    .systemAndIdentifier(parts[0], parts[1]));
      } else {
        baseQuery =
            baseQuery.and(
                new TokenClientParam("rx-prescription").exactly().identifier(prescription));
      }
    }
    return baseQuery;
  }

  public static IQuery<IBaseBundle> addIngredientCode(
      String ingredientCode, IQuery<IBaseBundle> baseQuery) {
    if (StringUtils.isNotEmpty(ingredientCode)) {
      if (ingredientCode.contains("|")) {
        String[] parts = ingredientCode.split("\\|", 2);
        baseQuery =
            baseQuery.and(
                new TokenClientParam("ingredient-code")
                    .exactly()
                    .systemAndCode(parts[0], parts[1]));
      } else {
        baseQuery =
            baseQuery.and(new TokenClientParam("ingredient-code").exactly().code(ingredientCode));
      }
    }
    return baseQuery;
  }

  public static IQuery<IBaseBundle> addMedicationRequestMedicationReference(
      String medicationReference, IQuery<IBaseBundle> query) {
    if (StringUtils.isNotEmpty(medicationReference)) {
      query = query.and(MedicationRequest.MEDICATION.hasId(medicationReference));
    }
    return query;
  }

  public static IQuery<IBaseBundle> addMedicationStatementMedicationReference(
      String medicationReference, IQuery<IBaseBundle> query) {
    if (StringUtils.isNotEmpty(medicationReference)) {
      query = query.and(MedicationStatement.MEDICATION.hasId(medicationReference));
    }
    return query;
  }
}
