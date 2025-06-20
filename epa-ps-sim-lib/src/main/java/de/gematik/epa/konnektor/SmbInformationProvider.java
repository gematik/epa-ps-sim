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
package de.gematik.epa.konnektor;

import de.gematik.epa.authentication.exception.TelematikIdNotFoundException;
import de.gematik.epa.config.AuthorInstitutionProvider;
import de.gematik.epa.data.SmbInformation;
import de.gematik.epa.ihe.model.simple.AuthorInstitution;
import de.gematik.epa.utils.TelematikIdHolder;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import telematik.ws.conn.cardservice.xsd.v8_1.CardInfoType;
import telematik.ws.conn.eventservice.xsd.v6_1.GetCardsResponse;

@Service
@Slf4j
public class SmbInformationProvider extends InformationProvider<SmbInformation>
    implements AuthorInstitutionProvider {

  public SmbInformationProvider(
      final KonnektorContextProvider konnektorContextProvider,
      final KonnektorInterfaceAssembly konnektorInterfaceAssembly) {
    super(konnektorContextProvider, konnektorInterfaceAssembly);
  }

  private static AuthorInstitution smbInformationToAuthorInstitution(final SmbInformation smbInfo) {
    return new AuthorInstitution(smbInfo.cardHolderName(), smbInfo.telematikId());
  }

  @Override
  protected GetCardsResponse getCardsResponse() {
    return eventServiceClient.getSmbInfo();
  }

  @Override
  protected SmbInformation retrieveCardInformation(final CardInfoType cardInfo) {
    return new SmbInformation(
        getTelematikId(cardInfo),
        cardInfo.getIccsn(),
        cardInfo.getCardHolderName(),
        cardInfo.getCardHandle(),
        getProfessionOids(cardInfo));
  }

  /**
   * the Info for the card having the specific telematikId
   *
   * @param telematikId - telematikId of the card
   * @return Optinal of SmbInformation
   */
  public Optional<SmbInformation> getSmbInformationForTelematikId(final String telematikId) {
    return getCardsInformations().stream()
        .filter(card -> card.telematikId().equals(telematikId))
        .findFirst();
  }

  public List<AuthorInstitution> getAuthorInstitutions() {
    return getCardsInformations().stream()
        .map(SmbInformationProvider::smbInformationToAuthorInstitution)
        .toList();
  }

  @Override
  public AuthorInstitution getAuthorInstitution() {
    var loggedInTelematikId = TelematikIdHolder.getTelematikId();
    return getAuthorInstitutions().stream()
        .filter(c -> c.identifier().equals(loggedInTelematikId))
        .findFirst()
        .orElseThrow(
            () ->
                new TelematikIdNotFoundException(
                    "No matching telematikId: " + loggedInTelematikId));
  }
}
