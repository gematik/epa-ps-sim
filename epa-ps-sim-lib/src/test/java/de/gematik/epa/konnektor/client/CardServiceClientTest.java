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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.epa.konnektor.KonnektorUtils;
import de.gematik.epa.unit.util.KonnektorInterfaceAnswer;
import de.gematik.epa.unit.util.TestBase;
import de.gematik.epa.unit.util.TestDataFactory;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import telematik.ws.conn.cardservice.xsd.v8_1.*;
import telematik.ws.conn.cardservicecommon.xsd.v2_0.CardTypeType;
import telematik.ws.conn.cardservicecommon.xsd.v2_0.PinResultEnum;
import telematik.ws.conn.eventservice.xsd.v6_1.GetCardsResponse;

class CardServiceClientTest extends TestBase {

  private CardServiceClient testObj;

  static Stream<PinStatusEnum> pinStatus() {
    return Stream.of(PinStatusEnum.VERIFIED, PinStatusEnum.VERIFIABLE, PinStatusEnum.DISABLED);
  }

  static Stream<PinStatusEnum> pinStatusFailure() {
    return Stream.of(PinStatusEnum.EMPTY_PIN, PinStatusEnum.TRANSPORT_PIN, PinStatusEnum.BLOCKED);
  }

  static Stream<PinResultEnum> pinResultEnum() {
    return Stream.of(PinResultEnum.REJECTED, PinResultEnum.WASBLOCKED, PinResultEnum.NOWBLOCKED);
  }

  @BeforeEach
  void beforeEach() {
    testObj = new CardServiceClient(konnektorContextProvider(), konnektorInterfaceAssembly());
  }

  @Test
  void getPinStatusResponseTest() {
    var eventServiceMock = konnektorInterfaceAssembly().eventService();
    var cardServiceMock = konnektorInterfaceAssembly().cardService();

    var getCardsResponse = TestDataFactory.getCardsSmbResponse();
    var cardHandle = TestDataFactory.cardInfoSmb().getCardHandle();

    var getPinStatusResponse =
        new KonnektorInterfaceAnswer<GetPinStatus, GetPinStatusResponse>()
            .setAnswer(TestDataFactory.getPinStatusResponse(PinStatusEnum.VERIFIED));

    when(eventServiceMock.getCards(any())).thenReturn(getCardsResponse);
    when(cardServiceMock.getPinStatus(any())).then(getPinStatusResponse);

    var response =
        assertDoesNotThrow(
            () -> testObj.getPinStatusResponse(cardHandle, CardServiceClient.PIN_SMC));
    assertThat(response).isNotNull();
    assertThat(response.getStatus().getResult()).isEqualTo(KonnektorUtils.STATUS_OK);
    assertThat(response.getPinStatus()).isEqualTo(PinStatusEnum.VERIFIED);

    var request = getPinStatusResponse.getRequest();
    assertThat(request).isNotNull();
    assertThat(request.getPinTyp()).isEqualTo(CardServiceClient.PIN_SMC);
  }

  @ParameterizedTest
  @MethodSource("pinStatus")
  void verifyPinSuccessTest(PinStatusEnum pinStatusEnum) {
    var eventServiceMock = konnektorInterfaceAssembly().eventService();
    var cardServiceMock = konnektorInterfaceAssembly().cardService();

    var getCardsResponse = TestDataFactory.getCardsSmbResponse();
    var getPinStatusResponse = TestDataFactory.getPinStatusResponse(pinStatusEnum);
    var verifyPinResponse =
        TestDataFactory.verifyPin(TestDataFactory.getStatusOk(), PinResultEnum.OK);

    when(eventServiceMock.getCards(any())).thenReturn(getCardsResponse);
    when(cardServiceMock.getPinStatus(any())).thenReturn(getPinStatusResponse);
    when(cardServiceMock.verifyPin(any())).thenReturn(verifyPinResponse);

    assertDoesNotThrow(() -> testObj.verifyPin(CardTypeType.SM_B, CardServiceClient.PIN_SMC));
    verify(eventServiceMock).getCards(any());
    verify(cardServiceMock).getPinStatus(any());
  }

  @ParameterizedTest
  @MethodSource("pinStatusFailure")
  void verifyPinThrowsTest(PinStatusEnum pinStatusEnum) {
    var eventServiceMock = konnektorInterfaceAssembly().eventService();
    var cardServiceMock = konnektorInterfaceAssembly().cardService();
    when(eventServiceMock.getCards(any())).thenReturn(TestDataFactory.getCardsSmbResponse());
    when(cardServiceMock.getPinStatus(any()))
        .thenReturn(TestDataFactory.getPinStatusResponse(pinStatusEnum));
    assertThrows(
        IllegalStateException.class,
        () -> testObj.verifyPin(CardTypeType.SM_B, CardServiceClient.PIN_SMC));
  }

