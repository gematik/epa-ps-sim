/*-
 * #%L
 * epa-ps-sim-app
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
package de.gematik.epa.ps.document.config;

import de.gematik.epa.api.testdriver.config.AddressConfig;
import de.gematik.epa.document.config.DocumentConnectionConfigurationMutable;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@Data
@RequiredArgsConstructor
@Accessors(fluent = true)
public class DocumentConnectionConfigurationData implements DocumentConnectionConfigurationMutable {
  private AddressConfig address;

  @ConstructorBinding
  public DocumentConnectionConfigurationData(AddressConfig address) {
    this.address = address;
  }
}
