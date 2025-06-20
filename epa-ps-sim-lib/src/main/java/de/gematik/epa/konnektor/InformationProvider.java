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

import de.gematik.epa.data.CardInformation;
import de.gematik.epa.konnektor.client.CertificateServiceClient;
import de.gematik.epa.konnektor.client.EventServiceClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import telematik.ws.conn.cardservice.xsd.v8_1.CardInfoType;
import telematik.ws.conn.eventservice.xsd.v6_1.GetCardsResponse;

@Slf4j
@Accessors(fluent = true)
abstract class InformationProvider<T extends CardInformation> {

  private final Map<String, T> knownCards = new HashMap<>();

  protected final EventServiceClient eventServiceClient;

  @Getter private final CertificateServiceClient certificateServiceClient;

  protected InformationProvider(
      final KonnektorContextProvider konnektorContextProvider,
      final KonnektorInterfaceAssembly konnektorInterfaceAssembly) {
    eventServiceClient =
        new EventServiceClient(konnektorContextProvider, konnektorInterfaceAssembly);
    certificateServiceClient =
        new CertificateServiceClient(konnektorContextProvider, konnektorInterfaceAssembly);
  }

  protected abstract GetCardsResponse getCardsResponse();

  protected String getTelematikId(final CardInfoType cardInfo) {
    final var start = System.currentTimeMillis();
    final var telematikId = certificateServiceClient.getTelematikIdFromCard(cardInfo);
    log.info("Time to get telematikId: {}", System.currentTimeMillis() - start);
    return telematikId;
  }

  protected List<String> getProfessionOids(final CardInfoType cardInfo) {
    return certificateServiceClient.getProfessionOidsFromCard(cardInfo);
  }

  protected abstract T retrieveCardInformation(final CardInfoType cardInfo);

  public List<T> getCardsInformations() {
    final var inserted = getCardsResponse().getCards().getCard();
    final var unknownCards =
        inserted.stream().filter(cardInfo -> !knownCards.containsKey(cardInfo.getIccsn())).toList();

    unknownCards.stream()
        .map(this::retrieveCardInformation)
        .forEach(cardInfo -> knownCards.put(cardInfo.iccsn(), cardInfo));

    return inserted.stream().map(cardInfo -> knownCards.get(cardInfo.getIccsn())).toList();
  }
}
