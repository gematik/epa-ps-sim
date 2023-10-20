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
import de.gematik.epa.ihe.model.response.RegistryObjectLists;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ergebnisse einer Suche im Aktenkonto")
public record FindObjectsResponseDTO(
    @JsonProperty(required = true) @Schema(description = SUCCESS_DESCRIPTION) Boolean success,
    @Schema(description = STATUS_MSG_DESCRIPTION) String statusMessage,
    @Schema(
            description =
                "Listen von gefunden Dokumenten (bzw. den zugehörigen Metadaten), Foldern, Submission-Sets und Associations")
        RegistryObjectLists registryObjectLists) {}
