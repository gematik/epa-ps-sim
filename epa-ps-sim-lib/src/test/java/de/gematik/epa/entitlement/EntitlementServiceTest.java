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

import static de.gematik.epa.entitlement.EntitlementService.ERROR_WHILE_SETTING_ENTITLEMENT;
import static de.gematik.epa.unit.util.TestDataFactory.simulateInbound;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jose4j.jws.AlgorithmIdentifiers.RSA_PSS_USING_SHA256;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.epa.api.entitlement.client.EntitlementsApi;
import de.gematik.epa.api.entitlement.client.UserBlockingApi;
import de.gematik.epa.api.entitlement.client.dto.BlockedUserPolicyAssignmentResponseType;
import de.gematik.epa.api.entitlement.client.dto.BlockedUserPolicyAssignmentType;
import de.gematik.epa.api.entitlement.client.dto.EntitlementRequestType;
import de.gematik.epa.api.entitlement.client.dto.ErrorType;
import de.gematik.epa.api.entitlement.client.dto.GetBlockedUserPolicyAssignments200Response;
import de.gematik.epa.api.entitlement.client.dto.ValidToResponseType;
import de.gematik.epa.api.testdriver.entitlement.dto.GetBlockedUserListResponseDTO;
import de.gematik.epa.api.testdriver.entitlement.dto.GetBlockedUserListResponseDTOAllOfAssignments;
import de.gematik.epa.api.testdriver.entitlement.dto.PostEntitlementRequestDTO;
import de.gematik.epa.api.testdriver.entitlement.dto.PostEntitlementResponseDTO;
import de.gematik.epa.api.testdriver.entitlement.dto.ResponseDTO;
import de.gematik.epa.api.testdriver.entitlement.dto.SetBlockedUserRequestDTO;
import de.gematik.epa.authentication.exception.TelematikIdNotFoundException;
import de.gematik.epa.client.JaxRsClientWrapper;
import de.gematik.epa.konnektor.CardAuthenticationService;
import de.gematik.epa.konnektor.KonnektorContextProvider;
import de.gematik.epa.konnektor.KonnektorInterfaceAssembly;
import de.gematik.epa.konnektor.SmbInformationProvider;
import de.gematik.epa.konnektor.client.VSDServiceClient;
import de.gematik.epa.unit.util.TestDataFactory;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@Slf4j
class EntitlementServiceTest {

  private final CardAuthenticationService cardAuthenticationService =
      mock(CardAuthenticationService.class);
  private final VSDServiceClient vsdServiceClient = mock(VSDServiceClient.class);
  private final JaxRsClientWrapper<EntitlementsApi> entitlementClientWrapper =
      mock(JaxRsClientWrapper.class);
  private final JaxRsClientWrapper<UserBlockingApi> blockingClientWrapper =
      mock(JaxRsClientWrapper.class);
  private final EntitlementsApi EntitlementsApi = mock(EntitlementsApi.class);
  private final UserBlockingApi UserBlockingApi = mock(UserBlockingApi.class);
  private final SmbInformationProvider smbInformationProvider = mock(SmbInformationProvider.class);
  private final String pruefungsNachweis =
      "H4sIAAAAAAAAAB2NQU+DMABG/wrp1YzSAhFN22URlB1oZdRNvRgCdTaWslgCyK+XefkOX/LeI9u5M96ofpzuLQXID4CnbNO32p4p2FdikyTx3QYBzw21bWvTW0XBr3Jgy8gz9x7S4uOYHaq94P/0lV+N1lHwNQyXewgn559VVw/6228V/Kzh6NoOXuwEx2uNEVkxHOAoiDFGYRihWwLXi2QME5itkXd2SrO5SMuFy91SyCws5Nsi0jLisj2e0rwJkIkfd0+hmA9xLkb8optI9zdmyl9VYkpK4CpZh7M/MsH+z+8AAAA=";
  private final String userAgent = "ps-sim";
  private final String xInsurantId = "X12345678";
  private final String telematikId = "1-2344556";
  private EntitlementService entitlementService;

