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
package de.gematik.epa.konnektor.client;

import static de.gematik.epa.unit.util.TestDataFactory.*;
import static de.gematik.epa.utils.StringUtils.toISO885915;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import de.gematik.epa.api.testdriver.entitlement.dto.PostEntitlementRequestDTO;
import de.gematik.epa.unit.util.ResourceLoader;
import de.gematik.epa.unit.util.TestBase;
import de.gematik.epa.unit.util.TestDataFactory;
import java.io.IOException;
import java.util.Base64;
import java.util.NoSuchElementException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class VSDServiceClientTest extends TestBase {

  public static final String BASE64_PATTERN = "^[A-Za-z0-9+/]*={0,2}$";
  private final VSDServiceClient tstObj =
      new VSDServiceClient(konnektorContextProvider(), konnektorInterfaceAssembly());

  @BeforeEach
  void beforeEach() {
    setupMocksForSmbInformationProvider(konnektorInterfaceAssembly());
  }

  @Test
  void transformRequestTest() {
    when(konnektorInterfaceAssembly().eventService().getCards(Mockito.any()))
        .thenReturn(getCardsEgkResponse(KVNR));
    var readVSDRequest = ResourceLoader.readVSDRequest();
    var konReadVSDRequest =
        assertDoesNotThrow(
            () -> tstObj.transformRequest(readVSDRequest.kvnr(), readVSDRequest.telematikId()));
    System.out.println(konReadVSDRequest);
    assertNotNull(konReadVSDRequest);
    assertTrue(konReadVSDRequest.isPerformOnlineCheck());
    assertTrue(konReadVSDRequest.isReadOnlineReceipt());
    assertNotNull(konReadVSDRequest.getEhcHandle());
    assertNotNull(konReadVSDRequest.getHpcHandle());
    assertEquals("EGK456", konReadVSDRequest.getEhcHandle());
  }

  @Test
  void transformRequestFailsWhenEhcHandleFails() {
    when(konnektorInterfaceAssembly().eventService().getCards(Mockito.any()))
        .thenThrow(IllegalArgumentException.class);
    var readVSDRequest = ResourceLoader.readVSDRequest();
    assertThrows(
        NoSuchElementException.class,
        () -> tstObj.transformRequest(readVSDRequest.kvnr(), readVSDRequest.telematikId()));
  }

  @Test
  void transformResponseTest() {
    var konReadVSDResponse = TestDataFactory.createReadVSDResponse();
    var readVSDResponse = assertDoesNotThrow(() -> tstObj.transformResponse(konReadVSDResponse));

    assertNotNull(readVSDResponse);
    assertTrue(readVSDResponse.success());
    assertEquals(2, readVSDResponse.resultOfOnlineCheckEGK());
  }

  @Test
  void shouldReturnPruefziffer() {
    when(konnektorInterfaceAssembly().vsdService().readVSD(Mockito.any()))
        .thenReturn(TestDataFactory.createReadVSDResponse());
    when(konnektorInterfaceAssembly().eventService().getCards(Mockito.any()))
        .thenReturn(getCardsEgkResponse(KVNR));
    assertDoesNotThrow(() -> tstObj.getPruefziffer(KVNR, SMB_AUT_TELEMATIK_ID));
  }

  @SneakyThrows
  @Test
  void shouldThrowExceptionWhenVsdResultIsNotSuccessful() {
    when(konnektorInterfaceAssembly().vsdService().readVSD(Mockito.any()))
        .thenReturn(readVSDWithUpdateNotPossible());
    when(konnektorInterfaceAssembly().eventService().getCards(Mockito.any()))
        .thenReturn(getCardsEgkResponse(KVNR));

    assertThrows(IOException.class, () -> tstObj.getPruefziffer(KVNR, SMB_AUT_TELEMATIK_ID));
  }

  @SneakyThrows
  @Test
  void shouldCreateHcv() {
    when(konnektorInterfaceAssembly().vsdService().readVSD(Mockito.any()))
        .thenReturn(TestDataFactory.createReadVSDResponse());
    when(konnektorInterfaceAssembly().eventService().getCards(Mockito.any()))
        .thenReturn(getCardsEgkResponse(KVNR));
    String actualHcv =
        tstObj.createHcv(
            KVNR, PostEntitlementRequestDTO.TestCaseEnum.VALID_HCV.value(), SMB_AUT_TELEMATIK_ID);
    String expectedHcv = "Yu+dhTA=";
    assertEquals(expectedHcv, actualHcv);
  }

  @Test
  void shouldCreateWrongHcv() {
    when(konnektorInterfaceAssembly().vsdService().readVSD(Mockito.any()))
        .thenReturn(TestDataFactory.createReadVSDResponse());
    when(konnektorInterfaceAssembly().eventService().getCards(Mockito.any()))
        .thenReturn(getCardsEgkResponse(KVNR));
    String actualHcv =
        tstObj.createHcv(
            KVNR,
            PostEntitlementRequestDTO.TestCaseEnum.INVALID_HCV_HASH.value(),
            SMB_AUT_TELEMATIK_ID);
    String notExpectedHcv = "YDpu+dhTA=";

    assertNotEquals(notExpectedHcv, actualHcv);
    assertEquals(8, actualHcv.length());
    assertTrue(isBase64String(actualHcv));
  }

  @Test
  void shouldCreateIllegalHcv() {
    when(konnektorInterfaceAssembly().vsdService().readVSD(Mockito.any()))
        .thenReturn(TestDataFactory.createReadVSDResponse());
    when(konnektorInterfaceAssembly().eventService().getCards(Mockito.any()))
        .thenReturn(getCardsEgkResponse(KVNR));
    String actualHcv =
        tstObj.createHcv(
            KVNR,
            PostEntitlementRequestDTO.TestCaseEnum.INVALID_HCV_STRUCTURE.value(),
            SMB_AUT_TELEMATIK_ID);

    assertTrue(!isBase64String(actualHcv) || actualHcv.length() != 8);
  }

  @Test
  void shouldReturnNullHcv() {
    when(konnektorInterfaceAssembly().vsdService().readVSD(Mockito.any()))
        .thenReturn(TestDataFactory.createReadVSDResponse());
    when(konnektorInterfaceAssembly().eventService().getCards(Mockito.any()))
        .thenReturn(getCardsEgkResponse(KVNR));
    String actualHcv =
        tstObj.createHcv(
            KVNR, PostEntitlementRequestDTO.TestCaseEnum.NO_HCV.value(), SMB_AUT_TELEMATIK_ID);

    assertNull(actualHcv);
  }

  @SneakyThrows
  @Test
  void shouldThrowExceptionWhenNotInGzipFormat() {
    when(konnektorInterfaceAssembly().vsdService().readVSD(Mockito.any()))
        .thenReturn(readVSDWithUpdateNotPossible());
    when(konnektorInterfaceAssembly().eventService().getCards(Mockito.any()))
        .thenReturn(getCardsEgkResponse(KVNR));

    Exception e =
        assertThrows(
            IOException.class,
            () ->
                tstObj.createHcv(
                    KVNR,
                    PostEntitlementRequestDTO.TestCaseEnum.VALID_HCV.value(),
                    SMB_AUT_TELEMATIK_ID));
    assertEquals("Not in GZIP format", e.getMessage());
  }

  @SneakyThrows
  @Test
  void shouldReturnVersicherungsbeginn() {
    when(konnektorInterfaceAssembly().vsdService().readVSD(Mockito.any()))
        .thenReturn(TestDataFactory.createReadVSDResponse());
    when(konnektorInterfaceAssembly().eventService().getCards(Mockito.any()))
        .thenReturn(getCardsEgkResponse(KVNR));
    byte[] actualVB = tstObj.getVersicherungsbeginn(KVNR, SMB_AUT_TELEMATIK_ID);
    assertArrayEquals(toISO885915("20200123"), actualVB);
  }

  @SneakyThrows
  @Test
  void shouldThrowExceptionWhenAllgemeineVersicherungsdatenNotInGzipFormat() {
    when(konnektorInterfaceAssembly().vsdService().readVSD(Mockito.any()))
        .thenReturn(readVSDWithUpdateNotPossible());
    when(konnektorInterfaceAssembly().eventService().getCards(Mockito.any()))
        .thenReturn(getCardsEgkResponse(KVNR));

    Exception e =
        assertThrows(
            IOException.class, () -> tstObj.getVersicherungsbeginn(KVNR, SMB_AUT_TELEMATIK_ID));
    assertEquals("Not in GZIP format", e.getMessage());
  }

  @SneakyThrows
  @Test
  void shouldReturnStrassenAdresse() {
    when(konnektorInterfaceAssembly().vsdService().readVSD(Mockito.any()))
        .thenReturn(TestDataFactory.createReadVSDResponse());
    when(konnektorInterfaceAssembly().eventService().getCards(Mockito.any()))
        .thenReturn(getCardsEgkResponse(KVNR));
    byte[] actualSAS = tstObj.getStrassenAdresse(KVNR, SMB_AUT_TELEMATIK_ID);
    assertArrayEquals(toISO885915("Ruhsal"), actualSAS);
  }

  @SneakyThrows
  @Test
  void shouldReturnStrassenAdresseWhenStrasseEmpty() {
    when(konnektorInterfaceAssembly().vsdService().readVSD(Mockito.any()))
        .thenReturn(readVSDResponseStrasseEmpty());
    when(konnektorInterfaceAssembly().eventService().getCards(Mockito.any()))
        .thenReturn(getCardsEgkResponse(KVNR));
    byte[] actualSAS = tstObj.getStrassenAdresse(KVNR, SMB_AUT_TELEMATIK_ID);
    assertArrayEquals(toISO885915(""), actualSAS);
  }

  @SneakyThrows
  @Test
  void shouldThrowExceptionWhenPersoenlicheVersichertendatenNotInGzipFormat() {
    when(konnektorInterfaceAssembly().vsdService().readVSD(Mockito.any()))
        .thenReturn(readVSDWithUpdateNotPossible());
    when(konnektorInterfaceAssembly().eventService().getCards(Mockito.any()))
        .thenReturn(getCardsEgkResponse(KVNR));

    Exception e =
        assertThrows(
            IOException.class, () -> tstObj.getStrassenAdresse(KVNR, SMB_AUT_TELEMATIK_ID));
    assertEquals("Not in GZIP format", e.getMessage());
  }

  /**
   * Checks if a string is a valid Base64 string.
   *
   * @param value the string to check
   * @return true if the string is a valid Base64 string, false otherwise
   */
  static boolean isBase64String(String value) {
    if (!value.matches(BASE64_PATTERN)) {
      return false;
    }
    if (value.length() % 4 != 0) {
      return false;
    }
    try {
      Base64.getDecoder().decode(value);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
