/*-
 * #%L
 * epa-ps-sim-app
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
package de.gematik.epa.unit;

import static de.gematik.epa.unit.AppTestDataFactory.*;

import de.gematik.epa.konnektor.cxf.KonnektorInterfacesCxfImpl;
import de.gematik.epa.ps.konnektor.KonnektorClientConfiguration;
import de.gematik.epa.ps.konnektor.config.KonnektorConfigurationData;
import de.gematik.epa.ps.utils.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.mockito.Mockito;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;
import telematik.ws.conn.authsignatureservice.wsdl.v7_4.AuthSignatureServicePortType;
import telematik.ws.conn.cardservice.wsdl.v8_1.CardServicePortType;
import telematik.ws.conn.cardservice.xsd.v8_1.PinStatusEnum;
import telematik.ws.conn.cardservicecommon.xsd.v2_0.CardTypeType;
import telematik.ws.conn.certificateservice.wsdl.v6_0.CertificateServicePortType;
import telematik.ws.conn.eventservice.wsdl.v6_1.EventServicePortType;
import telematik.ws.conn.eventservice.xsd.v6_1.GetCards;
import telematik.ws.conn.vsds.vsdservice.wsdl.v5_2.VSDServicePortType;
import telematik.ws.conn.vsds.vsdservice.xsd.v5_2.ReadVSDResponse;

@TestConfiguration
@ComponentScan("de.gematik.epa.ps")
@Profile("test")
@Slf4j
@EnableConfigurationProperties({KonnektorConfigurationData.class})
public class TestKonnektorClientConfiguration extends KonnektorClientConfiguration {
  protected VSDServicePortType vsdService = Mockito.mock(VSDServicePortType.class);

  public TestKonnektorClientConfiguration(
      KonnektorConfigurationData konnektorConfiguration, ResourceLoader resourceLoader) {
    super(konnektorConfiguration, resourceLoader);
  }

  @Override
  protected KonnektorInterfacesCxfImpl createNewKonnektorInterfaceAssembly() {
    return createKonnektorInterfaceCxfImpl();
  }

  protected KonnektorInterfacesCxfImpl createKonnektorInterfaceCxfImpl() {
    KonnektorInterfacesCxfImpl konnektorInterfacesCxf =
        new KonnektorInterfacesCxfImpl(
            filePath ->
                SpringUtils.findReadableResource(resourceLoader, filePath).getInputStream());
    var eventService = Mockito.mock(EventServicePortType.class);
    var cardService = Mockito.mock(CardServicePortType.class);
    var authSignatureService = Mockito.mock(AuthSignatureServicePortType.class);
    var certificateService = Mockito.mock(CertificateServicePortType.class);

    konnektorInterfacesCxf.cardService(cardService);
    konnektorInterfacesCxf.eventService(eventService);
    konnektorInterfacesCxf.authSignatureService(authSignatureService);
    konnektorInterfacesCxf.certificateService(certificateService);
    konnektorInterfacesCxf.vsdService(vsdService);

    Mockito.when(eventService.getCards(Mockito.any()))
        .thenAnswer(
            input -> {
              // get the first argument passed
              var getCards = input.getArgument(0);
              if (getCards instanceof GetCards) {
                var cardType = ((GetCards) getCards).getCardType();
                switch (cardType) {
                  case CardTypeType.SM_B:
                  case CardTypeType.SMC_B:
                    return getCardsSmbResponse();
                  case CardTypeType.EGK:
                    return getCardsEgkResponse(KVNR);
                  case CardTypeType.HBA:
                    return getCardsHbaResponse();
                }
              }
              log.debug("No GetCards object with known CardTypeType");
              throw new IllegalArgumentException("No GetCards object with known CardTypeType");
            });
    Mockito.when(cardService.getPinStatus(Mockito.any()))
        .thenReturn(getPinStatusResponse(PinStatusEnum.VERIFIED));
    Mockito.when(authSignatureService.externalAuthenticate(Mockito.any()))
        .thenReturn(getSignNonceResponse());
    Mockito.when(authSignatureService.externalAuthenticate(Mockito.any()))
        .thenReturn(getSignChallengeResponse());
    Mockito.when(certificateService.readCardCertificate(Mockito.any()))
        .thenReturn(getReadCardCertificateResponse());
    //    Mockito.when(vsdService.readVSD(Mockito.any())).thenReturn(getReadVSDResponsePZ1());

    return konnektorInterfacesCxf;
  }

  public void configureVsdServiceResponse(ReadVSDResponse response) {
    Mockito.when(vsdService.readVSD(Mockito.any())).thenReturn(response);
  }
}
