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
package de.gematik.epa.api.testdriver.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.epa.api.testdriver.authentication.dto.LoginResponseDTO;
import de.gematik.epa.authentication.AuthenticationService;
import de.gematik.epa.authentication.LoginResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthenticationApiImplTest {

  private final AuthenticationService authenticationService = mock(AuthenticationService.class);

  private AuthenticationApiImpl authenticationApiImpl;

  @BeforeAll
  void setup() {
    authenticationApiImpl = new AuthenticationApiImpl(authenticationService);
  }

  @Test
  void shouldLoginSuccessful() {
    var telematikId = "testTelematikId";
    var fqdn = "testFqdn";
    var recordId = "123456";

    final LoginResult loginResult = new LoginResult().success(true);
    when(authenticationService.login(telematikId, fqdn)).thenReturn(loginResult);

    LoginResponseDTO result = authenticationApiImpl.login(telematikId, recordId, fqdn);

    assertThat(result.getTelematikId()).isEqualTo(telematikId);
    assertThat(result.getSuccess()).isTrue();
  }

  @Test
  void loginShouldHandleFailedCase() {
    var telematikId = "testTelematikId";
    var fqdn = "testFqdn";
    var recordId = "123456";

    final LoginResult loginResult =
        new LoginResult().success(false).telematikId(telematikId).errorMessage("error");
    when(authenticationService.login(telematikId, fqdn)).thenReturn(loginResult);

    LoginResponseDTO result = authenticationApiImpl.login(telematikId, recordId, fqdn);

    assertThat(result.getTelematikId()).isEqualTo(telematikId);
    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).isEqualTo(loginResult.errorMessage());
  }
}
