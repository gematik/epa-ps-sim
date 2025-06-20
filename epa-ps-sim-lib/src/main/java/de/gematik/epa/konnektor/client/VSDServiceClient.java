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

import static de.gematik.epa.utils.StringUtils.toISO885915;

import de.gematik.epa.api.testdriver.dto.response.ReadVSDResponseDTO;
import de.gematik.epa.api.testdriver.entitlement.dto.PostEntitlementRequestDTO;
import de.gematik.epa.konnektor.KonnektorContextProvider;
import de.gematik.epa.konnektor.KonnektorInterfaceAssembly;
import de.gematik.epa.konnektor.conversion.VSDServiceUtils;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Objects;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import telematik.ws.conn.cardservice.xsd.v8_1.CardInfoType;
import telematik.ws.conn.cardservicecommon.xsd.v2_0.CardTypeType;
import telematik.ws.conn.connectorcontext.xsd.v2_0.ContextType;
import telematik.ws.conn.vsds.vsdservice.wsdl.v5_2.VSDServicePortType;
import telematik.ws.conn.vsds.vsdservice.xsd.v5_2.ReadVSD;
import telematik.ws.conn.vsds.vsdservice.xsd.v5_2.ReadVSDResponse;
import telematik.ws.fa.vsds.pruefungsnachweis.xsd.v1_0.PN;
import telematik.ws.fa.vsds.schema_vsd.xsd.v5_2_0.UCAllgemeineVersicherungsdatenXML;
import telematik.ws.fa.vsds.schema_vsd.xsd.v5_2_0.UCPersoenlicheVersichertendatenXML;

@Accessors(fluent = true)
@Slf4j
@Getter
@Service
public class VSDServiceClient extends KonnektorServiceClient {

  private VSDServicePortType vsdService;
  private ContextType context;
  private EventServiceClient eventService;

  public VSDServiceClient(
      KonnektorContextProvider konnektorContextProvider,
      KonnektorInterfaceAssembly konnektorInterfaceAssembly) {
    super(konnektorContextProvider, konnektorInterfaceAssembly);
    runInitializationSynchronized();
  }

  @Override
  protected void initialize() {
    context = konnektorContextProvider.getContext();
    vsdService = konnektorInterfaceAssembly.vsdService();
    eventService = new EventServiceClient(konnektorContextProvider, konnektorInterfaceAssembly);
  }

  public ReadVSDResponseDTO readVSDAndConvertToDto(@NonNull String kvnr) {
    return transformResponse(readVSD(kvnr));
  }

  public byte[] getPruefziffer(@NonNull String kvnr) throws IOException {
    ReadVSDResponse response = readVSD(kvnr);
    PN pn = VSDServiceUtils.getPn(response);
    int result = VSDServiceUtils.getResultOfOnlineCheckEGK(pn);
    boolean isSuccess = VSDServiceUtils.isResultSuccessful(result);
    if (!isSuccess) {
      throw new IOException("ReadVSD operation failed. Result: " + result);
    }
    return VSDServiceUtils.getPruefziffer(pn);
  }

  public String createHcv(String kvnr, String testCase) {
    if (testCase != null) {
      switch (PostEntitlementRequestDTO.TestCaseEnum.fromValue(testCase)) {
        case VALID_HCV -> {
          return VSDServiceUtils.calculateHcv(
              getVersicherungsbeginn(kvnr), getStrassenAdresse(kvnr));
        }
        case INVALID_HCV_STRUCTURE -> {
          return "7cc49e7af4"; // hexdump
        }
        case INVALID_HCV_HASH -> {
          return VSDServiceUtils.calculateHcv(
              toISO885915("18500131"), toISO885915("Falsche Strasse"));
        }
        default -> {
          return null;
        }
      }
    } else {
      throw new NullPointerException("TestCase for HCV return was null.");
    }
  }

  byte[] getVersicherungsbeginn(@NonNull String kvnr) {
    ReadVSDResponse response = readVSD(kvnr);
    UCAllgemeineVersicherungsdatenXML allgemeineVersicherungsdatenXML =
        VSDServiceUtils.getAllgemeineVersicherungsdatenXml(response);
    return toISO885915(
        allgemeineVersicherungsdatenXML.getVersicherter().getVersicherungsschutz().getBeginn());
  }

  byte[] getStrassenAdresse(@NonNull String kvnr) {
    ReadVSDResponse response = readVSD(kvnr);
    UCPersoenlicheVersichertendatenXML persoenlicheVersichertendatenXML =
        VSDServiceUtils.getPersoenlicheVersichertendatenXml(response);

    String strassenAdresse =
        persoenlicheVersichertendatenXML
            .getVersicherter()
            .getPerson()
            .getStrassenAdresse()
            .getStrasse();
    return toISO885915(strassenAdresse == null ? "" : strassenAdresse);
  }

  public ReadVSDResponse readVSD(String kvnr) {
    return vsdService.readVSD(transformRequest(kvnr));
  }

  protected ReadVSD transformRequest(String kvnr) {
    return new ReadVSD()
        .withContext(context)
        .withEhcHandle(getEhcHandle(kvnr))
        .withHpcHandle(getHcpHandle())
        .withReadOnlineReceipt(true)
        .withPerformOnlineCheck(true);
  }

  @SneakyThrows
  protected ReadVSDResponseDTO transformResponse(ReadVSDResponse response) {
    PN pn = VSDServiceUtils.getPn(response);
    int result = VSDServiceUtils.getResultOfOnlineCheckEGK(pn);
    boolean isSuccess = VSDServiceUtils.isResultSuccessful(result);
    String message = isSuccess ? "ReadVSD operation was successful" : "ReadVSD operation failed";
    return new ReadVSDResponseDTO(isSuccess, message, result);
  }

  // region private
  private String getHcpHandle() {
    return eventService.getCardHandle(CardTypeType.SMC_B);
  }

  private String getEhcHandle(String kvnr) {
    CardInfoType cardInfoType = retrieveCardInfo(kvnr);
    return cardInfoType.getCardHandle();
  }

  private CardInfoType retrieveCardInfo(String kvnr) throws NoSuchElementException {
    try (EventServiceClient eventServiceClient =
        new EventServiceClient(konnektorContextProvider, konnektorInterfaceAssembly)) {
      return Objects.requireNonNull(
          eventServiceClient.getEgkInfoToKvnr(kvnr),
          "No egkInfo could be retrieved for KVNR " + Objects.toString(kvnr, "null"));
    } catch (Exception e) {
      throw new NoSuchElementException(e.getMessage());
    }
  }

  // endregion private

}
