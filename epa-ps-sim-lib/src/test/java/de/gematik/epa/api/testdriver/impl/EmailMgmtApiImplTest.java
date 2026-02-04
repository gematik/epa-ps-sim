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

import de.gematik.epa.api.email.client.EmailManagementApi;
import de.gematik.epa.api.testdriver.email.dto.GetEmailAddressResponseDTO;
import de.gematik.epa.api.testdriver.email.dto.PutEmailRequestDTO;
import de.gematik.epa.api.testdriver.email.dto.PutEmailResponseDTO;
import de.gematik.epa.client.JaxRsClientWrapper;
import de.gematik.epa.email.EmailMgmtService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmailMgmtApiImplTest {

  private final JaxRsClientWrapper<EmailManagementApi> emailClientWrapper =
      mock(JaxRsClientWrapper.class);
  private EmailMgmtApiImpl emailMgmtApiImpl;
  private final EmailMgmtService emailMgmtService = mock(EmailMgmtService.class);

  @BeforeAll
  void setUp() {
    emailMgmtApiImpl = new EmailMgmtApiImpl(emailClientWrapper);
    emailMgmtApiImpl.setEmailMgmtService(emailMgmtService);
  }

  @Test
  void shouldReturnEmailAddress() {
    var insurantId = "X12345678";
    var expectedResponse = new GetEmailAddressResponseDTO();
    when(emailMgmtService.getEmailAddress(insurantId)).thenReturn(expectedResponse);
    var actualResponse = emailMgmtApiImpl.getEmailAddress(insurantId);
    assertThat(actualResponse).isEqualTo(expectedResponse);
  }

  @Test
  void shouldReplaceEmailAddress() {
    var insurantId = "X12345678";
    String email = "email@test.de";
    var expectedResponse = new PutEmailResponseDTO();

    PutEmailRequestDTO requestDTO = new PutEmailRequestDTO();
    requestDTO.setEmail(email);
    when(emailMgmtService.replaceEmail(insurantId, requestDTO)).thenReturn(expectedResponse);
    var actualResponse = emailMgmtApiImpl.replaceEmailAddress(insurantId, requestDTO);
    assertThat(actualResponse).isEqualTo(expectedResponse);
  }
}
