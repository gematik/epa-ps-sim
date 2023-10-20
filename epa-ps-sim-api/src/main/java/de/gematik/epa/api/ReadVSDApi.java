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

import de.gematik.epa.dto.request.ReadVSDRequest;
import de.gematik.epa.dto.response.ReadVSDResponseDTO;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("vsds")
public interface ReadVSDApi {
  @POST
  @Path("/readVSD")
  @Consumes({"application/json"})
  @Produces({"application/json"})
  ReadVSDResponseDTO readVSD(ReadVSDRequest request);
}
