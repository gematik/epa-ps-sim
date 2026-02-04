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

import de.gematik.epa.api.testdriver.card.CardApi;
import de.gematik.epa.api.testdriver.card.dto.EgkInformationDTO;
import de.gematik.epa.api.testdriver.card.dto.GetCardsInfoResponseDTO;
import de.gematik.epa.api.testdriver.card.dto.HbaInformationDTO;
import de.gematik.epa.api.testdriver.card.dto.SmbInformationDTO;
import de.gematik.epa.konnektor.HbaInformationProvider;
import de.gematik.epa.konnektor.SmbInformationProvider;
import de.gematik.epa.konnektor.client.EventServiceClient;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import telematik.ws.conn.cardservice.xsd.v8_1.Cards;
import telematik.ws.conn.eventservice.xsd.v6_1.GetCardsResponse;

@RequiredArgsConstructor
public class CardApiImpl implements CardApi {

  private final SmbInformationProvider smbInformationProvider;

  private final EventServiceClient eventServiceClient;

  private final HbaInformationProvider hbaInformationProvider;

  @Override
  public GetCardsInfoResponseDTO getCardsInfo() {
    try {
      return new GetCardsInfoResponseDTO()
          .success(true)
          .smbInfo(
              Optional.ofNullable(smbInformationProvider.getCardsInformations())
                  .orElse(List.of())
                  .stream()
                  .map(
                      info ->
                          new SmbInformationDTO()
                              .telematikId(info.telematikId())
                              .iccsn(info.iccsn())
                              .cardHolderName(info.cardHolderName())
                              .cardHandle(info.cardHandle()))
                  .toList())
          .egkInfo(
              Optional.ofNullable(eventServiceClient.getEgkInfo())
                  .map(GetCardsResponse::getCards)
                  .map(Cards::getCard)
                  .orElse(List.of())
                  .stream()
                  .map(
                      info ->
                          new EgkInformationDTO()
                              .kvnr(info.getKvnr())
                              .iccsn(info.getIccsn())
                              .cardHolderName(info.getCardHolderName())
                              .cardHandle(info.getCardHandle()))
                  .toList())
          .hbaInfo(
              Optional.ofNullable(hbaInformationProvider.getCardsInformations())
                  .orElse(List.of())
                  .stream()
                  .map(
                      info ->
                          new HbaInformationDTO()
                              .telematikId(info.telematikId())
                              .iccsn(info.iccsn())
                              .cardHolderName(info.cardHolderName())
                              .cardHandle(info.cardHandle()))
                  .toList());
    } catch (final RuntimeException e) {
      return new GetCardsInfoResponseDTO().success(false).statusMessage(e.getMessage());
    }
  }
}