  static Stream<Object[]> provideErrorCases() {
    return Stream.of(
        new Object[] {400, "malformedRequest"},
        new Object[] {403, "notEntitled"},
        new Object[] {403, "invalidOid"},
        new Object[] {404, "noHealthRecord"},
        new Object[] {409, "statusMismatch"},
        new Object[] {500, "internalError"});
  }

  static Stream<Object[]> setEntitlementErrorCases() {
    return Stream.of(
        new Object[] {400, "malformedRequest"},
        new Object[] {403, "invalidOid"},
        new Object[] {403, "invalidToken"},
        new Object[] {404, "noHealthRecord"},
        new Object[] {409, "statusMismatch"},
        new Object[] {409, "hcvMissing"},
        new Object[] {423, "locked"},
        new Object[] {500, "internalError"});
  }

  @BeforeEach
  void setUp() {
    final KonnektorContextProvider konnektorContextProvider =
        TestDataFactory.konnektorContextProvider();
    final KonnektorInterfaceAssembly konnektorInterface =
        TestDataFactory.konnektorInterfaceAssemblyMock();
    entitlementService =
        spy(
            new EntitlementService(
                entitlementClientWrapper,
                blockingClientWrapper,
                konnektorContextProvider,
                konnektorInterface,
                smbInformationProvider,
                vsdServiceClient));
    entitlementService.setCardAuthenticationService(cardAuthenticationService);
    when(entitlementClientWrapper.getServiceApi()).thenReturn(EntitlementsApi);
    when(blockingClientWrapper.getServiceApi()).thenReturn(UserBlockingApi);
  }

  @Test
  void shouldSetEntitlement() throws IOException {
    // given
    final PostEntitlementRequestDTO requestDTO = getPostEntitlementRequestDTO();
    //    final var cardHandle = "123";
    final var signedJwt = "testSignedJwt";
    final Response response =
        simulateInbound(
            Response.status(201)
                .entity(new ValidToResponseType().validTo(OffsetDateTime.now()))
                .build());
    final EntitlementRequestType request = new EntitlementRequestType().jwt(signedJwt);
    when(EntitlementsApi.setEntitlementPs(xInsurantId, userAgent, request)).thenReturn(response);

    // when
    final PostEntitlementResponseDTO result =
        entitlementService.setEntitlement(xInsurantId, requestDTO);

    // then
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getStatusMessage()).isNull();
    assertThat(result.getValidTo()).isNotNull();

