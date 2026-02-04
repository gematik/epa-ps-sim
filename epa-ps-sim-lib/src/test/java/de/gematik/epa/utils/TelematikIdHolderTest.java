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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TelematikIdHolderTest {

  @BeforeEach
  void clearTelematikIdBeforeEachTest() {
    TelematikIdHolder.clearTelematikId();
  }

  @Test
  void setAndGetTelematikIdReturnsCorrectValue() {
    String expectedTelematikId = "12345";
    TelematikIdHolder.setTelematikId(expectedTelematikId);
    assertThat(TelematikIdHolder.getTelematikId()).isEqualTo(expectedTelematikId);
  }

  @Test
  void clearTelematikIdResetsValueToNull() {
    TelematikIdHolder.setTelematikId("12345");
    TelematikIdHolder.clearTelematikId();
    assertThat(TelematikIdHolder.getTelematikId()).isNull();
  }

  @Test
  void getTelematikIdReturnsNullWhenNotSet() {
    assertThat(TelematikIdHolder.getTelematikId()).isNull();
  }

  @Test
  void setTelematikIdOverwritesPreviousValue() {
    TelematikIdHolder.setTelematikId("12345");
    TelematikIdHolder.setTelematikId("67890");
    assertThat(TelematikIdHolder.getTelematikId()).isEqualTo("67890");
  }
}
