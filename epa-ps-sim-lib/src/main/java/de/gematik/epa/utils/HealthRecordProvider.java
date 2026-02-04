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
package de.gematik.epa.utils;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HealthRecordProvider {

  private static final ConcurrentHashMap<String, String> INSURANT_ID_TO_URL =
      new ConcurrentHashMap<>();

  public static String getHealthRecordUrl(String insurantId) {
    if (!INSURANT_ID_TO_URL.containsKey(insurantId)) {
      throw new IllegalStateException(
          "No health record found for insurantId: "
              + insurantId
              + ". Try to call getRecordStatus. If still no record was found then there is no one configured for the given insurantId.");
    }
    return INSURANT_ID_TO_URL.get(insurantId);
  }

  public static boolean hasHealthRecord(String insurantId) {
    return INSURANT_ID_TO_URL.containsKey(insurantId);
  }

  public static void clearHealthRecord(String insurantId) {
    INSURANT_ID_TO_URL.remove(insurantId);
  }

  public static void addHealthRecord(String insurantId, String url) {
    INSURANT_ID_TO_URL.put(insurantId, url);
  }

  public static Map<String, String> getAllHealthRecords() {
    return Collections.unmodifiableMap(INSURANT_ID_TO_URL);
  }
}