    verify(vsdServiceClient).createHcv(requestDTO.getKvnr(), requestDTO.getTestCase());
  }

  @Test
  void shouldSetEntitlementGetTelematikIdFromSmb() throws IOException {
    // given
    PostEntitlementRequestDTO requestDTO = getPostEntitlementRequestDTO();
    final var signedJwt = "testSignedJwt";
    when(smbInformationProvider.getCardsInformations())
        .thenReturn(List.of(TestDataFactory.createSmbInformation()));

    final Response response =
        simulateInbound(
            Response.status(201)
                .entity(new ValidToResponseType().validTo(OffsetDateTime.now()))
                .build());
    final EntitlementRequestType request = new EntitlementRequestType().jwt(signedJwt);
    when(EntitlementsApi.setEntitlementPs(xInsurantId, userAgent, request)).thenReturn(response);

    // when
    final PostEntitlementResponseDTO result =
        entitlementService.setEntitlement(xInsurantId, requestDTO);

    // then
    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getStatusMessage()).isNull();
    assertThat(result.getValidTo()).isNotNull();
  }

  @Test
  void shouldFailWhenTelematikIdDoesNotExist() {
    // given
    final var requestDTO = new PostEntitlementRequestDTO();
    requestDTO.setKvnr("testKvnr");
    requestDTO.setTelematikId("testTelematikId");

    when(cardAuthenticationService.getCardHandle(requestDTO.getTelematikId()))
        .thenThrow(new TelematikIdNotFoundException("TelematikId not found"));

    // when
    final PostEntitlementResponseDTO response =
        entitlementService.setEntitlement(xInsurantId, requestDTO);

    // then
    assertThat(response.getSuccess()).isFalse();
  }

  @Test
  void shouldFailWhenTelematikIdDoesNotFound() {
    // given
    final var requestDTO = new PostEntitlementRequestDTO();
    requestDTO.setKvnr("testKvnr");

    when(smbInformationProvider.getCardsInformations()).thenReturn(List.of());

    // when
    final PostEntitlementResponseDTO response =
        entitlementService.setEntitlement(xInsurantId, requestDTO);

    // then
    assertThat(response.getSuccess()).isFalse();
  }

  @ParameterizedTest
  @MethodSource("setEntitlementErrorCases")
  void shouldReturnUnSuccessfulResponseWhenServerRespondsLocked(
      final int statusCode, final String errorCode) throws IOException {
    final PostEntitlementRequestDTO requestDTO = getPostEntitlementRequestDTO();

    // mock response
    final Response response =
        simulateInbound(
            Response.status(statusCode).entity(new ErrorType().errorCode(errorCode)).build());
    when(entitlementClientWrapper
            .getServiceApi()
            .setEntitlementPs(anyString(), anyString(), any(EntitlementRequestType.class)))
        .thenReturn(response);
    when(entitlementClientWrapper.getUserAgent()).thenReturn("ps-sim");

    // verify
    final PostEntitlementResponseDTO actualResponse =
        entitlementService.setEntitlement(xInsurantId, requestDTO);
    AssertionsForClassTypes.assertThat(actualResponse).isNotNull();
    AssertionsForClassTypes.assertThat(actualResponse.getSuccess()).isFalse();
    AssertionsForClassTypes.assertThat(actualResponse.getStatusMessage()).contains(errorCode);
  }

  private PostEntitlementRequestDTO getPostEntitlementRequestDTO() throws IOException {
    final var cardHandle = "123";
    final var signedJwt = "testSignedJwt";
    final var hcv = "testHcv";
    final PostEntitlementRequestDTO requestDTO = new PostEntitlementRequestDTO();
    requestDTO.setKvnr("testKvnr");
    requestDTO.setTelematikId("testTelematikId");
    requestDTO.setTestCase(PostEntitlementRequestDTO.TestCaseEnum.VALID_HCV);
    when(cardAuthenticationService.getCardHandle(requestDTO.getTelematikId()))
        .thenReturn("testCardHandle");

    when(vsdServiceClient.getPruefziffer(requestDTO.getKvnr()))
        .thenReturn(pruefungsNachweis.getBytes());
    when(vsdServiceClient.createHcv(requestDTO.getKvnr(), requestDTO.getTestCase()))
        .thenReturn(hcv);
    when(entitlementClientWrapper.getUserAgent()).thenReturn(userAgent);

    when(cardAuthenticationService.getCardHandle(requestDTO.getTelematikId()))
        .thenReturn(cardHandle);

    final UnaryOperator<byte[]> mockedContentSigner = TestDataFactory.getContentSigner(cardHandle);
    when(cardAuthenticationService.getContentSigner(cardHandle)).thenReturn(mockedContentSigner);

    final X509Certificate mockedCertificate = mock(X509Certificate.class);
    when(cardAuthenticationService.getX509Certificate(any())).thenReturn(mockedCertificate);
    when(cardAuthenticationService.determineAlgorithm(mockedCertificate.getPublicKey()))
        .thenReturn(RSA_PSS_USING_SHA256);

    when(cardAuthenticationService.createSignedJwt(any(), any(), any(), anyString()))
        .thenReturn(signedJwt);
    return requestDTO;
  }

  @Test
  @SneakyThrows
  void shouldReturnUnSuccessfulResponseWhenReadVsdFails() {
    // given
    final PostEntitlementRequestDTO requestDTO = new PostEntitlementRequestDTO();
    requestDTO.setKvnr("testKvnr");
    requestDTO.setTelematikId("testTelematikId");
    when(cardAuthenticationService.getCardHandle(requestDTO.getTelematikId()))
        .thenReturn("testCardHandle");

    when(vsdServiceClient.getPruefziffer(requestDTO.getKvnr()))
        .thenThrow(new RuntimeException("Error while reading VSD"));

    // when
    final PostEntitlementResponseDTO result =
        entitlementService.setEntitlement(xInsurantId, requestDTO);

    // then
    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).startsWith(ERROR_WHILE_SETTING_ENTITLEMENT);
  }

  @Test
  @SneakyThrows
  void shouldReturnUnSuccessfulResponseWhenReadVsdEgkCheckFails() {
    // given
    final PostEntitlementRequestDTO requestDTO = new PostEntitlementRequestDTO();
    requestDTO.setKvnr("testKvnr");
    requestDTO.setTelematikId("testTelematikId");
    when(cardAuthenticationService.getCardHandle(requestDTO.getTelematikId()))
        .thenReturn("testCardHandle");

    when(vsdServiceClient.getPruefziffer(requestDTO.getKvnr()))
        .thenThrow(new IOException("ReadVSD operation failed. Result: 3"));

    // when
    final PostEntitlementResponseDTO result =
        entitlementService.setEntitlement(xInsurantId, requestDTO);

    // then
    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).startsWith(ERROR_WHILE_SETTING_ENTITLEMENT);
  }

  @Test
  @SneakyThrows
  void shouldReturnUnSuccessfulResponseWhenContentSignerFails() {
    // given
    final var cardHandle = "123";

    final PostEntitlementRequestDTO requestDTO = new PostEntitlementRequestDTO();
    requestDTO.setKvnr("testKvnr");
    requestDTO.setTelematikId("testTelematikId");
    when(cardAuthenticationService.getCardHandle(requestDTO.getTelematikId()))
        .thenReturn("testCardHandle");

    when(vsdServiceClient.getPruefziffer(requestDTO.getKvnr()))
        .thenReturn(pruefungsNachweis.getBytes());
    when(entitlementClientWrapper.getUserAgent()).thenReturn(userAgent);

    when(cardAuthenticationService.getCardHandle(requestDTO.getTelematikId()))
        .thenReturn(cardHandle);
    when(cardAuthenticationService.getContentSigner(cardHandle))
        .thenThrow(new RuntimeException("Error during CardHandle"));

    // when
    final PostEntitlementResponseDTO result =
        entitlementService.setEntitlement(xInsurantId, requestDTO);

    // then
    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).startsWith(ERROR_WHILE_SETTING_ENTITLEMENT);
  }

  @Test
  @SneakyThrows
  void shouldReturnUnSuccessfulResponseWhenJwtCreationFails() {
    // given
    final var cardHandle = "123";

    final PostEntitlementRequestDTO requestDTO = new PostEntitlementRequestDTO();
    requestDTO.setKvnr("testKvnr");
    requestDTO.setTelematikId("testTelematikId");
    when(cardAuthenticationService.getCardHandle(requestDTO.getTelematikId()))
        .thenReturn("testCardHandle");

    when(vsdServiceClient.getPruefziffer(requestDTO.getKvnr()))
        .thenReturn(pruefungsNachweis.getBytes());
    when(entitlementClientWrapper.getUserAgent()).thenReturn(userAgent);

    when(cardAuthenticationService.getCardHandle(requestDTO.getTelematikId()))
        .thenReturn(cardHandle);
    final UnaryOperator<byte[]> mockedContentSigner = TestDataFactory.getContentSigner(cardHandle);
    when(cardAuthenticationService.getContentSigner(cardHandle)).thenReturn(mockedContentSigner);

    final X509Certificate mockedCertificate = mock(X509Certificate.class);
    when(cardAuthenticationService.getX509Certificate(any())).thenReturn(mockedCertificate);
    when(cardAuthenticationService.determineAlgorithm(mockedCertificate.getPublicKey()))
        .thenReturn(RSA_PSS_USING_SHA256);

    when(cardAuthenticationService.createSignedJwt(any(), any(), any(), anyString()))
        .thenThrow(new RuntimeException("Error during JWT creation"));

    // when
    final PostEntitlementResponseDTO result =
        entitlementService.setEntitlement(xInsurantId, requestDTO);

    // then
    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getStatusMessage()).startsWith(ERROR_WHILE_SETTING_ENTITLEMENT);
  }

  @Test
  void shouldReturnBlockedUsersWhenResponseStatusIs200() {
    // setup expected response
    final GetBlockedUserListResponseDTO expectedResponse = new GetBlockedUserListResponseDTO();
    final List<GetBlockedUserListResponseDTOAllOfAssignments> assignments = new ArrayList<>();
    final GetBlockedUserListResponseDTOAllOfAssignments assignment =
        new GetBlockedUserListResponseDTOAllOfAssignments();
    assignment.setTelematikId("telematikId");
    assignment.setAt(OffsetDateTime.parse("2023-09-05T10:15:30+01:00"));
    assignment.setOid("1.2.3");
    assignment.setDisplayName("DisplayName");
    assignments.add(assignment);

    expectedResponse.setAssignments(assignments);
    expectedResponse.setSuccess(true);
    expectedResponse.setStatusMessage("Ok. Returns a list of policy assignments");

    // mock response
    final GetBlockedUserPolicyAssignments200Response wrappedResponse =
        new GetBlockedUserPolicyAssignments200Response();
    final List<BlockedUserPolicyAssignmentResponseType> assignmentList = new ArrayList<>();
    final BlockedUserPolicyAssignmentResponseType assignmentResponse =
        new BlockedUserPolicyAssignmentResponseType();
    assignmentResponse.setActorId("telematikId");
    assignmentResponse.setAt(OffsetDateTime.parse("2023-09-05T10:15:30+01:00"));
    assignmentResponse.setOid("1.2.3");
    assignmentResponse.setDisplayName("DisplayName");
    assignmentList.add(assignmentResponse);
    wrappedResponse.setData(assignmentList);

    final Response response = mock(Response.class);
    when(response.getStatus()).thenReturn(200);
    when(response.readEntity(GetBlockedUserPolicyAssignments200Response.class))
        .thenReturn(wrappedResponse);

    when(blockingClientWrapper
            .getServiceApi()
            .getBlockedUserPolicyAssignments(anyString(), anyString()))
        .thenReturn(response);
    when(blockingClientWrapper.getUserAgent()).thenReturn("ps-sim");

    // verify
    final GetBlockedUserListResponseDTO actualResponse =
        entitlementService.getBlockedUserList(xInsurantId);
    assertThat(actualResponse).isEqualTo(expectedResponse);
  }

  @ParameterizedTest
  @MethodSource("provideErrorCases")
  void getBlockedUsersShouldReturnError(final int statusCode, final String errorCode) {
    // setup expected response
    final GetBlockedUserListResponseDTO expectedResponse = new GetBlockedUserListResponseDTO();
    expectedResponse.setSuccess(false);
    expectedResponse.setStatusMessage(errorCode);

    // mock response
    final Response response =
        simulateInbound(
            Response.status(statusCode).entity(new ErrorType().errorCode(errorCode)).build());

    when(blockingClientWrapper
            .getServiceApi()
            .getBlockedUserPolicyAssignments(anyString(), anyString()))
        .thenReturn(response);
    when(blockingClientWrapper.getUserAgent()).thenReturn("ps-sim");

    // verify
    final GetBlockedUserListResponseDTO actualResponse =
        entitlementService.getBlockedUserList(xInsurantId);
    AssertionsForClassTypes.assertThat(actualResponse).isNotNull();
    AssertionsForClassTypes.assertThat(actualResponse.getSuccess()).isFalse();
    AssertionsForClassTypes.assertThat(actualResponse.getStatusMessage()).isEqualTo(errorCode);
  }

  @Test
  void getBlockedUsersShouldReturnUnknownErrorForException() {
    when(blockingClientWrapper
            .getServiceApi()
            .getBlockedUserPolicyAssignments(anyString(), anyString()))
        .thenThrow(new RuntimeException("Test exception"));
    final GetBlockedUserListResponseDTO result =
        entitlementService.getBlockedUserList("insurantId");
    AssertionsForClassTypes.assertThat(result).isNotNull();
    AssertionsForClassTypes.assertThat(result.getSuccess()).isFalse();
    AssertionsForClassTypes.assertThat(result.getStatusMessage()).isEqualTo("Unknown error");
  }

  @Test
  void getBlockedUsersShouldReturnUnknownErrorForUnhandledStatus() {
    // setup expected response
    final GetBlockedUserListResponseDTO expectedResponse = new GetBlockedUserListResponseDTO();
    expectedResponse.setSuccess(false);
    expectedResponse.setStatusMessage("Unknown error");

    // mock response
    final Response response = mock(Response.class);
    when(response.getStatus()).thenReturn(418); // Unhandled status code

    when(blockingClientWrapper
            .getServiceApi()
            .getBlockedUserPolicyAssignments(anyString(), anyString()))
        .thenReturn(response);
    when(blockingClientWrapper.getUserAgent()).thenReturn("ps-sim");

    // verify
    final GetBlockedUserListResponseDTO actualResponse =
        entitlementService.getBlockedUserList(xInsurantId);
    AssertionsForClassTypes.assertThat(actualResponse).isNotNull();
    AssertionsForClassTypes.assertThat(actualResponse.getSuccess()).isFalse();
    AssertionsForClassTypes.assertThat(actualResponse.getStatusMessage())
        .isEqualTo("Unknown error");
  }

  @ParameterizedTest
  @ValueSource(ints = {201, 400, 403, 404, 409, 500, 999})
  void setBlockedUser(final int responseCode) {
    // given
    final var requestDTO =
        new SetBlockedUserRequestDTO().displayName("displayName").oid("1.2.3").actorId(telematikId);

    // mock response
    final Response response = mock(Response.class);
    when(response.getStatus()).thenReturn(responseCode);
    when(blockingClientWrapper
            .getServiceApi()
            .setBlockedUserPolicyAssignment(
                anyString(), anyString(), any(BlockedUserPolicyAssignmentType.class)))
        .thenReturn(response);

    when(blockingClientWrapper.getUserAgent()).thenReturn(userAgent);
    final BlockedUserPolicyAssignmentType assignment = new BlockedUserPolicyAssignmentType();
    assignment.setDisplayName(requestDTO.getDisplayName());
    assignment.setActorId("1-2344556");
    assignment.setOid(requestDTO.getOid());

    // when
    final ResponseDTO result = entitlementService.setBlockedUser(requestDTO, xInsurantId);

    // then
    if (responseCode == 201) {
      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getStatusMessage()).isEqualTo("Created");
      //noinspection resource
      verify(blockingClientWrapper.getServiceApi())
          .setBlockedUserPolicyAssignment(xInsurantId, userAgent, assignment);
    } else {
      assertThat(result.getSuccess()).isFalse();
      assertThat(result.getStatusMessage()).isNotEmpty();
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {204, 400, 403, 404, 409, 500, 999})
  void deleteBlockedUser(final int responseCode) {
    // mock response
    final Response response = mock(Response.class);
    when(response.getStatus()).thenReturn(responseCode);
    when(blockingClientWrapper
            .getServiceApi()
            .deleteBlockedUserPolicyAssignment(anyString(), anyString(), anyString()))
        .thenReturn(response);

    when(blockingClientWrapper.getUserAgent()).thenReturn(userAgent);

    // when
    final ResponseDTO result = entitlementService.deleteBlockedUser(xInsurantId, telematikId);

    // then
    if (responseCode == 204) {
      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getStatusMessage()).isEqualTo("OK. Assignment deleted");
      //noinspection resource
      verify(blockingClientWrapper.getServiceApi())
          .deleteBlockedUserPolicyAssignment(
              xInsurantId, telematikId, blockingClientWrapper.getUserAgent());
    } else {
      assertThat(result.getSuccess()).isFalse();
      assertThat(result.getStatusMessage()).isNotEmpty();
    }
  }
}
