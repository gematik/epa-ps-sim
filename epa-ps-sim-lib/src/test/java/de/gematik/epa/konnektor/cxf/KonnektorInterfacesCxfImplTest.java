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
package de.gematik.epa.konnektor.cxf;

import static de.gematik.epa.unit.util.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

import de.gematik.epa.api.testdriver.config.AddressConfig;
import de.gematik.epa.api.testdriver.config.KonnektorConnectionConfiguration;
import de.gematik.epa.konnektor.cxf.KonnektorInterfacesCxfImpl.FileLoader;
import de.gematik.epa.unit.util.ResourceLoader;
import de.gematik.epa.unit.util.TestDataFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import lombok.experimental.Accessors;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.junit.jupiter.api.Test;
import telematik.ws.conn.SdsApi;
import telematik.ws.conn.cardservice.wsdl.v8_1.CardServicePortType;
import telematik.ws.conn.cardservice.xsd.v8_1.PinStatusEnum;
import telematik.ws.conn.cardservicecommon.xsd.v2_0.PinResultEnum;
import telematik.ws.conn.eventservice.wsdl.v6_1.EventServicePortType;

class KonnektorInterfacesCxfImplTest {

  @Test
  void updateTest() {
    var connCfg = TestDataFactory.createKonnektorConnectionConfigurationMutable();
    var tstObj = new KonnektorInterfacesCxfImplForTest();

    var alsoTstObj = assertDoesNotThrow(() -> tstObj.update(connCfg));

    assertEquals(connCfg, alsoTstObj.configuration);
    assertTrue(alsoTstObj.isTlsPreferred());
    assertEquals(ResourceLoader.connectorServices(), alsoTstObj.connectorServices());
    assertClientProxy(alsoTstObj.eventService(), connCfg);
    assertClientProxy(alsoTstObj.certificateService(), connCfg);
    assertClientProxy(alsoTstObj.cardService(), connCfg);
    assertClientProxy(alsoTstObj.signatureService(), connCfg);
    assertClientProxy(alsoTstObj.authSignatureService(), connCfg);
  }

  @Test
  void unlockSmbTest() {
    var konnektorContextProvider = TestDataFactory.konnektorContextProvider();

    var tstObj = new KonnektorInterfacesCxfImplForTest();
    EventServicePortType eventServiceMock = mock(EventServicePortType.class);
    tstObj.eventService(eventServiceMock);

    CardServicePortType cardServiceMock = mock(CardServicePortType.class);
    tstObj.cardService(cardServiceMock);

    when(eventServiceMock.getCards(any())).thenReturn(TestDataFactory.getCardsSmbResponse());

    when(cardServiceMock.getPinStatus(any()))
        .thenReturn(TestDataFactory.getPinStatusResponse(PinStatusEnum.VERIFIABLE));
    var verifyPinResponse =
        TestDataFactory.verifyPin(TestDataFactory.getStatusOk(), PinResultEnum.OK);
    when(cardServiceMock.verifyPin(any())).thenReturn(verifyPinResponse);

    assertDoesNotThrow(() -> tstObj.unlockSmb(konnektorContextProvider));
  }

  @Test
  void shouldUnlockSmbs() {
    var konnektorContextProvider = TestDataFactory.konnektorContextProvider();
    var tstObj = new KonnektorInterfacesCxfImplForTest();
    EventServicePortType eventServiceMock = mock(EventServicePortType.class);
    tstObj.eventService(eventServiceMock);

    CardServicePortType cardServiceMock = mock(CardServicePortType.class);
    tstObj.cardService(cardServiceMock);

    when(eventServiceMock.getCards(any())).thenReturn(TestDataFactory.getCardsSmbResponse());

    when(cardServiceMock.getPinStatus(any()))
        .thenReturn(TestDataFactory.getPinStatusResponse(PinStatusEnum.VERIFIABLE));
    var verifyPinResponse =
        TestDataFactory.verifyPin(TestDataFactory.getStatusOk(), PinResultEnum.OK);
    when(cardServiceMock.verifyPin(any())).thenReturn(verifyPinResponse);
    assertDoesNotThrow(() -> tstObj.unlockSmbs(konnektorContextProvider));
  }

  @Test
  void sdsApiTest() {
    var connCfg = TestDataFactory.createKonnektorConnectionConfigurationMutable();
    var konnektorInterface = new KonnektorInterfacesCxfImpl(new TestFileLoader());

    konnektorInterface.configuration = connCfg;

    var sdsApi = assertDoesNotThrow(konnektorInterface::sdsApi);

    assertNotNull(sdsApi);

    var sdsHttpConduit = WebClient.getConfig(sdsApi).getHttpConduit();

    assertHttpConduit(sdsHttpConduit, connCfg);
  }

  @Test
  void sdsApiHttpTest() {
    var connCfg = TestDataFactory.createKonnektorConnectionConfigurationMutable();
    connCfg.address(new AddressConfig("localhost", 80, "http", "the/path"));
    var konnektorInterface = new KonnektorInterfacesCxfImpl(new TestFileLoader());
    konnektorInterface.configuration = connCfg;
    konnektorInterface.isTlsPreferred = Boolean.FALSE;

    var sdsApi = assertDoesNotThrow(konnektorInterface::sdsApi);

    assertNotNull(sdsApi);

    var sdsHttpConduit = WebClient.getConfig(sdsApi).getHttpConduit();

    assertNull(sdsHttpConduit.getTlsClientParameters());
  }

  private void assertClientProxy(
      Object clientProxyObject, KonnektorConnectionConfiguration connCfg) {
    assertNotNull(clientProxyObject);

    var httpConduit = (HTTPConduit) ClientProxy.getClient(clientProxyObject).getConduit();

    assertHttpConduit(httpConduit, connCfg);
  }

  private void assertHttpConduit(
      HTTPConduit httpConduit, KonnektorConnectionConfiguration connCfg) {
    assertNotNull(httpConduit);

    assertTlsConfig(connCfg.tlsConfig(), httpConduit.getTlsClientParameters());

    assertAuthorization(connCfg.basicAuthentication(), httpConduit.getAuthorization());

    assertProxy(connCfg.proxyAddress(), httpConduit.getClient());
  }

  @Accessors(fluent = true)
  static class KonnektorInterfacesCxfImplForTest extends KonnektorInterfacesCxfImpl {

    public KonnektorInterfacesCxfImplForTest() {
      super(new TestFileLoader());
    }

    @Override
    protected SdsApi sdsApi() {
      var thisSdsApi = mock(SdsApi.class);

      when(thisSdsApi.getConnectorSds()).thenReturn(ResourceLoader.connectorServices());

      return thisSdsApi;
    }
  }

  static class TestFileLoader implements FileLoader {

    @Override
    public InputStream process(String filePath) {
      return new ByteArrayInputStream(ResourceLoader.readBytesFromResource(filePath));
    }
  }
}
