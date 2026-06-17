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

import de.gematik.epa.api.testdriver.medication.MedicationStatementApi;
import de.gematik.epa.api.testdriver.medication.dto.*;
import de.gematik.epa.medication.MedicationStatementSearch;
import de.gematik.epa.medication.MedicationStatementService;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MedicationStatementApiImpl implements MedicationStatementApi {
  private static final int DEFAULT_COUNT = 10;
  private static final int DEFAULT_OFFSET = 0;
  private final MedicationStatementService medicationStatementService;

  @Override
  public AddEmlEntryResponseDTO addEmlEntry(
      String insurantId, UUID requestId, String useragent, AddEmlEntryInput addEmlEntryInput) {
    return medicationStatementService.addEmlEntry(
        insurantId, requestId, useragent, addEmlEntryInput);
  }

  @Override
  public CancelEmlEntryResponseDTO cancelEmlEntry(
      String insurantId,
      UUID requestId,
      String medicationStatementId,
      String useragent,
      CancelEmlEntryInput cancelEmlEntryInput) {
    return medicationStatementService.cancelEmlEntry(
        insurantId, requestId, medicationStatementId, useragent, cancelEmlEntryInput);
  }

  @Override
  public GetMedicationStatementHistoryByIdAndVersionResponseDTO
      getMedicationStatementHistoryByIdAndVersion(
          String insurantId,
          UUID requestId,
          String id,
          String versionId,
          String useragent,
          String format) {

    var searchRequest =
        new MedicationStatementSearch()
            .id(id)
            .insurantId(insurantId)
            .requestId(requestId)
            .useragent(useragent)
            .versionId(versionId)
            .format(format);
    return medicationStatementService.getMedicationStatementHistoryById(searchRequest);
  }

  @Override
  public GetMedicationStatementHistoryResponseDTO getMedicationStatementHistoryList(
      String insurantId, UUID requestId, String id, String useragent, String format) {
    var searchRequest =
        new MedicationStatementSearch()
            .id(id)
            .insurantId(insurantId)
            .requestId(requestId)
            .useragent(useragent)
            .format(format);
    return medicationStatementService.searchMedicationStatementHistory(searchRequest);
  }

  @Override
  public GetMedicationStatementListDTO getMedicationStatements(
      String insurantId,
      UUID requestId,
      String useragent,
      Integer count,
      Integer offset,
      String total,
      String id,
      String lastUpdated,
      String include,
      String revinclude,
      String format,
      String medication,
      String status,
      LocalDate effective,
      String prescription,
      String derivedFrom,
      String context,
      String basedOnEmp) {

    if (id == null) {
      var searchRequest =
          new MedicationStatementSearch()
              .insurantId(insurantId)
              .requestId(requestId)
              .useragent(useragent)
              .count(Optional.ofNullable(count).orElse(DEFAULT_COUNT))
              .offset(Optional.ofNullable(offset).orElse(DEFAULT_OFFSET))
              .total(total)
              .lastUpdated(lastUpdated)
              .include(include)
              .revinclude(revinclude)
              .format(format)
              .medicationReference(medication)
              .status(status)
              .effective(effective)
              .prescription(prescription)
              .derivedFrom(derivedFrom)
              .context(context)
              .basedOnEmp(basedOnEmp);
      return medicationStatementService.searchMedicationStatements(searchRequest);
    }
    return medicationStatementService.executeGetById(id);
  }

  @Override
  public LinkEmpResponseDTO linkEmp(
      String insurantId,
      UUID requestId,
      String medicationStatementId,
      String useragent,
      LinkEmpInput linkEmpInput) {
    return medicationStatementService.linkEmp(
        insurantId, requestId, medicationStatementId, useragent, linkEmpInput);
  }

  @Override
  public UnlinkEmpResponseDTO unlinkEmp(
      String insurantId,
      UUID requestId,
      String medicationStatementId,
      String useragent,
      UnlinkEmpInput unlinkEmpInput) {

    return medicationStatementService.unlinkEmp(
        insurantId, requestId, medicationStatementId, useragent, unlinkEmpInput);
  }
}