  @Test
  void verifyPinReturnsPinResultOk() {
    var eventServiceMock = konnektorInterfaceAssembly().eventService();
    var cardServiceMock = konnektorInterfaceAssembly().cardService();
    when(eventServiceMock.getCards(any())).thenReturn(TestDataFactory.getCardsSmbResponse());
    when(cardServiceMock.getPinStatus(any()))
        .thenReturn(TestDataFactory.getPinStatusResponse(PinStatusEnum.VERIFIABLE));
    var verifyPinResponse =
        TestDataFactory.verifyPin(TestDataFactory.getStatusOk(), PinResultEnum.OK);
    when(cardServiceMock.verifyPin(any())).thenReturn(verifyPinResponse);
    assertDoesNotThrow(() -> testObj.verifyPin(CardTypeType.SM_B, CardServiceClient.PIN_SMC));
    verify(eventServiceMock).getCards(any());
    verify(cardServiceMock).getPinStatus(any());
  }

  @Test
  void verifyPinReturnsPinResultErrorAndWarning() {
    var eventServiceMock = konnektorInterfaceAssembly().eventService();
    var cardServiceMock = konnektorInterfaceAssembly().cardService();
    when(eventServiceMock.getCards(any())).thenReturn(TestDataFactory.getCardsSmbResponse());
    when(cardServiceMock.getPinStatus(any()))
        .thenReturn(TestDataFactory.getPinStatusResponse(PinStatusEnum.VERIFIABLE));
    var verifyPinResponse =
        TestDataFactory.verifyPin(TestDataFactory.getStatusWarning(), PinResultEnum.ERROR);
    when(cardServiceMock.verifyPin(any())).thenReturn(verifyPinResponse);
    assertThrows(
        IllegalStateException.class,
        () -> testObj.verifyPin(CardTypeType.SM_B, CardServiceClient.PIN_SMC));
    verify(eventServiceMock).getCards(any());
    verify(cardServiceMock).getPinStatus(any());
  }

  @ParameterizedTest
  @MethodSource("pinResultEnum")
  void verifyPinReturnsPinResultWithStatusOkThrowsTest(PinResultEnum pinResult) {
    var eventServiceMock = konnektorInterfaceAssembly().eventService();
    var cardServiceMock = konnektorInterfaceAssembly().cardService();
    when(eventServiceMock.getCards(any())).thenReturn(TestDataFactory.getCardsSmbResponse());
    when(cardServiceMock.getPinStatus(any()))
        .thenReturn(TestDataFactory.getPinStatusResponse(PinStatusEnum.VERIFIABLE));
    var verifyPinResponse = TestDataFactory.verifyPin(TestDataFactory.getStatusOk(), pinResult);
    when(cardServiceMock.verifyPin(any())).thenReturn(verifyPinResponse);
    assertThrows(
        IllegalStateException.class,
        () -> testObj.verifyPin(CardTypeType.SM_B, CardServiceClient.PIN_SMC));
    verify(eventServiceMock).getCards(any());
    verify(cardServiceMock).getPinStatus(any());
  }

  @Test
  void verifySmbTest() {
    var eventServiceMock = konnektorInterfaceAssembly().eventService();
    var cardServiceMock = konnektorInterfaceAssembly().cardService();
    when(eventServiceMock.getCards(any())).thenReturn(TestDataFactory.getCardsSmbResponse());
    when(cardServiceMock.getPinStatus(any()))
        .thenReturn(TestDataFactory.getPinStatusResponse(PinStatusEnum.VERIFIABLE));
    var verifyPinResponse =
        TestDataFactory.verifyPin(TestDataFactory.getStatusOk(), PinResultEnum.OK);
    when(cardServiceMock.verifyPin(any())).thenReturn(verifyPinResponse);
    assertDoesNotThrow(() -> testObj.verifySmb());
    verify(eventServiceMock).getCards(any());
    verify(cardServiceMock).getPinStatus(any());
    verify(cardServiceMock).verifyPin(any());
  }

