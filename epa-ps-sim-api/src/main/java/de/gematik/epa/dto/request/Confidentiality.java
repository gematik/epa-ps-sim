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

package de.gematik.epa.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import java.util.Objects;

@Schema(description = "Vertraulichkeitsstufen von Dokumenten")
public enum Confidentiality {
  NORMAL,
  EXTENDED;

  @JsonCreator
  public static Confidentiality fromValue(String value) {
    return Arrays.stream(Confidentiality.values())
        .filter(v -> v.getName().equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "No confidentiality known with value " + Objects.toString(value, "null")));
  }

  @JsonValue
  public String getName() {
    return this.name().toLowerCase();
  }
}
