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
package de.gematik.epa.konnektor;

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.epa.authentication.exception.TelematikIdNotFoundException;
import de.gematik.epa.unit.util.TestBase;
import de.gematik.epa.unit.util.TestDataFactory;
import de.gematik.epa.utils.TelematikIdHolder;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SmbInformationProviderTest extends TestBase {

  SmbInformationProvider tstObj;

  @BeforeEach
  void beforeEach() {
    tstObj = new SmbInformationProvider(konnektorContextProvider(), konnektorInterfaceAssembly());
    TelematikIdHolder.clearTelematikId();
  }

  @SneakyThrows
  @Test
  void getSmbInformationsTest() {
    TestDataFactory.setupMocksForSmbInformationProvider(konnektorInterfaceAssembly());
    final var getCardsSmbResponse = TestDataFactory.getCardsSmbResponse();

    final var smbInformations = assertDoesNotThrow(() -> tstObj.getCardsInformations());

    assertNotNull(smbInformations);
    assertEquals(getCardsSmbResponse.getCards().getCard().size(), smbInformations.size());
  }

  @Test
  void getAuthorInstitutionsTest() {
    TestDataFactory.setupMocksForSmbInformationProvider(konnektorInterfaceAssembly());

    final var authorInformations = assertDoesNotThrow(() -> tstObj.getAuthorInstitutions());

    assertNotNull(authorInformations);
    assertFalse(authorInformations.isEmpty());
  }

  @Test
  void shouldGetAuthorInstitutionFromLoggedInUser() {
    TestDataFactory.setupMocksForSmbInformationProvider(konnektorInterfaceAssembly());
    TelematikIdHolder.setTelematikId(TestDataFactory.SMB_AUT_TELEMATIK_ID);

    final var authorInformation = assertDoesNotThrow(() -> tstObj.getAuthorInstitution());

    assertEquals(TestDataFactory.cardInfoSmb().getCardHolderName(), authorInformation.name());
    assertEquals(TestDataFactory.SMB_AUT_TELEMATIK_ID, authorInformation.identifier());
  }

  @Test
  void getAuthorInstitutionThrowsExceptionForNotLoggedInUser() {
    TestDataFactory.setupMocksForSmbInformationProvider(konnektorInterfaceAssembly());

    assertThrows(TelematikIdNotFoundException.class, () -> tstObj.getAuthorInstitution());
  }

  @Test
  void shouldReturnSmbInformationByTelematikId() {
    TestDataFactory.setupMocksForSmbInformationProvider(konnektorInterfaceAssembly());

    final var smbInformation =
        assertDoesNotThrow(
            () -> tstObj.getSmbInformationForTelematikId(TestDataFactory.SMB_AUT_TELEMATIK_ID));

    assertTrue(smbInformation.isPresent());
    assertEquals(TestDataFactory.cardInfoSmb().getIccsn(), smbInformation.get().iccsn());
  }
}
