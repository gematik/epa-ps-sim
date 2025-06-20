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
package de.gematik.epa.ps.kob.endpoints;

import static de.gematik.epa.ps.kob.util.KobTestdriverAction.mapAction;

import de.gematik.epa.api.psTestdriver.SystemApi;
import de.gematik.epa.api.psTestdriver.dto.Action;
import de.gematik.epa.api.psTestdriver.dto.ResetPrimaersystem;
import de.gematik.epa.ps.kob.services.KobSystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RequiredArgsConstructor
public class SystemApiImpl implements SystemApi {

  private final KobSystemService kobSystemService;

  @Override
  public ResponseEntity<Action> resetPrimaersystem(ResetPrimaersystem resetPrimaersystem) {
    return new ResponseEntity<>(
        mapAction(kobSystemService.reset(resetPrimaersystem)), HttpStatus.OK);
  }
}