  @Test
  void verifyPinsVerifiesAllCardHandles() {
    var eventServiceMock = konnektorInterfaceAssembly().eventService();
    var cardServiceMock = konnektorInterfaceAssembly().cardService();

    final Cards cards =
        new Cards().withCard(TestDataFactory.cardInfoSmb(), TestDataFactory.cardInfoHba());
    final var getCardsResponse =
        new GetCardsResponse().withStatus(TestDataFactory.getStatusOk()).withCards(cards);
    var cardHandles =
        getCardsResponse.getCards().getCard().stream().map(CardInfoType::getCardHandle).toList();

    when(eventServiceMock.getCards(any())).thenReturn(getCardsResponse);
    when(cardServiceMock.getPinStatus(any()))
        .thenReturn(TestDataFactory.getPinStatusResponse(PinStatusEnum.VERIFIABLE));
    when(cardServiceMock.verifyPin(any()))
        .thenReturn(TestDataFactory.verifyPin(TestDataFactory.getStatusOk(), PinResultEnum.OK));

    assertDoesNotThrow(() -> testObj.verifyPins(CardTypeType.SM_B, CardServiceClient.PIN_SMC));
    verify(eventServiceMock).getCards(any());
    verify(cardServiceMock, times(cardHandles.size())).getPinStatus(any());
    verify(cardServiceMock, times(cardHandles.size())).verifyPin(any());
  }

  @Test
  void verifyPinsThrowsIfAnyCardHasBlockedPin() {
    var eventServiceMock = konnektorInterfaceAssembly().eventService();
    var cardServiceMock = konnektorInterfaceAssembly().cardService();

    final Cards cards =
        new Cards().withCard(TestDataFactory.cardInfoSmb(), TestDataFactory.cardInfoHba());
    final var getCardsResponse =
        new GetCardsResponse().withStatus(TestDataFactory.getStatusOk()).withCards(cards);
    when(eventServiceMock.getCards(any())).thenReturn(getCardsResponse);
    when(cardServiceMock.getPinStatus(any()))
        .thenReturn(TestDataFactory.getPinStatusResponse(PinStatusEnum.VERIFIABLE));

    when(cardServiceMock.getPinStatus(any()))
        .thenReturn(TestDataFactory.getPinStatusResponse(PinStatusEnum.BLOCKED));
    assertThrows(
        IllegalStateException.class,
        () -> testObj.verifyPins(CardTypeType.SM_B, CardServiceClient.PIN_SMC));
    verify(eventServiceMock).getCards(any());
    verify(cardServiceMock, atLeastOnce()).getPinStatus(any());
  }

  @Test
  void verifyPinSingleCardTest() {
    var eventServiceMock = konnektorInterfaceAssembly().eventService();
    var cardServiceMock = konnektorInterfaceAssembly().cardService();

    when(eventServiceMock.getCards(any())).thenReturn(TestDataFactory.getCardsSmbResponse());
    when(cardServiceMock.getPinStatus(any()))
        .thenReturn(TestDataFactory.getPinStatusResponse(PinStatusEnum.VERIFIABLE));
    when(cardServiceMock.verifyPin(any()))
        .thenReturn(TestDataFactory.verifyPin(TestDataFactory.getStatusOk(), PinResultEnum.OK));

    assertDoesNotThrow(
        () -> testObj.verifyPin(CardTypeType.SM_B, CardServiceClient.PIN_SMC, false));
    verify(eventServiceMock).getCards(any());
    verify(cardServiceMock).getPinStatus(any());
    verify(cardServiceMock).verifyPin(any());
  }

  @Test
  void verifyPinMultipleCardsTest() {
    var eventServiceMock = konnektorInterfaceAssembly().eventService();
    var cardServiceMock = konnektorInterfaceAssembly().cardService();

    final Cards cards =
        new Cards().withCard(TestDataFactory.cardInfoSmb(), TestDataFactory.cardInfoSmb());
    final var getCardsResponse =
        new GetCardsResponse().withStatus(TestDataFactory.getStatusOk()).withCards(cards);

    when(eventServiceMock.getCards(any())).thenReturn(getCardsResponse);
    when(cardServiceMock.getPinStatus(any()))
        .thenReturn(TestDataFactory.getPinStatusResponse(PinStatusEnum.VERIFIABLE));
    when(cardServiceMock.verifyPin(any()))
        .thenReturn(TestDataFactory.verifyPin(TestDataFactory.getStatusOk(), PinResultEnum.OK));

    assertDoesNotThrow(() -> testObj.verifyPin(CardTypeType.SM_B, CardServiceClient.PIN_SMC, true));
    verify(eventServiceMock).getCards(any());
    verify(cardServiceMock, times(2)).getPinStatus(any());
    verify(cardServiceMock, times(2)).verifyPin(any());
  }
}
