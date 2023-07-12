/*
 * Copyright 2023 gematik GmbH
 *
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
 */

package de.gematik.epa.api;

import de.gematik.epa.dto.request.GetAuthorizationStateRequest;
import de.gematik.epa.dto.request.PermissionHcpoRequest;
import de.gematik.epa.dto.response.GetAuthorizationStateResponseDTO;
import de.gematik.epa.dto.response.ResponseDTO;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("permission")
public interface PermissionApi {

  @POST
  @Path("/permissionHcpo")
  @Consumes({"application/json"})
  @Produces({"application/json"})
  ResponseDTO permissionHcpo(PermissionHcpoRequest request);

  @POST
  @Path("/getAuthorizationState")
  @Consumes({"application/json"})
  @Produces({"application/json"})
  GetAuthorizationStateResponseDTO getAuthorizationState(GetAuthorizationStateRequest request);
}
