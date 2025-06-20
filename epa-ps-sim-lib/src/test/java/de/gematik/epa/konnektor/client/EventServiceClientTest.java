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
package de.gematik.epa.konnektor.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.gematik.epa.unit.util.KonnektorInterfaceAnswer;
import de.gematik.epa.unit.util.TestBase;
import de.gematik.epa.unit.util.TestDataFactory;
import java.util.NoSuchElementException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import telematik.ws.conn.cardservice.xsd.v8_1.Cards;
import telematik.ws.conn.cardservicecommon.xsd.v2_0.CardTypeType;
import telematik.ws.conn.eventservice.wsdl.v6_1.FaultMessage;
import telematik.ws.conn.eventservice.xsd.v6_1.GetCards;
import telematik.ws.conn.eventservice.xsd.v6_1.GetCardsResponse;

class EventServiceClientTest extends TestBase {

  @SneakyThrows
  @Test
  void getSmbInfo() {
    // Arrange
    final var eventServiceMock = konnektorInterfaceAssembly().eventService();
    final var answer =
        new KonnektorInterfaceAnswer<GetCards, GetCardsResponse>()
            .setAnswer(TestDataFactory.getCardsSmbResponse());

    Mockito.when(eventServiceMock.getCards(Mockito.any())).then(answer);

    // Act
    final var result =
        assertDoesNotThrow(
            () ->
                new EventServiceClient(konnektorContextProvider(), konnektorInterfaceAssembly())
                    .getSmbInfo());

    // Assert
    assertNotNull(result);

    final var request = answer.getRequest();
    assertNotNull(request);
    assertEquals(CardTypeType.SM_B, request.getCardType());
  }

  @SneakyThrows
  @Test
  void getHbaInfo() {
    // Arrange
    final var eventServiceMock = konnektorInterfaceAssembly().eventService();
    final var answer =
        new KonnektorInterfaceAnswer<GetCards, GetCardsResponse>()
            .setAnswer(TestDataFactory.getCardsHbaResponse());

    Mockito.when(eventServiceMock.getCards(Mockito.any())).then(answer);

    // Act
    final var result =
        assertDoesNotThrow(
            () ->
                new EventServiceClient(konnektorContextProvider(), konnektorInterfaceAssembly())
                    .getHbaInfo());

    // Assert
    assertNotNull(result);

    final var request = answer.getRequest();
    assertNotNull(request);
    assertEquals(CardTypeType.HBA, request.getCardType());
  }

  @SneakyThrows
  @Test
  void getEgkInfo() {
    // Arrange
    final var eventServiceMock = konnektorInterfaceAssembly().eventService();
    final var answer =
        new KonnektorInterfaceAnswer<GetCards, GetCardsResponse>()
            .setAnswer(TestDataFactory.getCardsEgkResponse(TestDataFactory.KVNR));

    Mockito.when(eventServiceMock.getCards(Mockito.any())).then(answer);

    // Act
    final var result =
        assertDoesNotThrow(
            () ->
                new EventServiceClient(konnektorContextProvider(), konnektorInterfaceAssembly())
                    .getEgkInfo());

    // Assert
    assertNotNull(result);

    final var request = answer.getRequest();
    assertNotNull(request);
    assertEquals(CardTypeType.EGK, request.getCardType());
  }

  @SneakyThrows
  @Test
  void getCardsThrowsTest() {
    // Arrange
    final var eventServiceMock = konnektorInterfaceAssembly().eventService();

    final var faultMessage =
        new FaultMessage("Some error occurred", TestDataFactory.getTelematikError());

    Mockito.when(eventServiceMock.getCards(Mockito.any())).thenThrow(faultMessage);

    final var tstObj =
        new EventServiceClient(konnektorContextProvider(), konnektorInterfaceAssembly());
    final var request = new GetCards();

    // Act
    final var exception = assertThrows(FaultMessage.class, () -> tstObj.getCards(request));

    // Assert
    assertEquals(faultMessage.getClass(), exception.getClass());
    assertEquals(faultMessage.getFaultInfo(), exception.getFaultInfo());
  }

  @SneakyThrows
  @Test
  void getEgkInfoToKvnr() {
    // Arrange
    final var eventServiceMock = konnektorInterfaceAssembly().eventService();
    final var answer =
        new KonnektorInterfaceAnswer<GetCards, GetCardsResponse>()
            .setAnswer(TestDataFactory.getCardsEgkResponse(TestDataFactory.KVNR));

    Mockito.when(eventServiceMock.getCards(Mockito.any())).then(answer);

    // Act
    final var result =
        assertDoesNotThrow(
            () ->
                new EventServiceClient(konnektorContextProvider(), konnektorInterfaceAssembly())
                    .getEgkInfoToKvnr(TestDataFactory.KVNR));

    // Assert
    assertNotNull(result);

    final var request = answer.getRequest();
    assertNotNull(request);
    assertEquals(CardTypeType.EGK, request.getCardType());
  }

  @Test
  void getCardHandleTest() {
    // Arrange
    final var eventServiceMock = konnektorInterfaceAssembly().eventService();
    final var getCardsResponse = TestDataFactory.getCardsSmbResponse();
    Mockito.when(eventServiceMock.getCards(Mockito.any())).thenReturn(getCardsResponse);

    final var tstObj =
        new EventServiceClient(konnektorContextProvider(), konnektorInterfaceAssembly());

    // Act
    final var cardHandle = assertDoesNotThrow(() -> tstObj.getCardHandle(CardTypeType.SM_B));

    // Assert
    assertNotNull(cardHandle);
    assertEquals(getCardsResponse.getCards().getCard().get(0).getCardHandle(), cardHandle);
  }

  @Test
  void getCardHandleNoCardTest() {
    // Arrange
    final var eventServiceMock = konnektorInterfaceAssembly().eventService();
    final var getCardsResponse =
        new GetCardsResponse().withStatus(TestDataFactory.getStatusOk()).withCards(new Cards());
    Mockito.when(eventServiceMock.getCards(Mockito.any())).thenReturn(getCardsResponse);

    final var tstObj =
        new EventServiceClient(konnektorContextProvider(), konnektorInterfaceAssembly());

    // Act & Assert
    assertThrows(NoSuchElementException.class, () -> tstObj.getCardHandle(CardTypeType.HB_AX));
  }
}
