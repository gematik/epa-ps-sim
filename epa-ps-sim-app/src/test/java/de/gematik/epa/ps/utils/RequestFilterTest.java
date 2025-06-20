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
package de.gematik.epa.ps.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.epa.utils.InsurantIdHolder;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RequestFilterTest {

  private final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
  private final UriInfo uriInfo = mock(UriInfo.class);

  private final RequestFilter requestFilter = new RequestFilter();

  @BeforeEach
  void setup() {
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(uriInfo.getPathParameters()).thenReturn(new MultivaluedStringMap());
    when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedStringMap());
  }

  @AfterEach
  void clear() {
    InsurantIdHolder.clear();
  }

  @Test
  void shouldSetInsurantIdFromHeader() {
    var insurantId = "12345";
    var key = "insurantId";
    when(requestContext.getHeaderString(key)).thenReturn(insurantId);

    requestFilter.filter(requestContext);
    assertThat(InsurantIdHolder.getInsurantId()).isEqualTo(insurantId);
  }

  @Test
  void shouldSetInsurantIdFromPathParameters() {
    MultivaluedMap<String, String> pathParameters = new MultivaluedHashMap<>();
    var insurantId = "12345";
    pathParameters.add("insurantId", insurantId);
    when(uriInfo.getPathParameters()).thenReturn(pathParameters);

    requestFilter.filter(requestContext);
    assertThat(InsurantIdHolder.getInsurantId()).isEqualTo(insurantId);
  }

  @Test
  void shouldSetInsurantIdFromQueryParameters() {
    MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();
    var insurantId = "12345";
    queryParameters.add("insurantId", insurantId);
    when(uriInfo.getQueryParameters()).thenReturn(queryParameters);

    requestFilter.filter(requestContext);
    assertThat(InsurantIdHolder.getInsurantId()).isEqualTo(insurantId);
  }

  @Test
  void shouldNotSetInsurantIdWhenNotPresent() {
    requestFilter.filter(requestContext);

    assertThat(InsurantIdHolder.getInsurantId()).isNull();
  }
}
