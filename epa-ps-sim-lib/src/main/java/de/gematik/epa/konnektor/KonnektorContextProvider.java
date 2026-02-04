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
package de.gematik.epa.konnektor;

import de.gematik.epa.api.testdriver.config.Context;
import java.util.MissingResourceException;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import telematik.ws.conn.connectorcontext.xsd.v2_0.ContextType;

/**
 * Class to provide the Konnektor Context for client requests of Konnektor operations.<br>
 * The requests for Konnektor operations triggered by this library will get the Konnektor {@link
 * telematik.ws.conn.connectorcontext.xsd.v2_0.ContextType}.<br>
 * A using application must supply a {@link KonnektorConfigurationProvider}.<br>
 * ContextType is created and cached in a ThreadLocal field, thus being multi threading capable. For
 * the creation of the {@link ContextType} the KVNR must be supplied, which should be included in
 * the clients request.
 */
@RequiredArgsConstructor
@Accessors(fluent = true)
@Data
public class KonnektorContextProvider {

  private final KonnektorConfigurationProvider konnektorConfigurationProvider;

  private final KonnektorInterfaceAssembly konnektorInterfaceAssembly;

  @Getter(AccessLevel.PROTECTED)
  private final ThreadLocal<ContextType> contextType = new ThreadLocal<>();

  public ContextType createContextType() {
    contextType.set(
        new ContextType()
            .withUserId(konnektorContext().userId())
            .withMandantId(konnektorContext().mandantId())
            .withClientSystemId(konnektorContext().clientSystemId())
            .withWorkplaceId(konnektorContext().workplaceId()));
    return contextType.get();
  }

  public ContextType getContextType() {
    return contextType.get();
  }

  public ContextType getContext() {
    return Optional.ofNullable(getContextType())
        .or(
            () ->
                Optional.ofNullable(konnektorContext())
                    .map(
                        konCtx ->
                            new ContextType()
                                .withWorkplaceId(konCtx.workplaceId())
                                .withMandantId(konCtx.mandantId())
                                .withUserId(konCtx.userId())
                                .withClientSystemId(konCtx.clientSystemId())))
        .orElseThrow(
            () ->
                new MissingResourceException(
                    "No context has been set at the ContextProvider",
                    this.getClass().getSimpleName(),
                    "konnektorContext"));
  }

  /**
   * Remove the ContextHeader of the current operation from the cache. Purpose of this function is
   * to prevent the cache from swelling up with old data over time. So it is not strictly necessary
   * for the functionality, but rather for reducing memory usage and to prevent decreasing
   * performance during runtime.
   */
  public void removeContextHeader() {
    contextType.remove();
  }

  private Context konnektorContext() {
    return konnektorConfigurationProvider.context();
  }
}
