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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.param.DateRangeParam;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.junit.jupiter.api.Test;

class SearchUtilsTest {

  @Test
  void addContextShouldAddTokenParamWhenContextIsNotEmpty() {
    IQuery<IBaseBundle> query = mock(IQuery.class);
    IQuery<IBaseBundle> expected = mock(IQuery.class);
    when(query.and(any(ICriterion.class))).thenReturn(expected);

    IQuery<IBaseBundle> result = SearchUtils.addContext("EMP", query);

    assertThat(result).isEqualTo(expected);
    verify(query).and(any(ICriterion.class));
  }

  @Test
  void addContextShouldReturnOriginalQueryWhenContextIsEmpty() {
    IQuery<IBaseBundle> query = mock(IQuery.class);

    IQuery<IBaseBundle> result = SearchUtils.addContext("", query);

    assertThat(result).isEqualTo(query);
    verify(query, never()).and(any(ICriterion.class));
  }

  @Test
  void addIncludeShouldAddIncludeWhenIncludeIsNotEmpty() {
    IQuery<IBaseBundle> query = mock(IQuery.class);
    IQuery<IBaseBundle> expected = mock(IQuery.class);
    when(query.include(any(Include.class))).thenReturn(expected);

    IQuery<IBaseBundle> result = SearchUtils.addInclude("MedicationRequest:medication", query);

    assertThat(result).isEqualTo(expected);
    verify(query).include(any(Include.class));
  }

  @Test
  void addIncludeShouldReturnOriginalQueryWhenIncludeIsEmpty() {
    IQuery<IBaseBundle> query = mock(IQuery.class);

    IQuery<IBaseBundle> result = SearchUtils.addInclude("", query);

    assertThat(result).isEqualTo(query);
    verify(query, never()).include(any(Include.class));
  }

  @Test
  void addRevIncludeShouldAddRevIncludeWhenRevIncludeIsNotEmpty() {
    IQuery<IBaseBundle> query = mock(IQuery.class);
    IQuery<IBaseBundle> expected = mock(IQuery.class);
    when(query.revInclude(any(Include.class))).thenReturn(expected);

    IQuery<IBaseBundle> result = SearchUtils.addRevInclude("MedicationRequest:medication", query);

    assertThat(result).isEqualTo(expected);
    verify(query).revInclude(any(Include.class));
  }

  @Test
  void addRevIncludeShouldReturnOriginalQueryWhenRevIncludeIsEmpty() {
    IQuery<IBaseBundle> query = mock(IQuery.class);

    IQuery<IBaseBundle> result = SearchUtils.addRevInclude("", query);

    assertThat(result).isEqualTo(query);
    verify(query, never()).revInclude(any(Include.class));
  }

  @Test
  void addLastUpdatedShouldAddDateRangeParamWhenLastUpdatedIsNotEmpty() {
    IQuery<IBaseBundle> query = mock(IQuery.class);
    IQuery<IBaseBundle> expected = mock(IQuery.class);
    when(query.lastUpdated(any(DateRangeParam.class))).thenReturn(expected);

    IQuery<IBaseBundle> result = SearchUtils.addLastUpdated("gt2021-01-01", query);

    assertThat(result).isEqualTo(expected);
    verify(query).lastUpdated(any(DateRangeParam.class));
  }

  @Test
  void addLastUpdatedShouldReturnOriginalQueryWhenLastUpdatedIsEmpty() {
    IQuery<IBaseBundle> query = mock(IQuery.class);

    IQuery<IBaseBundle> result = SearchUtils.addLastUpdated("", query);

    assertThat(result).isEqualTo(query);
    verify(query, never()).lastUpdated(any(DateRangeParam.class));
  }

  @Test
  void addRxPrescriptionShouldAddTokenParamWhenPrescriptionIsValueOnly() {
    IQuery<IBaseBundle> query = mock(IQuery.class);
    IQuery<IBaseBundle> expected = mock(IQuery.class);
    when(query.and(any(ICriterion.class))).thenReturn(expected);

    IQuery<IBaseBundle> result = SearchUtils.addRxPrescription("160.000.000.000.000.00", query);

    assertThat(result).isEqualTo(expected);
    verify(query).and(any(ICriterion.class));
  }

  @Test
  void addRxPrescriptionShouldAddSystemAndValueWhenPrescriptionContainsPipe() {
    IQuery<IBaseBundle> query = mock(IQuery.class);
    IQuery<IBaseBundle> expected = mock(IQuery.class);
    when(query.and(any(ICriterion.class))).thenReturn(expected);

    IQuery<IBaseBundle> result =
        SearchUtils.addRxPrescription(
            "https://gematik.de/fhir/sid/erp-prescription-id|160.000.000.000.000.00", query);

    assertThat(result).isEqualTo(expected);
    verify(query).and(any(ICriterion.class));
  }

  @Test
  void addRxPrescriptionShouldReturnOriginalQueryWhenPrescriptionIsEmpty() {
    IQuery<IBaseBundle> query = mock(IQuery.class);

    IQuery<IBaseBundle> result = SearchUtils.addRxPrescription("", query);

    assertThat(result).isEqualTo(query);
    verify(query, never()).and(any(ICriterion.class));
  }

  @Test
  void addIngredientCodeShouldAddTokenParamWhenIngredientCodeIsValueOnly() {
    IQuery<IBaseBundle> query = mock(IQuery.class);
    IQuery<IBaseBundle> expected = mock(IQuery.class);
    when(query.and(any(ICriterion.class))).thenReturn(expected);

    IQuery<IBaseBundle> result = SearchUtils.addIngredientCode("24421", query);

    assertThat(result).isEqualTo(expected);
    verify(query).and(any(ICriterion.class));
  }

  @Test
  void addIngredientCodeShouldAddSystemAndCodeWhenIngredientCodeContainsPipe() {
    IQuery<IBaseBundle> query = mock(IQuery.class);
    IQuery<IBaseBundle> expected = mock(IQuery.class);
    when(query.and(any(ICriterion.class))).thenReturn(expected);

    IQuery<IBaseBundle> result =
        SearchUtils.addIngredientCode("http://fhir.de/CodeSystem/ask|24421", query);

    assertThat(result).isEqualTo(expected);
    verify(query).and(any(ICriterion.class));
  }

  @Test
  void addIngredientCodeShouldReturnOriginalQueryWhenIngredientCodeIsEmpty() {
    IQuery<IBaseBundle> query = mock(IQuery.class);

    IQuery<IBaseBundle> result = SearchUtils.addIngredientCode("", query);

    assertThat(result).isEqualTo(query);
    verify(query, never()).and(any(ICriterion.class));
  }

  @Test
  void
      addMedicationReferenceShouldAddCriterionWhenMedicationRequestMedicationReferenceIsNotEmpty() {
    IQuery<IBaseBundle> query = mock(IQuery.class);
    IQuery<IBaseBundle> expected = mock(IQuery.class);
    when(query.and(any(ICriterion.class))).thenReturn(expected);

    IQuery<IBaseBundle> result =
        SearchUtils.addMedicationRequestMedicationReference("Medication/123", query);

    assertThat(result).isEqualTo(expected);
    verify(query).and(any(ICriterion.class));
  }

  @Test
  void
      addMedicationReferenceShouldReturnOriginalQueryWhenMedicationRequestMedicationReferenceIsEmpty() {
    IQuery<IBaseBundle> query = mock(IQuery.class);

    IQuery<IBaseBundle> result = SearchUtils.addMedicationRequestMedicationReference("", query);

    assertThat(result).isEqualTo(query);
    verify(query, never()).and(any(ICriterion.class));
  }
}
