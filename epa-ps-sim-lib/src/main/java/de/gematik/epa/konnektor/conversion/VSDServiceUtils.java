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
package de.gematik.epa.konnektor.conversion;

import de.gematik.epa.utils.XmlUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import telematik.ws.conn.vsds.vsdservice.xsd.v5_2.ReadVSDResponse;
import telematik.ws.fa.vsds.pruefungsnachweis.xsd.v1_0.PN;
import telematik.ws.fa.vsds.schema_vsd.xsd.v5_2_0.UCAllgemeineVersicherungsdatenXML;
import telematik.ws.fa.vsds.schema_vsd.xsd.v5_2_0.UCPersoenlicheVersichertendatenXML;

@UtilityClass
@Slf4j
public class VSDServiceUtils {

  public static final int UPDATES_SUCCESSFUL = 1;
  public static final int NO_UPDATES = 2;

  @SneakyThrows
  public static String calculateHcv(byte[] versicherungsBeginn, byte[] strassenAdresse) {
    if (versicherungsBeginn == null || strassenAdresse == null) {
      throw new IllegalArgumentException(
          "Versicherungsbeginn oder Strassenadresse must not be null");
    }
    var digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(ArrayUtils.addAll(versicherungsBeginn, strassenAdresse));
    byte[] hcv = Arrays.copyOfRange(hash, 0, 5);
    hcv[0] &= 0x7F; // switch the most significant bit in first byte
    return Base64.getEncoder().encodeToString(hcv);
  }

  @SneakyThrows
  public static int getResultOfOnlineCheckEGK(PN pn) {
    return getE(pn).intValue();
  }

  public static BigInteger getE(PN pn) {
    BigInteger e = pn.getE();
    log.info("Result of online check EGK: " + e);
    return e;
  }

  public static byte[] getPruefziffer(PN pn) {
    return getPz(pn);
  }

  public static PN getPn(ReadVSDResponse response) {
    byte[] pruefungsnachweis = getPruefungsnachweis(response);
    return VSDServiceUtils.unmarshalXmlBytes(PN.class, pruefungsnachweis);
  }

  public static UCPersoenlicheVersichertendatenXML getPersoenlicheVersichertendatenXml(
      ReadVSDResponse response) {
    byte[] persoenlicheVersichertendatenStr = getPersoenlicheVersichertendaten(response);
    return VSDServiceUtils.unmarshalXmlBytes(
        UCPersoenlicheVersichertendatenXML.class, persoenlicheVersichertendatenStr);
  }

  public static UCAllgemeineVersicherungsdatenXML getAllgemeineVersicherungsdatenXml(
      ReadVSDResponse response) {
    byte[] allgemeineVersicherungsdatenStr = getAllgemeineVersicherungsdaten(response);
    return VSDServiceUtils.unmarshalXmlBytes(
        UCAllgemeineVersicherungsdatenXML.class, allgemeineVersicherungsdatenStr);
  }

  public static byte[] getPz(PN pn) {
    return pn.getPZ();
  }

  @SneakyThrows
  public byte[] getPruefungsnachweis(ReadVSDResponse response) {
    return decodeAndUnzipData(response.getPruefungsnachweis());
  }

  @SneakyThrows
  public byte[] getAllgemeineVersicherungsdaten(ReadVSDResponse response) {
    return decodeAndUnzipData(response.getAllgemeineVersicherungsdaten());
  }

  @SneakyThrows
  public byte[] getPersoenlicheVersichertendaten(ReadVSDResponse response) {
    return decodeAndUnzipData(response.getPersoenlicheVersichertendaten());
  }

  @SneakyThrows
  private byte[] decodeAndUnzipData(byte[] data) {
    var dataAsBase64 = Base64.getEncoder().encodeToString(data);
    byte[] decodedData = decodeData(dataAsBase64);

    return unzipDecodedData(decodedData);
  }

  byte[] decodeData(String base64Data) {
    return Base64.getDecoder().decode(base64Data);
  }

  byte[] unzipDecodedData(byte[] data) throws IOException {
    try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(data))) {
      return gzipInputStream.readAllBytes();
    } catch (ZipException e) {
      log.error("Failed to unzip data: ", e);
      throw e;
    }
  }

  public <T> T unmarshalXmlBytes(Class<T> clazz, byte[] xmlBytes) {
    return XmlUtils.unmarshal(clazz, new ByteArrayInputStream(xmlBytes));
  }

  public static boolean isResultSuccessful(int result) {
    if (result == UPDATES_SUCCESSFUL) {
      log.info(
          "Result of online check EGK is {}. VSD update on eGK completed successfully", result);
      return true;
    } else if (result == NO_UPDATES) {
      log.info("Result of online check EGK is {}. No need to update VSD on eGK", result);
      return true;
    } else {
      log.error(
          "Unknown result of online check EGK is {}. Please check logs manually via Konnektor management interface",
          result);
      return false;
    }
  }
}
