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

import de.gematik.epa.api.testdriver.ConfigurationApi;
import de.gematik.epa.api.testdriver.dto.request.KonnektorConfigurationRequestDTO;
import de.gematik.epa.api.testdriver.dto.response.ResponseDTO;
import de.gematik.epa.konnektor.KonnektorConfigurationProvider;
import de.gematik.epa.konnektor.KonnektorContextProvider;
import de.gematik.epa.konnektor.KonnektorUtils;
import de.gematik.epa.konnektor.cxf.KonnektorInterfacesCxfImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ConfigurationApiImpl implements ConfigurationApi {

  private final KonnektorConfigurationProvider configurationProvider;

  private final KonnektorInterfacesCxfImpl konnektorInterfacesCxf;

  private final KonnektorContextProvider contextProvider;

  @Override
  public ResponseDTO configureKonnektor(KonnektorConfigurationRequestDTO request) {
    try {
      log.info("Running operation configureKonnektor");
      configurationProvider
          .configurationChangeSynchronizer()
          .runBlocking(
              () -> {
                configurationProvider.updateKonnektorConfigurations(request);
                konnektorInterfacesCxf.update(configurationProvider.connection());
                konnektorInterfacesCxf.unlockSmb(contextProvider);
              });
      return new ResponseDTO(true, "Konnektor configuration update completed");
    } catch (Exception e) {
      log.error("Operation configureKonnektor failed with an exception", e);
      return KonnektorUtils.fromThrowable(e);
    }
  }
}
