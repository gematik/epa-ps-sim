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
package de.gematik.epa.ps.entitlement;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static de.gematik.epa.unit.AppTestDataFactory.getReadVSDResponsePZ2;
import static de.gematik.epa.unit.util.TestDataFactory.KVNR;
import static de.gematik.epa.unit.util.TestDataFactory.SMB_AUT_TELEMATIK_ID;
import static de.gematik.epa.unit.util.TestDataFactory.USER_AGENT;
import static de.gematik.epa.unit.util.TestDataFactory.X_INSURANTID;
import static de.gematik.epa.unit.util.TestDataFactory.X_USERAGENT;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.epa.api.testdriver.entitlement.dto.GetBlockedUserListResponseDTO;
import de.gematik.epa.api.testdriver.entitlement.dto.PostEntitlementRequestDTO;
import de.gematik.epa.api.testdriver.entitlement.dto.ResponseDTO;
import de.gematik.epa.api.testdriver.entitlement.dto.SetBlockedUserRequestDTO;
import de.gematik.epa.ps.endpoint.EntitlementApiEndpoint;
import de.gematik.epa.ps.utils.AbstractIntegrationTest;
import de.gematik.epa.unit.TestKonnektorClientConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EntitlementApiEndpointIntegrationTest extends AbstractIntegrationTest {

  @Autowired EntitlementApiEndpoint entitlementApiEndpoint;
  @Autowired private TestKonnektorClientConfiguration testKonnektorClientConfiguration;

  @Test
  void contextLoads() {
    assertThat(entitlementApiEndpoint).isNotNull();
  }

  @BeforeEach
  void setup() {
    testKonnektorClientConfiguration.configureVsdServiceResponse(getReadVSDResponsePZ2());
  }

  @Test
  void shouldSetEntitlement() {
    mockEntitlementServer.stubFor(
        post(urlEqualTo("/epa/basic/api/v1/ps/entitlements"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(201)
                    .withBody("{" + "  \"validTo\": \"2025-12-31T23:59:59+01:00\"" + "}")));

    var request =
        new PostEntitlementRequestDTO()
            .kvnr(KVNR)
            .telematikId(SMB_AUT_TELEMATIK_ID)
            .testCase(PostEntitlementRequestDTO.TestCaseEnum.VALID_HCV);

    var response = entitlementApiEndpoint.postEntitlement(KVNR, request);

    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getValidTo()).isNotNull();
    assertThat(response.getStatusMessage()).isBlank();
  }

  @Test
  void shouldReturnNotSuccessWhenApiReturns403() {
    mockEntitlementServer.stubFor(
        post(urlEqualTo("/epa/basic/api/v1/ps/entitlements"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(403)
                    .withBody("{" + "  \"errorCode\": \"notEntitled\"" + "}")));

    var request = new PostEntitlementRequestDTO().kvnr(KVNR).telematikId(SMB_AUT_TELEMATIK_ID);

    var response = entitlementApiEndpoint.postEntitlement(KVNR, request);

    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getValidTo()).isNull();
    assertThat(response.getStatusMessage()).contains("notEntitled");
  }

  @Test
  void getBlockedUsersShouldReturnSuccessFor200Response() {
    mockEntitlementServer.stubFor(
        get(urlEqualTo("/epa/basic/api/v1/blockedusers"))
            .withHeader(X_INSURANTID, equalTo(KVNR))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody(
                        """
                                                        {
                                                          "data": [
                                                            {
                                                              "actorId": "2-883110000092414",
                                                              "oid": "1.2.276.0.76.4.51",
                                                              "displayName": "Zahnarztpraxis Norbert Freiherr Schomaker",
                                                              "at": "2025-07-01T12:00:00Z"
                                                            }
                                                          ]
                                                        }""")));

    GetBlockedUserListResponseDTO response = entitlementApiEndpoint.getBlockedUserList(KVNR);
    assertThat(response.getAssignments()).isNotNull();
    assertThat(response.getAssignments()).hasSize(1);
  }

  @Test
  void getBlockedUsersShouldReturnNotSuccessWhenApiReturns404() {
    mockEntitlementServer.stubFor(
        get(urlEqualTo("/epa/basic/api/v1/blockedusers"))
            .withHeader(X_INSURANTID, equalTo(KVNR))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(404)
                    .withBody("{" + "  \"errorCode\": \"noResource\"" + "}")));

    GetBlockedUserListResponseDTO response = entitlementApiEndpoint.getBlockedUserList(KVNR);
    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getAssignments()).isEmpty();
    assertThat(response.getStatusMessage()).contains("noResource");
  }

  @Test
  void setBlockedUsersShouldReturnSuccessFor201Response() {
    mockEntitlementServer.stubFor(
        post(urlEqualTo("/epa/basic/api/v1/blockedusers"))
            .withHeader(X_INSURANTID, equalTo(KVNR))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(201)
                    .withBody(
                        """
                                                        {
                                                          "actorId": "1-2345",
                                                          "oid": "1.2.276.0.76.4.51",
                                                          "displayName": "Zahnarztpraxis Dr. Alfons Adamiç",
                                                          "at": "2025-07-03T12:00:00Z"
                                                        }""")));

    SetBlockedUserRequestDTO requestDTO = new SetBlockedUserRequestDTO();
    requestDTO.setDisplayName("Zahnarztpraxis Dr. Alfons Adamiç");
    requestDTO.setOid("1.2.276");
    requestDTO.setActorId(SMB_AUT_TELEMATIK_ID);
    ResponseDTO response = entitlementApiEndpoint.postBlockedUser(KVNR, requestDTO);
    assertThat(response.getSuccess()).isTrue();
  }

  @Test
  void setBlockedUsersShouldReturnNotSuccessWhenApiReturns403() {
    mockEntitlementServer.stubFor(
        post(urlEqualTo("/epa/basic/api/v1/blockedusers"))
            .withHeader(X_INSURANTID, equalTo(KVNR))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(403)
                    .withBody("{" + "  \"errorCode\": \"notEntitled\"" + "}")));

    SetBlockedUserRequestDTO requestDTO = new SetBlockedUserRequestDTO();
    requestDTO.setActorId(SMB_AUT_TELEMATIK_ID);
    ResponseDTO response = entitlementApiEndpoint.postBlockedUser(KVNR, requestDTO);
    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getStatusMessage()).contains("notEntitled");
  }

  @Test
  void deleteBlockedUsersShouldReturnSuccessFor204Response() {

    mockEntitlementServer.stubFor(
        delete(urlEqualTo("/epa/basic/api/v1/blockedusers/" + SMB_AUT_TELEMATIK_ID))
            .withHeader(X_INSURANTID, equalTo(KVNR))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .willReturn(
                aResponse().withHeader("Content-Type", "application/json").withStatus(204)));

    ResponseDTO response = entitlementApiEndpoint.deleteBlockedUser(KVNR, SMB_AUT_TELEMATIK_ID);
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getStatusMessage()).isEqualTo("OK. Assignment deleted");
  }

  @Test
  void deleteBlockedUsersShouldReturnNotSuccessWhenApiReturns400() {
    mockEntitlementServer.stubFor(
        delete(urlEqualTo("/epa/basic/api/v1/blockedusers/" + SMB_AUT_TELEMATIK_ID))
            .withHeader(X_INSURANTID, equalTo(KVNR))
            .withHeader(X_USERAGENT, equalTo(USER_AGENT))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(400)
                    .withBody("{" + "  \"errorCode\": \"malformedRequest\"" + "}")));

    ResponseDTO response = entitlementApiEndpoint.deleteBlockedUser(KVNR, SMB_AUT_TELEMATIK_ID);
    assertThat(response).isNotNull();
    assertThat(response.getSuccess()).isFalse();
    assertThat(response.getStatusMessage()).contains("malformedRequest");
  }
}
