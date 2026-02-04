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
package de.gematik.epa.konnektor.client;

import de.gematik.epa.konnektor.KonnektorContextProvider;
import de.gematik.epa.konnektor.KonnektorInterfaceAssembly;
import de.gematik.epa.konnektor.KonnektorUtils;
import de.gematik.epa.utils.CardLogger;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import telematik.ws.conn.cardservice.xsd.v8_1.CardInfoType;
import telematik.ws.conn.cardservicecommon.xsd.v2_0.CardTypeType;
import telematik.ws.conn.connectorcontext.xsd.v2_0.ContextType;
import telematik.ws.conn.eventservice.wsdl.v6_1.EventServicePortType;
import telematik.ws.conn.eventservice.xsd.v6_1.GetCards;
import telematik.ws.conn.eventservice.xsd.v6_1.GetCardsResponse;
import telematik.ws.conn.eventservice.xsd.v6_1.ObjectFactory;

@Accessors(fluent = true)
@Slf4j
public class EventServiceClient extends KonnektorServiceClient {

  private final CardLogger cardLogger = new CardLogger();
  private EventServicePortType eventService;
  private ContextType context;

  public EventServiceClient(
      final KonnektorContextProvider konnektorContextProvider,
      final KonnektorInterfaceAssembly konnektorInterfaceAssembly) {
    super(konnektorContextProvider, konnektorInterfaceAssembly);
    runInitializationSynchronized();
  }

  public GetCardsResponse getSmbInfo() {
    return getCardsInfo(CardTypeType.SM_B);
  }

  public GetCardsResponse getHbaInfo() {
    return getCardsInfo(CardTypeType.HBA);
  }

  public GetCardsResponse getEgkInfo() {
    return getCardsInfo(CardTypeType.EGK);
  }

  public CardInfoType getEgkInfoToKvnr(@NonNull final String kvnr) {
    final var response = getEgkInfo();

    KonnektorUtils.logWarningIfPresent(
        log, response.getStatus(), KonnektorUtils.warnMsgWithOperationName("getCards"));

    return response.getCards().getCard().stream()
        .filter(ci -> kvnr.equals(ci.getKvnr()))
        .findFirst()
        .orElse(null);
  }

  /**
   * Retrieves all card handles for the specified card type.
   *
   * @param cardType the type of card
   * @return a List of all card handles
   */
  public List<String> getCardHandles(final CardTypeType cardType) {
    return getCardHandles(cardType, false);
  }

  /**
   * Retrieves the first card handle for the specified card type.
   *
   * @param cardType the type of card
   * @return the first card handle
   * @throws NoSuchElementException if no card is found
   */
  public String getCardHandle(final CardTypeType cardType) {
    return getCardHandles(cardType, true).getFirst();
  }

  /**
   * Retrieves card handles for the specified card type.
   *
   * @param cardType the type of card
   * @param firstOnly if true, returns only the first card handle; otherwise, returns all handles
   * @return if firstOnly is true, returns a List with a single card handle or empty if no card
   *     found; otherwise returns a List of all card handles
   * @throws NoSuchElementException if firstOnly is true and no card is found
   */
  protected List<String> getCardHandles(final CardTypeType cardType, final boolean firstOnly) {
    var cards = getCardsInfo(cardType).getCards().getCard();

    if (firstOnly) {
      return cards.stream()
          .peek(cardLogger)
          .findFirst()
          .map(CardInfoType::getCardHandle)
          .map(List::of)
          .orElseThrow(
              () ->
                  new NoSuchElementException(
                      String.format("No %s card was present in the Konnektor", cardType.value())));
    }

    return cards.stream().peek(cardLogger).map(CardInfoType::getCardHandle).toList();
  }

  GetCardsResponse getCards(@NonNull final GetCards request) {
    return eventService.getCards(request);
  }

  @Override
  protected void initialize() {
    context = konnektorContextProvider.getContext();
    eventService = konnektorInterfaceAssembly.eventService();
  }

  private GetCardsResponse getCardsInfo(final CardTypeType cardType) {
    final var request = buildGetCards(true, cardType);
    return getCards(request);
  }

  // region private

  private GetCards buildGetCards(final boolean mandantWide, final CardTypeType cardType) {
    final var getCardsRequest = new ObjectFactory().createGetCards();
    getCardsRequest.setCardType(cardType);
    getCardsRequest.setContext(context);
    getCardsRequest.setMandantWide(mandantWide);
    return getCardsRequest;
  }

  // endregion private
}
