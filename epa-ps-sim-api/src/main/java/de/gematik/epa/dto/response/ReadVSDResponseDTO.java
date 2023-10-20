/*
 * Copyright 2023 gematik GmbH
 *
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
 */

package de.gematik.epa.dto.response;

import static de.gematik.epa.constants.Documentation.STATUS_MSG_DESCRIPTION;
import static de.gematik.epa.constants.Documentation.SUCCESS_DESCRIPTION;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(
    description =
        "Response der readVSD Abfrage mit dem Ergebnis der Onlineprüfung und Aktualisierung")
public record ReadVSDResponseDTO(
    @JsonProperty(required = true) @Schema(description = SUCCESS_DESCRIPTION) Boolean success,
    @Schema(description = STATUS_MSG_DESCRIPTION) String statusMessage,
    @Schema(
            description =
                "Ergebnis der Onlineprüfung und Aktualisierung"
                    + "1 = Aktualisierung VSD auf eGK durchgeführt"
                    + " 2 = Keine Aktualisierung VSD auf eGK erforderlich")
        int resultOfOnlineCheckEGK) {

  public ReadVSDResponseDTO(ResponseDTO responseDTO) {
    this(responseDTO.success(), responseDTO.statusMessage(), 0);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ReadVSDResponseDTO that = (ReadVSDResponseDTO) o;
    return Objects.equals(success, that.success)
        && Objects.equals(statusMessage, that.statusMessage)
        && Objects.equals(resultOfOnlineCheckEGK, that.resultOfOnlineCheckEGK);
  }

  @Override
  public int hashCode() {
    return Objects.hash(success, statusMessage, resultOfOnlineCheckEGK);
  }

  @Override
  public String toString() {
    return "ReadVSDResponseDTO{"
        + "success="
        + success
        + ", statusMessage='"
        + statusMessage
        + '\''
        + ", resultOfOnlineCheckEGK="
        + resultOfOnlineCheckEGK
        + '}';
  }
}
