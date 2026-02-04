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
package de.gematik.epa.entitlement;

import de.gematik.epa.api.entitlement.client.EntitlementsApi;
import de.gematik.epa.api.entitlement.client.UserBlockingApi;
import de.gematik.epa.api.entitlement.client.dto.*;
import de.gematik.epa.api.testdriver.entitlement.dto.*;
import de.gematik.epa.client.JaxRsClientWrapper;
import de.gematik.epa.data.SmbInformation;
import de.gematik.epa.konnektor.CardAuthenticationService;
import de.gematik.epa.konnektor.KonnektorContextProvider;
import de.gematik.epa.konnektor.KonnektorInterfaceAssembly;
import de.gematik.epa.konnektor.SmbInformationProvider;
import de.gematik.epa.konnektor.client.AuthSignatureServiceClient;
import de.gematik.epa.konnektor.client.VSDServiceClient;
import de.gematik.idp.field.ClaimName;
import jakarta.ws.rs.core.Response;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.function.UnaryOperator;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwt.JwtClaims;
import telematik.ws.conn.cardservice.xsd.v8_1.CardInfoType;

@Slf4j
public class EntitlementService {

  static final String ERROR_WHILE_SETTING_ENTITLEMENT = "Error while setting entitlement: ";
  private static final String UNKNOWN_ERROR = "Unknown error";
  private static final String AUDIT_EVIDENCE = "auditEvidence";
  private static final String HCV = "hcv";
  @Setter private JaxRsClientWrapper<EntitlementsApi> entitlementClientWrapper;
  private final JaxRsClientWrapper<UserBlockingApi> blockingClientWrapper;
  @Getter private final SmbInformationProvider smbInformationProvider;
  @Setter private CardAuthenticationService cardAuthenticationService;
  @Getter private final VSDServiceClient vsdServiceClient;

  public EntitlementService(
      final JaxRsClientWrapper<EntitlementsApi> entitlementClientWrapper,
      final JaxRsClientWrapper<UserBlockingApi> blockingClientWrapper,
      final KonnektorContextProvider contextProvider,
      final KonnektorInterfaceAssembly konnektorInterfaceAssembly,
      final SmbInformationProvider smbInformationProvider,
      final VSDServiceClient vsdServiceClient) {
    this.entitlementClientWrapper = entitlementClientWrapper;
    this.blockingClientWrapper = blockingClientWrapper;
    this.smbInformationProvider = smbInformationProvider;
    this.cardAuthenticationService =
        new CardAuthenticationService(
            smbInformationProvider,
            new AuthSignatureServiceClient(contextProvider, konnektorInterfaceAssembly));
    this.vsdServiceClient = vsdServiceClient;
  }

  public PostEntitlementResponseDTO setEntitlement(
      String xInsurantid, final PostEntitlementRequestDTO postEntitlementRequest) {

    try {
      final String telematikId =
          (postEntitlementRequest.getTelematikId() != null)
              ? postEntitlementRequest.getTelematikId()
              : getSmbInformationProvider().getCardsInformations().stream()
                  .findFirst()
                  .map(SmbInformation::telematikId)
                  .orElseThrow(() -> new IllegalStateException("No SMC-B found!"));

      // card handle
      final String cardHandle = cardAuthenticationService.getCardHandle(telematikId);
      final X509Certificate certificate =
          cardAuthenticationService.getX509Certificate(
              new CardInfoType().withCardHandle(cardHandle));

      // read VSD
      final byte[] pruefziffer =
          getVsdServiceClient().getPruefziffer(postEntitlementRequest.getKvnr());

      String testCase =
          postEntitlementRequest.getTestCase() != null
              ? postEntitlementRequest.getTestCase()
              : PostEntitlementRequestDTO.TestCaseEnum.VALID_HCV.value();
      String hcv = getVsdServiceClient().createHcv(postEntitlementRequest.getKvnr(), testCase);

      // external authenticate
      final UnaryOperator<byte[]> contentSigner =
          cardAuthenticationService.getContentSigner(cardHandle);

      // create signed JWT
      final JwtClaims claims = new JwtClaims();
      final ZonedDateTime now = ZonedDateTime.now();
      claims.setClaim(ClaimName.ISSUED_AT.getJoseName(), now.toEpochSecond());
      claims.setClaim(ClaimName.EXPIRES_AT.getJoseName(), now.plusMinutes(20).toEpochSecond());
      claims.setClaim(AUDIT_EVIDENCE, Base64.getEncoder().encodeToString(pruefziffer));
      if (hcv != null) claims.setClaim(HCV, hcv);

      final String signedJwt =
          cardAuthenticationService.createSignedJwt(
              claims,
              certificate,
              contentSigner,
              cardAuthenticationService.determineAlgorithm(certificate.getPublicKey()));
      final Response response =
          entitlementClientWrapper
              .getServiceApi()
              .setEntitlementPs(
                  xInsurantid,
                  this.entitlementClientWrapper.getUserAgent(),
                  new EntitlementRequestType().jwt(signedJwt));

      return createResponseDTO(response);
    } catch (final Exception e) {
      log.error(ERROR_WHILE_SETTING_ENTITLEMENT, e);
      return new PostEntitlementResponseDTO()
          .success(false)
          .statusMessage(ERROR_WHILE_SETTING_ENTITLEMENT + e.getMessage());
    }
  }

