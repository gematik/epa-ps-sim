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

import de.gematik.epa.api.testdriver.card.dto.EgkInformationDTO;
import de.gematik.epa.api.testdriver.card.dto.GetCardsInfoResponseDTO;
import de.gematik.epa.api.testdriver.card.dto.HbaInformationDTO;
import de.gematik.epa.api.testdriver.card.dto.SmbInformationDTO;
import de.gematik.epa.data.HbaInformation;
import de.gematik.epa.data.SmbInformation;
import de.gematik.epa.konnektor.HbaInformationProvider;
import de.gematik.epa.konnektor.SmbInformationProvider;
import de.gematik.epa.konnektor.client.EventServiceClient;
import de.gematik.epa.unit.util.TestDataFactory;
import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import telematik.ws.conn.cardservice.xsd.v8_1.CardInfoType;
import telematik.ws.conn.cardservice.xsd.v8_1.Cards;
import telematik.ws.conn.cardservicecommon.xsd.v2_0.CardTypeType;
import telematik.ws.conn.eventservice.xsd.v6_1.GetCardsResponse;

class CardApiImplTest {

  private SmbInformationProvider smbInformationProvider;
  private EventServiceClient eventServiceClient;
  private HbaInformationProvider hbaInformationProvider;
  private CardApiImpl cardApi;

  @BeforeEach
  void setUp() {
    smbInformationProvider = mock(SmbInformationProvider.class);
    eventServiceClient = mock(EventServiceClient.class);
    hbaInformationProvider = mock(HbaInformationProvider.class);
    cardApi = new CardApiImpl(smbInformationProvider, eventServiceClient, hbaInformationProvider);
  }

  @Test
  void getCardsInfo_returnsCorrectResponse_whenCardsArePresent() {
    // Arrange
    final SmbInformation smbInformation = TestDataFactory.createSmbInformation();
    when(smbInformationProvider.getCardsInformations()).thenReturn(List.of(smbInformation));
    final HbaInformation hbaInformation = TestDataFactory.createHbaInformation();
    when(hbaInformationProvider.getCardsInformations()).thenReturn(List.of(hbaInformation));

    final var cardsResponce = new GetCardsResponse();
    final var cards = new Cards();
    final var cardInfo = cardInfoEgk();
    cards.withCard(cardInfo);
    cardsResponce.setCards(cards);
    when(eventServiceClient.getEgkInfo()).thenReturn(cardsResponce);

    // Act
    final var response = cardApi.getCardsInfo();

    // Assert
    final var expectedResponse =
        new GetCardsInfoResponseDTO()
            .success(true)
            .smbInfo(
                List.of(
                    new SmbInformationDTO()
                        .cardHandle(smbInformation.cardHandle())
                        .cardHolderName(smbInformation.cardHolderName())
                        .iccsn(smbInformation.iccsn())
                        .telematikId(smbInformation.telematikId())))
            .egkInfo(
                List.of(
                    new EgkInformationDTO()
                        .kvnr(cardInfo.getKvnr())
                        .iccsn(cardInfo.getIccsn())
                        .cardHolderName(cardInfo.getCardHolderName())
                        .cardHandle(cardInfo.getCardHandle())))
            .hbaInfo(
                List.of(
                    new HbaInformationDTO()
                        .cardHandle(hbaInformation.cardHandle())
                        .cardHolderName(hbaInformation.cardHolderName())
                        .iccsn(hbaInformation.iccsn())
                        .telematikId(hbaInformation.telematikId())));

    assertThat(response).isEqualTo(expectedResponse);
  }

  @Test
  void getCardsInfo_returnsEmptyList_whenNoCardsInformationsArePresent() {
    // Arrange
    when(smbInformationProvider.getCardsInformations()).thenReturn(List.of());
    final var cardsResponce = new GetCardsResponse();
    final var cards = new Cards();
    cardsResponce.setCards(cards);
    when(eventServiceClient.getEgkInfo()).thenReturn(cardsResponce);
    when(hbaInformationProvider.getCardsInformations()).thenReturn(List.of());

    // Act
    final var response = cardApi.getCardsInfo();

    // Assert
    final var expectedResponse = new GetCardsInfoResponseDTO().success(true);
    assertThat(response).isEqualTo(expectedResponse);
  }

  @Test
  void getCardsInfo_handlesNullCardsInformationsGracefully() {
    // Arrange
    when(smbInformationProvider.getCardsInformations()).thenReturn(null);
    when(eventServiceClient.getEgkInfo()).thenReturn(null);
    when(hbaInformationProvider.getCardsInformations()).thenReturn(null);

    // Act
    final var response = cardApi.getCardsInfo();

    // Assert
    final var expectedResponse = new GetCardsInfoResponseDTO().success(true);
    assertThat(response).isEqualTo(expectedResponse);
  }

  @Test
  void getCardsInfo_handlesThrowException() {
    // Arrange
    when(smbInformationProvider.getCardsInformations()).thenThrow(new RuntimeException("error"));

    // Act
    final var response = cardApi.getCardsInfo();

    // Assert
    final var expectedResponse =
        new GetCardsInfoResponseDTO().success(false).statusMessage("error");
    assertThat(response).isEqualTo(expectedResponse);
  }

  private static CardInfoType cardInfoEgk() {
    final var cardInfo = new CardInfoType();
    cardInfo.setCardType(CardTypeType.EGK);
    cardInfo.setCardVersion(new CardInfoType.CardVersion());
    cardInfo.setCardHandle("EGK456");
    cardInfo.setCardHolderName("Elfriede Barbara Gudrun Müller");
    cardInfo.setIccsn("80271282320235252170");
    cardInfo.setCtId("CT1");
    cardInfo.setSlotId(BigInteger.valueOf(2));
    cardInfo.setKvnr("X110435031");

    return cardInfo;
  }
}
