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

import static de.gematik.epa.konnektor.client.CertificateServiceClient.OID_PRAXIS_ARZT;
import static org.junit.jupiter.api.Assertions.*;

import de.gematik.epa.unit.util.KonnektorInterfaceAnswer;
import de.gematik.epa.unit.util.TestBase;
import de.gematik.epa.unit.util.TestDataFactory;
import java.util.List;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import telematik.ws.conn.certificateservice.wsdl.v6_0.CertificateServicePortType;
import telematik.ws.conn.certificateservice.wsdl.v6_0.FaultMessage;
import telematik.ws.conn.certificateservice.xsd.v6_0.ReadCardCertificate;
import telematik.ws.conn.certificateservice.xsd.v6_0.ReadCardCertificateResponse;

@Accessors(fluent = true)
class CertificateServiceClientTest extends TestBase {

  CertificateServiceClient tstObj =
      new CertificateServiceClient(konnektorContextProvider(), konnektorInterfaceAssembly());

  @Getter(lazy = true)
  private final CertificateServicePortType certPortMock =
      konnektorInterfaceAssembly().certificateService();

  @SneakyThrows
  @Test
  void getTelematikIdFromCard() {
    var response = TestDataFactory.readCardCertificateResponse();
    var answer =
        new KonnektorInterfaceAnswer<ReadCardCertificate, ReadCardCertificateResponse>()
            .setAnswer(response);
    var cardInfo = TestDataFactory.cardInfoSmb();

    Mockito.when(certPortMock().readCardCertificate(Mockito.any())).then(answer);

    var telematikId = assertDoesNotThrow(() -> tstObj.getTelematikIdFromCard(cardInfo));
    var professionOids = assertDoesNotThrow(() -> tstObj.getProfessionOidsFromCard(cardInfo));

    assertNotNull(telematikId);
    assertEquals(TestDataFactory.SMB_AUT_TELEMATIK_ID, telematikId);

    assertNotNull(professionOids);
    assertArrayEquals(List.of(OID_PRAXIS_ARZT).toArray(), professionOids.toArray());

    var request = answer.getRequest();

    assertEquals(cardInfo.getCardHandle(), request.getCardHandle());
    assertNotNull(request.getCertRefList());
    assertFalse(request.getCertRefList().getCertRef().isEmpty());
  }

  @SneakyThrows
  @Test
  void readCardCertificateThrowsTest() {
    var telematikError = TestDataFactory.getTelematikError();

    Mockito.when(certPortMock().readCardCertificate(Mockito.any()))
        .thenThrow(new FaultMessage("readCardCertificateThrowsTest Fault", telematikError));

    var request = new ReadCardCertificate();

    var exception = assertThrows(FaultMessage.class, () -> tstObj.readCardCertificate(request));

    assertNotNull(exception);
    assertEquals(telematikError, exception.getFaultInfo());
  }
}