  public GetBlockedUserListResponseDTO getBlockedUserList(final String xInsurantid) {
    final var responseDTO =
        new GetBlockedUserListResponseDTO()
            .success(true)
            .statusMessage("Ok. Returns a list of policy assignments");
    try (final Response response =
        blockingClientWrapper
            .getServiceApi()
            .getBlockedUserPolicyAssignments(xInsurantid, blockingClientWrapper.getUserAgent())) {
      switch (response.getStatus()) {
        case 200 -> {
          final GetBlockedUserPolicyAssignments200Response wrappedResponse =
              response.readEntity(GetBlockedUserPolicyAssignments200Response.class);
          final var assignmentResponseList = wrappedResponse.getData();
          final var assignmentDTOList =
              assignmentResponseList.stream()
                  .map(
                      assignmentResponse ->
                          new GetBlockedUserListResponseDTOAllOfAssignments()
                              .telematikId(assignmentResponse.getActorId())
                              .oid(assignmentResponse.getOid())
                              .displayName(assignmentResponse.getDisplayName())
                              .at(assignmentResponse.getAt()))
                  .toList();
          responseDTO.assignments(assignmentDTOList);
        }
        case 400, 403, 404, 409, 500 -> {
          final var errorType = response.hasEntity() ? response.readEntity(ErrorType.class) : null;
          final String errorMessage = errorType != null ? errorType.getErrorCode() : UNKNOWN_ERROR;
          log.error("Error occurred: {}", errorMessage);
          return responseDTO.success(false).statusMessage(errorMessage);
        }
        default -> responseDTO.success(false).statusMessage(UNKNOWN_ERROR);
      }
    } catch (final Exception e) {
      log.error("Error occurred while fetching email address: {}", e.getMessage());
      responseDTO.success(false).statusMessage(UNKNOWN_ERROR);
    }
    return responseDTO;
  }

  public ResponseDTO setBlockedUser(
      final SetBlockedUserRequestDTO setBlockedUserRequestDTO, final String xInsurantId) {
    ResponseDTO responseDTO;
    final var assignmentType =
        new BlockedUserPolicyAssignmentType()
            .actorId(setBlockedUserRequestDTO.getActorId())
            .oid(setBlockedUserRequestDTO.getOid())
            .displayName(setBlockedUserRequestDTO.getDisplayName());
    try (final Response response =
        blockingClientWrapper
            .getServiceApi()
            .setBlockedUserPolicyAssignment(
                xInsurantId, blockingClientWrapper.getUserAgent(), assignmentType)) {
      responseDTO = createResponseDTO(response, "Created");

    } catch (final Exception e) {
      log.error("Error occurred while setting blocked user: {}", e.getMessage());
      responseDTO = new ResponseDTO().success(false).statusMessage(UNKNOWN_ERROR);
    }
    return responseDTO;
  }

  public ResponseDTO deleteBlockedUser(final String xInsurantId, final String telematikId) {
    ResponseDTO responseDTO;
    try (final Response response =
        blockingClientWrapper
            .getServiceApi()
            .deleteBlockedUserPolicyAssignment(
                xInsurantId, telematikId, blockingClientWrapper.getUserAgent())) {
      responseDTO = createResponseDTO(response, "OK. Assignment deleted");
    } catch (final Exception e) {
      log.error("Error occurred while deleting blocked user: {}", e.getMessage());
      responseDTO = new ResponseDTO().success(false).statusMessage(UNKNOWN_ERROR);
    }
    return responseDTO;
  }

  private PostEntitlementResponseDTO createResponseDTO(final Response response) {
    switch (response.getStatus()) {
      case 201 -> {
        return new PostEntitlementResponseDTO()
            .success(true)
            .validTo(response.readEntity(ValidToResponseType.class).getValidTo());
      }
      case 400, 403, 404, 409, 423, 500 -> {
        final var errorType =
            response.getEntity() != null ? response.readEntity(ErrorType.class) : null;
        String errorCode = errorType != null ? errorType.getErrorCode() : UNKNOWN_ERROR;
        String errorDetail = errorType != null ? errorType.getErrorDetail() : UNKNOWN_ERROR;
        return new PostEntitlementResponseDTO()
            .success(false)
            .statusMessage(
                "Status Code: "
                    + response.getStatus()
                    + ", Error: "
                    + errorCode
                    + ", Detail: "
                    + errorDetail);
      }
      default -> {
        return new PostEntitlementResponseDTO().success(false).statusMessage(UNKNOWN_ERROR);
      }
    }
  }

  public ResponseDTO createResponseDTO(final Response response, final String successMessage) {
    return switch (response.getStatus()) {
      case 201, 204 -> new ResponseDTO().success(true).statusMessage(successMessage);
      case 400, 403, 404, 409, 500 -> {
        final var errorType =
            response.getEntity() == null ? null : response.readEntity(ErrorType.class);
        yield new ResponseDTO()
            .success(false)
            .statusMessage(errorType == null ? UNKNOWN_ERROR : errorType.getErrorCode());
      }
      default ->
          new ResponseDTO()
              .success(false)
              .statusMessage("Failed with status code: " + response.getStatus());
    };
  }
}
