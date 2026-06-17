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

import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
@ToString
public class MedicationStatementSearch {
  private String insurantId;
  private String useragent;
  private UUID requestId;
  private Integer count;
  private Integer offset;
  private String total;
  private String id;
  private String lastUpdated;
  private String include;
  private String revinclude;
  private String format;
  private String medicationReference;
  private String status;
  private LocalDate effective;
  private String prescription;
  private String derivedFrom;
  private String context;
  private String basedOnEmp;
  private String versionId;
}
