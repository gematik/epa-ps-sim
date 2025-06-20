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

import java.nio.charset.Charset;
import java.util.Objects;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/** Collection of static methods, which deal with {@link String}s in one way or the other. */
@UtilityClass
@Slf4j
public class StringUtils {

  /**
   * Convenience String function to make sure the given String ends with the given suffix.
   *
   * @param string {@link String} the string to check
   * @param suffix {@link String} the suffix which shall be at the end of the String
   * @return {@link String} If it already ends with the suffix, the original string is returned, if
   *     not the suffix is added to the end of the string and the concatenated String is returned
   */
  public static String ensureEndsWith(@NonNull String string, @NonNull String suffix) {
    if (!string.endsWith(suffix)) return string.concat(suffix);
    return string;
  }

  /**
   * Convenience String function to make sure the given String starts with the given prefix.
   *
   * @param string {@link String} the string to check
   * @param prefix {@link String} the prefix which shall be at the start of the String
   * @return {@link String} If it already starts with the prefix, the original string is returned,
   *     if not the prefix is added at the start of the string and the concatenated String is
   *     returned
   */
  public static String ensureStartsWith(@NonNull String string, @NonNull String prefix) {
    if (!string.startsWith(prefix)) return prefix.concat(string);
    return string;
  }

  /**
   * Method to split a full name into first and last name.<br>
   * It's based on the assumption, that there are one too many first names, followed by one last
   * name.<br>
   * If the given name is null, or does not contain a full name, defaults will be returned.
   *
   * @param cardHolderName full name to be split
   * @return {@link Pair} where {@link Pair#getLeft()} are the first names and {@link
   *     Pair#getRight()} is the last name.
   */
  public static Pair<String, String> getLastAndFirstName(String cardHolderName) {
    String lastname = "Meier";
    String firstname = "Fritze";

    if (Objects.nonNull(cardHolderName)) {
      lastname = cardHolderName;

      var cnParts = cardHolderName.split("\\s");
      if (cnParts.length > 1) {
        lastname = cnParts[cnParts.length - 1];
        firstname = cardHolderName.substring(0, cardHolderName.lastIndexOf(lastname) - 1);
      }
    }

    return new ImmutablePair<>(firstname, lastname);
  }

  /**
   * Appends the causes of a given throwable to a provided StringBuilder.
   *
   * <p>This method iterates through the chain of causes of the provided throwable and appends each
   * cause to the provided StringBuilder. Each cause is prefixed with a tab character and a
   * semicolon.
   *
   * @param throwable the throwable whose causes are to be appended; must not be null
   * @param builder the StringBuilder to which the causes will be appended; must not be null
   * @return the StringBuilder with the appended causes
   */
  public static StringBuilder appendCauses(
      @NonNull Throwable throwable, @NonNull StringBuilder builder) {
    Throwable cause = throwable.getCause();

    while (Objects.nonNull(cause)) {
      builder.append(";\t").append(cause);
      cause = cause.getCause();
    }

    return builder;
  }

  /***
   * This method converts string literals to a byte array based on ISO-8859-15 charset
   * @param string the  string literal to convert
   * @return byte array based on ISO-8859-15 charset
   */
  public static byte[] toISO885915(@NonNull String string) {
    return string.getBytes(Charset.forName("ISO-8859-15"));
  }
}
