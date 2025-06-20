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

import de.gematik.epa.api.testdriver.medication.MedicationApi;
import de.gematik.epa.api.testdriver.medication.dto.*;
import de.gematik.epa.medication.MedicationService;
import de.gematik.epa.medication.MedicationsSearch;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MedicationApiImpl implements MedicationApi {
  private static final int DEFAULT_COUNT = 10;
  private static final int DEFAULT_OFFSET = 0;

  private final MedicationService medicationService;

  @Override
  public GetMedicationResponseDTO getMedicationList(
      String insurantId,
      UUID requestId,
      String useragent,
      Integer count,
      Integer offset,
      String total,
      String id,
      String lastUpdated,
      String identifier,
      String include,
      String revinclude,
      String code,
      String status) {

    if (id == null) {
      var searchRequest =
          new MedicationsSearch()
              .code(code)
              .useragent(useragent)
              .count(Optional.ofNullable(count).orElse(DEFAULT_COUNT))
              .offset(Optional.ofNullable(offset).orElse(DEFAULT_OFFSET))
              .lastUpdated(lastUpdated)
              .status(status)
              .identifier(identifier)
              .code(code)
              .requestID(requestId)
              .include(include)
              .revinclude(revinclude)
              .insurantId(insurantId)
              .total(total);
      return medicationService.searchMedications(searchRequest);
    }
    return medicationService.executeGetById(id);
  }

  @Override
  public GetMedicationListAsPdfResponseDTO getMedicationListAsPdf(String recordId) {
    return medicationService.getEmlAsPdf(recordId);
  }

  @Override
  public GetMedicationListAsXhtmlResponseDTO getMedicationListAsXhtml(String recordId) {
    return medicationService.getEmlAsXhtml(recordId);
  }

  @Override
  public GetEmlAsFhirResponseDTO getEmlAsFhir(
      String insurantId, UUID requestId, String date, Integer count, Integer offset) {
    return medicationService.getEmlAsFhir(requestId, insurantId, date, count, offset);
  }

  public GetMedicationRequestListDTO getMedicationRequests(
      String insurantId,
      UUID requestID,
      String useragent,
      Integer count,
      Integer offset,
      String total,
      String id,
      String lastUpdated,
      String identifier,
      String include,
      String revinclude,
      OffsetDateTime authoredon,
      String status,
      String requester) {
    if (id == null) {
      var medicationRequestsSearch =
          new MedicationsSearch()
              .insurantId(insurantId)
              .requestID(requestID)
              .useragent(useragent)
              .count(Optional.ofNullable(count).orElse(DEFAULT_COUNT))
              .offset(Optional.ofNullable(offset).orElse(DEFAULT_OFFSET))
              .total(total)
              .lastUpdated(lastUpdated)
              .identifier(identifier)
              .include(include)
              .revinclude(revinclude)
              .authoredon(authoredon)
              .status(status)
              .requester(requester);
      return medicationService.searchMedicationRequests(medicationRequestsSearch);
    }
    return medicationService.getMedicationRequestById(id);
  }

  @Override
  public GetMedicationDispenseListDTO getMedicationDispenses(
      String insurantId,
      UUID requestID,
      String useragent,
      Integer count,
      Integer offset,
      String total,
      String id,
      String lastUpdated,
      String identifier,
      String include,
      String revinclude,
      String whenhandedover,
      String prescription,
      String performer,
      String status) {
    if (id == null) {
      var medicationDispensesSearch =
          new MedicationsSearch()
              .insurantId(insurantId)
              .requestID(requestID)
              .useragent(useragent)
              .count(Optional.ofNullable(count).orElse(DEFAULT_COUNT))
              .offset(Optional.ofNullable(offset).orElse(DEFAULT_OFFSET))
              .total(total)
              .lastUpdated(lastUpdated)
              .identifier(identifier)
              .include(include)
              .revinclude(revinclude)
              .whenhandedover(whenhandedover)
              .prescription(prescription)
              .performer(performer)
              .status(status);
      return medicationService.searchMedicationDispenses(medicationDispensesSearch);
    }

    return medicationService.getMedicationDispenseById(id);
  }

  @Override
  public GetMedicationListAsFhirResponseDTO getMedicationListAsFhir(
      String insurantId,
      String useragent,
      Integer count,
      Integer offset,
      String total,
      String status,
      String lastUpdated) {

    var searchRequest =
        new MedicationsSearch()
            .useragent(useragent)
            .count(Optional.ofNullable(count).orElse(DEFAULT_COUNT))
            .offset(Optional.ofNullable(offset).orElse(DEFAULT_OFFSET))
            .status(status)
            .insurantId(insurantId)
            .lastUpdated(lastUpdated)
            .total(total);
    return medicationService.searchForEmlAsFhir(searchRequest);
  }
}
