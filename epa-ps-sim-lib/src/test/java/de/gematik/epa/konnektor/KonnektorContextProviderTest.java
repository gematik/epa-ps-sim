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
package de.gematik.epa.konnektor;

import static de.gematik.epa.unit.util.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import de.gematik.epa.unit.util.TestBase;
import de.gematik.epa.unit.util.TestDataFactory;
import java.util.MissingResourceException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class KonnektorContextProviderTest extends TestBase {

  @SneakyThrows
  @Test
  void contextProviderCacheTest() {
    var tstObj =
        new KonnektorContextProvider(
            konnektorConfigurationProvider(), konnektorInterfaceAssembly());

    var firstResult = assertDoesNotThrow(tstObj::createContextType);

    assertDoesNotThrow(tstObj::removeContextHeader);

    var secondResult = assertDoesNotThrow(tstObj::createContextType);

    assertEquals(firstResult, secondResult);
    assertNotSame(firstResult, secondResult);
  }

  @Test
  void getContextFromHeaderTest() {
    var tstObj =
        new KonnektorContextProvider(
            konnektorConfigurationProvider(), konnektorInterfaceAssembly());
    tstObj.createContextType();

    var context = assertDoesNotThrow(tstObj::getContext);

    assertNotNull(context);
    assertEquals(TestDataFactory.contextType(), context);
  }

  @Test
  void getContextThrowsTest() {
    var konCfgProvider =
        new KonnektorConfigurationProvider(
            TestDataFactory.createKonnektorConfigurationMutable().context(null));
    var konCtxProvider = new KonnektorContextProvider(konCfgProvider, konnektorInterfaceAssembly());

    assertThrows(MissingResourceException.class, konCtxProvider::getContext);
  }
}
