/*-
 * #%L
 * epa-ps-sim-app
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
package de.gematik.epa.ps.endpoint;

import static de.gematik.epa.unit.util.TestDataFactory.KVNR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import de.gematik.epa.api.testdriver.card.dto.EgkInformationDTO;
import de.gematik.epa.api.testdriver.card.dto.GetCardsInfoResponseDTO;
import de.gematik.epa.api.testdriver.card.dto.HbaInformationDTO;
import de.gematik.epa.api.testdriver.card.dto.SmbInformationDTO;
import de.gematik.epa.data.HbaInformation;
import de.gematik.epa.data.SmbInformation;
import de.gematik.epa.konnektor.HbaInformationProvider;
import de.gematik.epa.konnektor.SmbInformationProvider;
import de.gematik.epa.ps.fhir.config.TestFhirClientProvider;
import de.gematik.epa.unit.AppTestDataFactory;
import de.gematik.epa.unit.TestDocumentClientConfiguration;
import de.gematik.epa.unit.TestKonnektorClientConfiguration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
      TestKonnektorClientConfiguration.class,
      TestDocumentClientConfiguration.class,
      TestFhirClientProvider.class
    })
@ActiveProfiles("test")
@Slf4j
@AutoConfigureRestTestClient
class CardApiEndpointTest {

  @LocalServerPort private int port;

  @Autowired private RestTestClient restTestClient;

  @MockitoBean private SmbInformationProvider smbInformationProvider;

  @MockitoBean private HbaInformationProvider hbaInformationProvider;

  @Test
  void getCardsInfoShouldReturnDefaultMessage() {
    // Arrange
    final SmbInformation smbInformation = AppTestDataFactory.createSmbInformation();
    when(smbInformationProvider.getCardsInformations()).thenReturn(List.of(smbInformation));
    final HbaInformation hbaInformation = AppTestDataFactory.createHbaInformation();
    when(hbaInformationProvider.getCardsInformations()).thenReturn(List.of(hbaInformation));
    final var egk = AppTestDataFactory.cardInfoEgk(KVNR);

    // Act
    final var response =
        this.restTestClient
            .get()
            .uri("http://localhost:" + port + "/services/epa/testdriver/api/v1/cards")
            .exchange()
            .returnResult(GetCardsInfoResponseDTO.class);

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
                        .cardHandle(egk.getCardHandle())
                        .cardHolderName(egk.getCardHolderName())
                        .iccsn(egk.getIccsn())
                        .kvnr(egk.getKvnr())))
            .hbaInfo(
                List.of(
                    new HbaInformationDTO()
                        .cardHandle(hbaInformation.cardHandle())
                        .cardHolderName(hbaInformation.cardHolderName())
                        .iccsn(hbaInformation.iccsn())
                        .telematikId(hbaInformation.telematikId())));

    assertThat(response.getResponseBody()).isEqualTo(expectedResponse);
  }
}
