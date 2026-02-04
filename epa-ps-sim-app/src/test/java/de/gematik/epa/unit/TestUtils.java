/*-
 * #%L
 * epa-ps-sim-app
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
package de.gematik.epa.unit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.bind.DatatypeConverter;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

public class TestUtils {
  public static final long IAT_TIME_OFFSET = 1735689600L;
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final String DB_FILENAME = "vsdm-fd-db.json";

  public static byte[] calculateRIat8Value() {
    long iat = Instant.now().getEpochSecond();
    long rIat = iat - IAT_TIME_OFFSET;
    long rIat8 = rIat >> 3;
    return Arrays.copyOfRange(ByteBuffer.allocate(4).putInt((int) rIat8).array(), 1, 4);
  }

  public static String bytesToHex(byte[] bytes) {
    return DatatypeConverter.printHexBinary(bytes).toLowerCase();
  }

  static byte[] setRevokedFlag(byte[] hcv) {
    hcv[0] = (byte) (hcv[0] | 128);
    return hcv;
  }

  static byte[] concatenate(byte[]... arrays) {
    byte[] result = new byte[0];
    for (byte[] array : arrays) {
      result = ArrayUtils.addAll(result, array);
    }
    return result;
  }

  static byte[] generateRandomIV() {
    SecureRandom secureRandom = new SecureRandom();
    byte[] iv = new byte[12];
    secureRandom.nextBytes(iv);
    return iv;
  }

  public static byte[] deriveAesKeyFromSecret(byte[] commonSecret) {
    HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA256Digest());
    HKDFParameters params =
        new HKDFParameters(commonSecret, null, "VSDM+ Version 2 AES/GCM".getBytes());
    hkdf.init(params);

    byte[] vsdmplusAesKey = new byte[16]; // 128-bit key
    hkdf.generateBytes(vsdmplusAesKey, 0, vsdmplusAesKey.length);
    return vsdmplusAesKey;
  }

  static byte[] calculatePzv2Prefix(String betreiberkennung, int keyVersion) {
    int tmp = 128 + ((betreiberkennung.charAt(0) - 65) << 2) + keyVersion;
    if (tmp <= 128 || tmp >= 256) {
      throw new IllegalArgumentException("tmp must be between 128 and 256");
    }
    byte[] pzv2Prefix = new byte[] {(byte) tmp};
    if (pzv2Prefix.length != 1
        || Byte.toUnsignedInt(pzv2Prefix[0]) < 128
        || Byte.toUnsignedInt(pzv2Prefix[0]) >= 256) {
      throw new IllegalArgumentException(
          "pzv2Prefix must be a single byte with a value between 128 and 255");
    }
    return pzv2Prefix;
  }

  @SneakyThrows
  public static Map<String, Map<String, String>> loadDb() {
    try (InputStream inputStream =
        TestUtils.class.getClassLoader().getResourceAsStream(DB_FILENAME)) {
      if (inputStream == null) {
        throw new FileNotFoundException("File not found: " + DB_FILENAME);
      }
      return objectMapper.readValue(inputStream, new TypeReference<>() {});
    }
  }

  public static byte[] hexStringToByteArray(String s) {
    if (s == null || s.length() % 2 != 0) {
      throw new IllegalArgumentException(
          "Invalid hex string: must be non-null and have an even length.");
    }

    if (!s.matches("[0-9a-fA-F]+")) {
      throw new IllegalArgumentException("Invalid hex string: contains non-hex characters.");
    }

    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] =
          (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
    }
    return data;
  }
}
