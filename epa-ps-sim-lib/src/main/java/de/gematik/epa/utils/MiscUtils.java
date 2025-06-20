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
package de.gematik.epa.utils;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MiscUtils {

  public static final String X_TARGET_FQDN = "x-target-fqdn";
  public static final List<String> ENDPOINTS_TO_IGNORE = List.of("information/api/v1/ehr");
  public static final List<String> INSURANT_ID_KEYS =
      List.of("recordId", "insurantId", "x-insurantid", "insurantid");
  public static final String X_INSURANT_ID = "x-insurantid";
  public static final String X_USER_AGENT = "x-useragent";
  public static final String X_ACTOR_ID = "x-actorId";

  public static <T> T safeCast(Object obj, Class<T> castType) {
    return castType.isAssignableFrom(obj.getClass()) ? castType.cast(obj) : null;
  }

  @Nullable
  public static String findInsurantId(Map<String, List<String>> headers) {
    for (String key : INSURANT_ID_KEYS) {
      var insurantId = extractInsurantId(headers, key);
      if (insurantId != null) {
        return insurantId;
      }
    }
    return null;
  }

  @Nullable
  public static String extractInsurantId(Map<String, List<String>> headers, String key) {
    return headers.getOrDefault(key, List.of()).stream().findFirst().orElse(null);
  }
}
