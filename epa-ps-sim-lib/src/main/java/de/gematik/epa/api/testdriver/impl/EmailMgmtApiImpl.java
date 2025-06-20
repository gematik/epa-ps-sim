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
package de.gematik.epa.api.testdriver.impl;

import de.gematik.epa.api.email.client.EmailManagementApi;
import de.gematik.epa.api.testdriver.email.EmailMgmtApi;
import de.gematik.epa.api.testdriver.email.dto.GetEmailAddressResponseDTO;
import de.gematik.epa.api.testdriver.email.dto.PutEmailRequestDTO;
import de.gematik.epa.api.testdriver.email.dto.PutEmailResponseDTO;
import de.gematik.epa.client.JaxRsClientWrapper;
import de.gematik.epa.email.EmailMgmtService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Slf4j
public class EmailMgmtApiImpl implements EmailMgmtApi {

  private EmailMgmtService emailMgmtService;

  public EmailMgmtApiImpl(final JaxRsClientWrapper<EmailManagementApi> emailClientWrapper) {
    this.emailMgmtService = new EmailMgmtService(emailClientWrapper);
  }

  @Override
  public GetEmailAddressResponseDTO getEmailAddress(String xInsurantid) {
    return emailMgmtService.getEmailAddress(xInsurantid);
  }

  @Override
  public PutEmailResponseDTO replaceEmailAddress(
      String xInsurantid, PutEmailRequestDTO putEmailRequestDTO) {
    return emailMgmtService.replaceEmail(xInsurantid, putEmailRequestDTO);
  }
}
