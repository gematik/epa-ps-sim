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

import de.gematik.epa.api.testdriver.medication.EmpApi;
import de.gematik.epa.api.testdriver.medication.dto.*;
import de.gematik.epa.medication.EmpService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class EmpApiImpl implements EmpApi {

  private final EmpService empService;

  @Override
  public AddEmpEntryResponseDTO addEmpEntry(
      String insurantId, UUID requestId, String useragent, AddEmpEntryInput addEmpEntryInput) {
    return empService.addEmpEntry(insurantId, requestId, useragent, addEmpEntryInput);
  }

  @Override
  public UpdateEmpEntryResponseDTO updateEmpEntry(
      String insurantId,
      UUID requestId,
      String useragent,
      UpdateEmpEntryInput updateEmpEntryInput) {
    return empService.updateEmpEntry(insurantId, requestId, useragent, updateEmpEntryInput);
  }

  @Override
  public GetMedicationPlanLogsResponseDTO getMedicationPlanLogs(
      String insurantId, UUID requestId, Integer count, Integer offset, String format) {
    return empService.getMedicationPlanLogs(insurantId, requestId, count, offset, format);
  }
}
