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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import de.gematik.epa.unit.util.KonnektorInterfaceAnswer;
import de.gematik.epa.unit.util.TestBase;
import de.gematik.epa.unit.util.TestDataFactory;
import java.util.NoSuchElementException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import telematik.ws.conn.cardservice.xsd.v8_1.Cards;
import telematik.ws.conn.cardservicecommon.xsd.v2_0.CardTypeType;
import telematik.ws.conn.eventservice.wsdl.v6_1.FaultMessage;
import telematik.ws.conn.eventservice.xsd.v6_1.GetCards;
import telematik.ws.conn.eventservice.xsd.v6_1.GetCardsResponse;

class EventServiceClientTest extends TestBase {

  private final EventServiceClient eventServiceClient =
      new EventServiceClient(konnektorContextProvider(), konnektorInterfaceAssembly());

  @SneakyThrows
  @Test
  void getSmbInfo() {
    // Arrange
    final var eventServiceMock = konnektorInterfaceAssembly().eventService();
    final var answer =
        new KonnektorInterfaceAnswer<GetCards, GetCardsResponse>()
            .setAnswer(TestDataFactory.getCardsSmbResponse());

    when(eventServiceMock.getCards(any())).then(answer);

    // Act
    final var result = assertDoesNotThrow(eventServiceClient::getSmbInfo);

    // Assert
    assertThat(result).isNotNull();

    final var request = answer.getRequest();
    assertThat(request).isNotNull();
    assertThat(request.getCardType()).isEqualTo(CardTypeType.SM_B);
  }

  @SneakyThrows
  @Test
  void getHbaInfo() {
    // Arrange
    final var eventServiceMock = konnektorInterfaceAssembly().eventService();
    final var answer =
        new KonnektorInterfaceAnswer<GetCards, GetCardsResponse>()
            .setAnswer(TestDataFactory.getCardsHbaResponse());

    when(eventServiceMock.getCards(any())).then(answer);

    // Act
    final var result = assertDoesNotThrow(eventServiceClient::getHbaInfo);

    // Assert
    assertThat(result).isNotNull();

    final var request = answer.getRequest();
    assertThat(request).isNotNull();
    assertThat(request.getCardType()).isEqualTo(CardTypeType.HBA);
  }

  @SneakyThrows
  @Test
  void getEgkInfo() {
    // Arrange
    final var eventServiceMock = konnektorInterfaceAssembly().eventService();
    final var answer =
        new KonnektorInterfaceAnswer<GetCards, GetCardsResponse>()
            .setAnswer(TestDataFactory.getCardsEgkResponse(TestDataFactory.KVNR));

    when(eventServiceMock.getCards(any())).then(answer);

    // Act
    final var result = assertDoesNotThrow(eventServiceClient::getEgkInfo);

    // Assert
    assertThat(result).isNotNull();

    final var request = answer.getRequest();
    assertThat(request).isNotNull();
    assertThat(request.getCardType()).isEqualTo(CardTypeType.EGK);
  }

  @SneakyThrows
  @Test
  void getCardsThrowsTest() {
    // Arrange
    final var eventServiceMock = konnektorInterfaceAssembly().eventService();

    final var faultMessage =
        new FaultMessage("Some error occurred", TestDataFactory.getTelematikError());

    when(eventServiceMock.getCards(any())).thenThrow(faultMessage);

    final var request = new GetCards();

    // Act
    final var exception =
        assertThrows(FaultMessage.class, () -> eventServiceClient.getCards(request));

    // Assert
    assertThat(exception.getClass()).isEqualTo(faultMessage.getClass());
    assertThat(exception.getFaultInfo()).isEqualTo(faultMessage.getFaultInfo());
  }

  @SneakyThrows
  @Test
  void getEgkInfoToKvnr() {
    // Arrange
    final var eventServiceMock = konnektorInterfaceAssembly().eventService();
    final var answer =
        new KonnektorInterfaceAnswer<GetCards, GetCardsResponse>()
            .setAnswer(TestDataFactory.getCardsEgkResponse(TestDataFactory.KVNR));

    when(eventServiceMock.getCards(any())).then(answer);

    // Act
    final var result =
        assertDoesNotThrow(() -> eventServiceClient.getEgkInfoToKvnr(TestDataFactory.KVNR));

    // Assert
    assertThat(result).isNotNull();

    final var request = answer.getRequest();
    assertThat(request).isNotNull();
    assertThat(request.getCardType()).isEqualTo(CardTypeType.EGK);
  }

