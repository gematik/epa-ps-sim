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
package de.gematik.epa.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.epa.api.information.client.AccountInformationApi;
import de.gematik.epa.client.JaxRsClientWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HealthRecordProviderTest {

  private final JaxRsClientWrapper<AccountInformationApi> client = mock(JaxRsClientWrapper.class);

  @BeforeEach
  void setup() {
    when(client.getUrl()).thenReturn("http://example.com");
  }

  @Test
  void shouldThrowExceptionWhenNoClientForInsurantIdExists() {
    var insurantId = "192021";
    assertThatThrownBy(() -> HealthRecordProvider.getHealthRecordUrl(insurantId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No health record found for insurantId: " + insurantId);
  }

  @Test
  void shouldReturnUrlWhenHealthRecordExists() {
    var insurantId = "123";
    HealthRecordProvider.addHealthRecord(insurantId, client.getUrl());

    String result = HealthRecordProvider.getHealthRecordUrl(insurantId);

    assertThat(result).isEqualTo("http://example.com");
  }

  @Test
  void shouldThrowExceptionWhenNoHealthRecordExists() {
    var insurantId = "456";
    assertThatThrownBy(() -> HealthRecordProvider.getHealthRecordUrl(insurantId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No health record found for insurantId: " + insurantId);
  }

  @Test
  void shouldReturnTrueWhenHealthRecordExists() {
    var insurantId = "789";
    HealthRecordProvider.addHealthRecord(insurantId, client.getUrl());

    boolean result = HealthRecordProvider.hasHealthRecord(insurantId);

    assertThat(result).isTrue();
  }

  @Test
  void shouldReturnFalseWhenNoHealthRecordExists() {
    var insurantId = "101112";

    boolean result = HealthRecordProvider.hasHealthRecord(insurantId);

    assertThat(result).isFalse();
  }

  @Test
  void shouldRemoveHealthRecord() {
    var insurantId = "131415";
    HealthRecordProvider.addHealthRecord(insurantId, client.getUrl());

    HealthRecordProvider.clearHealthRecord(insurantId);

    assertThat(HealthRecordProvider.hasHealthRecord(insurantId)).isFalse();
  }

  @Test
  void shouldAddHealthRecord() {
    var insurantId = "161718";

    HealthRecordProvider.addHealthRecord(insurantId, client.getUrl());

    assertThat(HealthRecordProvider.hasHealthRecord(insurantId)).isTrue();
  }
}
