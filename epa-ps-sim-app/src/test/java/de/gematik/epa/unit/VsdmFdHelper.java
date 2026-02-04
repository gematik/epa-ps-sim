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

import jakarta.xml.bind.DatatypeConverter;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VsdmFdHelper {
  private final byte[] encKey;
  private final byte[] pzv2Prefix;
  private final Map<String, Map<String, String>> vsdmDb;

  private static final Charset ISO_8859_15 = Charset.forName("ISO-8859-15");
  private static final long IAT_TIME_OFFSET = 1735689600L;

  private static final int KVNR_LENGTH = 10;
  private static final int VB_LENGTH = 8;
  private static final int PLAINTEXT_LENGTH = 18;
  private static final int CIPHERTEXT_LENGTH = 34;

  public VsdmFdHelper(String betreiberkennung, byte[] commonSecret, int keyVersion) {
    validateInputs(betreiberkennung, commonSecret, keyVersion);
    this.encKey = TestUtils.deriveAesKeyFromSecret(commonSecret);
    this.pzv2Prefix = TestUtils.calculatePzv2Prefix(betreiberkennung, keyVersion);
    this.vsdmDb = TestUtils.loadDb();
    logInitialization(betreiberkennung, commonSecret, keyVersion);
  }

  private void validateInputs(String betreiberkennung, byte[] commonSecret, int keyVersion) {
    if (commonSecret.length < 32) {
      throw new IllegalArgumentException("Invalid commonSecret length");
    }
    if (betreiberkennung.length() != 1)
      throw new IllegalArgumentException("Invalid betreiberkennung length");

    if (keyVersion < 0 || keyVersion >= 4) throw new IllegalArgumentException("Invalid keyVersion");
  }

  private void logInitialization(String betreiberkennung, byte[] commonSecret, int keyVersion) {
    log.info(
        "VSDM-FD initialized with betreiberkennung={}, keyVersion={}",
        betreiberkennung,
        keyVersion);
    log.info("Common KEy: {}", TestUtils.bytesToHex(commonSecret));
    log.info("Secret AES/GCM-Key: {}", TestUtils.bytesToHex(this.encKey));
    log.info("Prefix is (hexdump): {}", TestUtils.bytesToHex(this.pzv2Prefix));
    log.info("iatTimeOffset={}", IAT_TIME_OFFSET);
  }

  @SneakyThrows
  public String genPruefziffer(String iccsn) {
    byte[] rIat8Bytes = TestUtils.calculateRIat8Value();

    Map<String, String> vsdmData = this.vsdmDb.get(iccsn);
    if (vsdmData == null) throw new IllegalArgumentException("ICCSN not found in database");

    String kvnr = vsdmData.get("KVNR");
    if (kvnr.getBytes(ISO_8859_15).length != KVNR_LENGTH) {
      throw new IllegalArgumentException("KVNR must be 10 characters long");
    }
    String vb = vsdmData.get("VB");
    String sas = vsdmData.get("Strasse");
    boolean revoked = Boolean.parseBoolean(vsdmData.get("revoked"));

    byte[] hcv = generateHcv(vb, sas);
    log.info("H_40_0 = (hexdump) " + DatatypeConverter.printHexBinary(hcv));

    byte[] iFeld1 = revoked ? TestUtils.setRevokedFlag(hcv) : hcv;
    log.info("iFeld1 = (hexdump) " + TestUtils.bytesToHex(iFeld1));
    log.info("r_iat_8 = (hexdump) " + TestUtils.bytesToHex(rIat8Bytes));

    byte[] plaintext = TestUtils.concatenate(iFeld1, rIat8Bytes, kvnr.getBytes(ISO_8859_15));
    if (plaintext.length != PLAINTEXT_LENGTH) {
      throw new IllegalArgumentException("Plaintext length must be 18 bytes");
    }

    byte[] iv = TestUtils.generateRandomIV();
    log.info("IV: " + TestUtils.bytesToHex(iv));

    byte[] ciphertext = encryptData(iv, plaintext);
    byte[] pzBase64Encoded = TestUtils.concatenate(this.pzv2Prefix, iv, ciphertext);
    if (pzBase64Encoded.length != 47) {
      throw new IllegalArgumentException("Base64-encoded Prüfziffer length must be 47 bytes");
    }

    String pruefziffer = Base64.getEncoder().encodeToString(pzBase64Encoded);
    log.info("Prüfziffer V2: " + pruefziffer);
    return pruefziffer;
  }

  private byte[] encryptData(byte[] iv, byte[] plaintext) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
    SecretKeySpec keySpec = new SecretKeySpec(this.encKey, "AES");
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);

    byte[] ciphertext = cipher.doFinal(plaintext);
    log.info("Ciphertext: " + TestUtils.bytesToHex(ciphertext));
    if (ciphertext.length != CIPHERTEXT_LENGTH) {
      throw new IllegalArgumentException("Ciphertext length must be 34 bytes");
    }
    return ciphertext;
  }

  @SneakyThrows
  // A_27352: VSDM-Prüfziffer Version 2: Erzeugung von hcv
  public byte[] generateHcv(String vb, String sas) {
    if (vb.contains(" ")) {
      throw new IllegalArgumentException("VB must not contain spaces");
    }
    byte[] vbBytes = vb.getBytes(ISO_8859_15);
    if (vbBytes.length != VB_LENGTH) {
      throw new IllegalArgumentException("VB must be 8 bytes when encoded in ISO-8859-15");
    }

    sas = sas.trim();
    byte[] sasBytes = sas.getBytes(ISO_8859_15);

    byte[] concatenated = TestUtils.concatenate(vbBytes, sasBytes);

    MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
    byte[] hash = sha256.digest(concatenated);
    if (hash.length != 32) {
      throw new IllegalArgumentException("Concatenated length must be 32 bytes");
    }
    log.info("Data-to-hashed(hexdump): " + TestUtils.bytesToHex(concatenated));

    byte[] h40 = Arrays.copyOfRange(hash, 0, 5);
    if (h40.length != 5) {
      throw new IllegalArgumentException("h40 length must be 5 bytes");
    }

    h40[0] = (byte) (h40[0] & 127);
    log.info("Generated hcv by VSDM-FD: " + TestUtils.bytesToHex(h40));
    return h40;
  }
}
