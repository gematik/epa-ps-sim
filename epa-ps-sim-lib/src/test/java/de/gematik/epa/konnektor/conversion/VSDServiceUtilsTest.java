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
package de.gematik.epa.konnektor.conversion;

import static de.gematik.epa.konnektor.conversion.VSDServiceUtils.NO_UPDATES;
import static de.gematik.epa.konnektor.conversion.VSDServiceUtils.UPDATES_SUCCESSFUL;
import static de.gematik.epa.utils.StringUtils.toISO885915;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class VSDServiceUtilsTest {

  @Test
  void decodeDataTest() {
    String validBase64 = "SGVsbG8gV29ybGQ=";
    byte[] expectedBytes = "Hello World".getBytes();
    byte[] actual = VSDServiceUtils.decodeData(validBase64);
    assertArrayEquals(expectedBytes, actual);
  }

  @Test
  void unzipAndDecodeTest() throws IOException {
    String testData = "This is some compressed data.";
    byte[] compressedData = compressData(testData);

    var unzippedData = VSDServiceUtils.unzipDecodedData(compressedData);
    assertArrayEquals(toISO885915(testData), unzippedData);
  }

  private byte[] compressData(String data) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
      gzipOutputStream.write(toISO885915(data));
    }
    return outputStream.toByteArray();
  }

  @Test
  void testResultIsSuccessfulUpdatesSuccessful() {
    boolean actual = VSDServiceUtils.isResultSuccessful(UPDATES_SUCCESSFUL);
    assertTrue(actual);
  }

  @Test
  void testResultIsSuccessfulNoUpdates() {
    boolean actual = VSDServiceUtils.isResultSuccessful(NO_UPDATES);
    assertTrue(actual);
  }

  @Test
  void testResultIsNotSuccessful() {
    int result = 42; // Ein unbekannter Wert
    boolean actual = VSDServiceUtils.isResultSuccessful(result);
    assertFalse(actual);
  }

  @ParameterizedTest()
  @MethodSource("provideHcvTable")
  void testCalculateHcv(byte[] versicherungsBeginn, byte[] strassenName, String expectedH40Hex) {
    String actualHcvBase64 = VSDServiceUtils.calculateHcv(versicherungsBeginn, strassenName);
    assertEquals(expectedH40Hex, actualHcvBase64);
  }

  private static Stream<Arguments> provideHcvTable() {
    return Stream.of(
        Arguments.of(toISO885915("20190212"), toISO885915(""), "SIXug5Q="),
        Arguments.of(toISO885915("19981123"), toISO885915("Berliner Straße"), "ZUVJHRQ="),
        Arguments.of(toISO885915("19841003"), toISO885915("Angermünder Straße"), "fMSeevQ="),
        Arguments.of(toISO885915("20010119"), toISO885915("Björnsonstraße"), "GGJp5Pc="),
        Arguments.of(toISO885915("20040718"), toISO885915("Schönhauser Allee"), "NTZGtcg="));
  }

  @Test
  void calculateHcvShouldThrowExceptionWhenVBnull() {
    byte[] strassenAdresse = toISO885915("");
    Exception e =
        assertThrows(
            IllegalArgumentException.class,
            () -> VSDServiceUtils.calculateHcv(null, strassenAdresse));
    assertEquals("Versicherungsbeginn oder Strassenadresse must not be null", e.getMessage());
  }

  @Test
  void calculateHcvShouldThrowExceptionWhenSASnull() {
    byte[] versicherungsbeginn = toISO885915("20250203");
    Exception e =
        assertThrows(
            IllegalArgumentException.class,
            () -> VSDServiceUtils.calculateHcv(versicherungsbeginn, null));
    assertEquals("Versicherungsbeginn oder Strassenadresse must not be null", e.getMessage());
  }
}
