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
package de.gematik.epa.konnektor;

import de.gematik.epa.data.HbaInformation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import telematik.ws.conn.cardservice.xsd.v8_1.CardInfoType;
import telematik.ws.conn.eventservice.xsd.v6_1.GetCardsResponse;

@Service
@Slf4j
public class HbaInformationProvider extends InformationProvider<HbaInformation> {

  public HbaInformationProvider(
      final KonnektorContextProvider konnektorContextProvider,
      final KonnektorInterfaceAssembly konnektorInterfaceAssembly) {
    super(konnektorContextProvider, konnektorInterfaceAssembly);
  }

  @Override
  protected GetCardsResponse getCardsResponse() {
    return eventServiceClient.getHbaInfo();
  }

  @Override
  protected HbaInformation retrieveCardInformation(final CardInfoType cardInfo) {
    return new HbaInformation(
        getTelematikId(cardInfo),
        cardInfo.getIccsn(),
        cardInfo.getCardHolderName(),
        cardInfo.getCardHandle(),
        getProfessionOids(cardInfo));
  }
}
