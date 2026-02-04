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
package de.gematik.epa.ps.utils;

import de.gematik.epa.utils.InsurantIdHolder;
import de.gematik.epa.utils.MiscUtils;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RequestFilter implements ContainerRequestFilter {

  @Override
  public void filter(ContainerRequestContext requestContext) {
    var insurantId = getInsurantId(requestContext);
    if (insurantId != null) {
      InsurantIdHolder.setInsurantId(insurantId);
    }
  }

  @Nullable
  private static String getInsurantId(ContainerRequestContext requestContext) {
    for (String key : MiscUtils.INSURANT_ID_KEYS) {
      // check header parameters
      String insurantId = requestContext.getHeaderString(key);

      // check path parameters
      if (insurantId == null) {
        final UriInfo uriInfo = requestContext.getUriInfo();
        insurantId = uriInfo.getPathParameters().getFirst(key);
      }

      // check query parameters
      if (insurantId == null) {
        final UriInfo uriInfo = requestContext.getUriInfo();
        insurantId = uriInfo.getQueryParameters().getFirst(key);
      }

      if (insurantId != null) {
        return insurantId;
      }
    }

    return null;
  }
}
