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
package de.gematik.epa.ps.config;

import de.gematik.epa.ps.popp.PoppTokenServiceProvider;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * Use this to register servers defined in application.yaml. <br>
 * Example: {@link PoppTokenServiceProvider}.
 */
@Data
public class ServerConfiguration {

  private String protocol;
  private String host;
  private String port;
  private String path;

  public String buildUrl() {
    return (StringUtils.isBlank(path))
        ? protocol + "://" + host + ":" + port
        : protocol + "://" + host + ":" + port + "/" + path;
  }
}