  @Test
  void getCardHandleTest() {
    // Arrange
    final var eventServiceMock = konnektorInterfaceAssembly().eventService();
    final var getCardsResponse = TestDataFactory.getCardsSmbResponse();
    when(eventServiceMock.getCards(any())).thenReturn(getCardsResponse);

    // Act
    final var cardHandle =
        assertDoesNotThrow(() -> eventServiceClient.getCardHandle(CardTypeType.SM_B));

    // Assert
    assertThat(cardHandle).isNotNull();
    assertThat(getCardsResponse.getCards().getCard()).isNotEmpty();
    assertThat(getCardsResponse.getCards().getCard().getFirst().getCardHandle())
        .isEqualTo(cardHandle);
  }

  @Test
  void getCardHandleNoCardTest() {
    // Arrange
    final var eventServiceMock = konnektorInterfaceAssembly().eventService();
    final var getCardsResponse =
        new GetCardsResponse().withStatus(TestDataFactory.getStatusOk()).withCards(new Cards());
    when(eventServiceMock.getCards(any())).thenReturn(getCardsResponse);

    // Act & Assert
    assertThrows(
        NoSuchElementException.class, () -> eventServiceClient.getCardHandle(CardTypeType.HB_AX));
  }

  @Test
  void getCardHandlesShouldReturnsFirst() {
    final var eventServiceMock = konnektorInterfaceAssembly().eventService();
    final Cards cards =
        new Cards().withCard(TestDataFactory.cardInfoSmb(), TestDataFactory.cardInfoHba());
    final var getCardsResponse =
        new GetCardsResponse().withStatus(TestDataFactory.getStatusOk()).withCards(cards);
    when(eventServiceMock.getCards(any())).thenReturn(getCardsResponse);

    final var cardHandles =
        assertDoesNotThrow(() -> eventServiceClient.getCardHandles(CardTypeType.SM_B, true));

    assertThat(cardHandles).isNotNull().hasSize(1);
    assertThat(cardHandles.getFirst())
        .isEqualTo(getCardsResponse.getCards().getCard().getFirst().getCardHandle());
  }

  @Test
  void getCardHandlesShouldReturnsAll() {
    final var eventServiceMock = konnektorInterfaceAssembly().eventService();
    final Cards cards =
        new Cards().withCard(TestDataFactory.cardInfoSmb(), TestDataFactory.cardInfoHba());
    final var getCardsResponse =
        new GetCardsResponse().withStatus(TestDataFactory.getStatusOk()).withCards(cards);
    when(eventServiceMock.getCards(any())).thenReturn(getCardsResponse);

    final var cardHandles =
        assertDoesNotThrow(() -> eventServiceClient.getCardHandles(CardTypeType.SM_B, false));

    assertThat(cardHandles).isNotNull().hasSize(2);
  }

  @Test
  void getCardHandlesReturnsEmptyListWhenNoCardsFound() {
    final var eventServiceMock = konnektorInterfaceAssembly().eventService();
    final var getCardsResponse =
        new GetCardsResponse().withStatus(TestDataFactory.getStatusOk()).withCards(new Cards());
    when(eventServiceMock.getCards(any())).thenReturn(getCardsResponse);

    final var cardHandles =
        assertDoesNotThrow(() -> eventServiceClient.getCardHandles(CardTypeType.HBA, false));

    assertThat(cardHandles).isNotNull().isEmpty();
  }

  @Test
  void getFirstCardHandleReturnsThrowExceptionWhenNoCardsFound() {
    final var eventServiceMock = konnektorInterfaceAssembly().eventService();
    final var getCardsResponse =
        new GetCardsResponse().withStatus(TestDataFactory.getStatusOk()).withCards(new Cards());
    when(eventServiceMock.getCards(any())).thenReturn(getCardsResponse);

    assertThrows(
        NoSuchElementException.class,
        () -> eventServiceClient.getCardHandles(CardTypeType.HBA, true));
  }
}
