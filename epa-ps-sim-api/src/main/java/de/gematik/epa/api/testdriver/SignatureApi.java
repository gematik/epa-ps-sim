/*-
 * #%L
 * epa-ps-sim-api
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
package de.gematik.epa.api.testdriver;

import de.gematik.epa.api.testdriver.dto.request.SignDocumentRequest;
import de.gematik.epa.api.testdriver.dto.response.SignDocumentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("signature")
public interface SignatureApi {

  @POST
  @Path("/signDocument")
  @Consumes({"application/json"})
  @Produces({"application/json"})
  @Operation(
      summary = "Dokument signieren",
      description =
          "Dokument durch den Konnektor signieren lassen. Gegenwärtig wird nur CMS Signatur mit eingebettetem Dokument unterstützt",
      requestBody =
          @RequestBody(
              required = true,
              description = "Dokument welches signiert werden soll, sowie Signaturparameter",
              content = @Content(schema = @Schema(implementation = SignDocumentRequest.class))),
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Erstellte Signatur im Erfolgsfall, andernfalls Fehlerinformationen",
            content = @Content(schema = @Schema(implementation = SignDocumentResponse.class)))
      })
  SignDocumentResponse signDocument(SignDocumentRequest request);
}
