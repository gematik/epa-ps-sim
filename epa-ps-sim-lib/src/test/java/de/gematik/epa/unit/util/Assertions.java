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
package de.gematik.epa.unit.util;

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.epa.api.testdriver.config.BasicAuthenticationConfig;
import de.gematik.epa.api.testdriver.config.ProxyAddressConfig;
import de.gematik.epa.api.testdriver.config.TlsConfig;
import de.gematik.epa.ihe.model.simple.InsurantId;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.opentest4j.AssertionFailedError;
import telematik.ws.conn.connectorcontext.xsd.v2_0.ContextType;

@UtilityClass
public class Assertions {

  public static void assertEquals(@NonNull ContextType expected, @NonNull ContextType actual) {
    if (actual.equals(expected)
        || (actual.getMandantId().equals(expected.getMandantId())
            && actual.getClientSystemId().equals(expected.getClientSystemId())
            && actual.getWorkplaceId().equals(expected.getWorkplaceId())
            && actual.getUserId().equals(expected.getUserId()))) return;

    throw new AssertionFailedError("Contexts are not the same", expected, actual);
  }

  public static void assertEquals(@NonNull InsurantId expected, @NonNull InsurantId actual) {
    if (actual.equals(expected)
        || (actual.getExtension().equals(expected.getExtension())
            && actual.getRoot().equals(expected.getRoot()))) return;

    throw new AssertionFailedError("InsurantIds are not the same", expected, actual);
  }

  public static void assertTlsConfig(
      @NonNull TlsConfig expectedValues, TLSClientParameters actualValues) {
    assertNotNull(actualValues);

    assertTrue(actualValues.getKeyManagers().length > 0);
    assertTrue(actualValues.getTrustManagers().length > 0);
    assertNotNull(actualValues.getCipherSuites());
    assertArrayEquals(
        expectedValues.ciphersuites().toArray(new String[0]),
        actualValues.getCipherSuites().toArray(new String[0]));
  }

  public static void assertAuthorization(
      @NonNull BasicAuthenticationConfig expectedValues, AuthorizationPolicy actualValues) {
    assertNotNull(actualValues);

    assertTrue(actualValues.isSetAuthorizationType());
    org.junit.jupiter.api.Assertions.assertEquals(
        expectedValues.username(), actualValues.getUserName());
    org.junit.jupiter.api.Assertions.assertEquals(
        expectedValues.password(), actualValues.getPassword());
  }

  public static void assertProxy(
      @NonNull ProxyAddressConfig expectedValues, HTTPClientPolicy actualValues) {
    assertNotNull(actualValues);

    org.junit.jupiter.api.Assertions.assertEquals(
        expectedValues.address(), actualValues.getProxyServer());
    org.junit.jupiter.api.Assertions.assertEquals(
        expectedValues.port(), actualValues.getProxyServerPort());
  }
}
