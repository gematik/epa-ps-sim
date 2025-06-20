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
package de.gematik.epa.ps.document;

import de.gematik.epa.config.AppConfig;
import de.gematik.epa.config.DefaultdataProvider;
import de.gematik.epa.config.InsurantIdBuilder;
import de.gematik.epa.document.cxf.DocumentInterfacesCxfImpl;
import de.gematik.epa.ps.config.DefaultdataConfig;
import de.gematik.epa.ps.document.config.DocumentConfigurationData;
import de.gematik.epa.ps.utils.SpringUtils;
import java.util.Optional;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;

@Configuration
@Slf4j
@Accessors(fluent = true)
@Profile("!test")
@EnableConfigurationProperties(DocumentConfigurationData.class)
public class DocumentClientConfiguration {

  @Getter protected DocumentConfigurationData documentConfiguration;
  @Getter protected final DefaultdataConfig defaultdata;

  @Getter protected final ResourceLoader resourceLoader;

  private DocumentInterfacesCxfImpl documentInterfaceAssembly;
  private InsurantIdBuilder insurantIdBuilder;
  private DefaultdataProvider defaultdataProvider;
  @Autowired private AppConfig appConfig;

  @Autowired
  public DocumentClientConfiguration(
      DocumentConfigurationData documentConfiguration,
      DefaultdataConfig defaultdata,
      ResourceLoader resourceLoader) {
    this.documentConfiguration = documentConfiguration;
    this.defaultdata = defaultdata;
    this.resourceLoader = resourceLoader;
  }

  @Bean(name = "documentInterfaceAssembly")
  public DocumentInterfacesCxfImpl documentInterfaceAssembly() {
    return Optional.ofNullable(documentInterfaceAssembly)
        .orElseGet(
            () -> {
              documentInterfaceAssembly = createNewDocumentInterfaceAssembly();
              return documentInterfaceAssembly;
            });
  }

  @Bean(name = "insurantBuilder")
  public InsurantIdBuilder insurantBuilder() {
    return Optional.ofNullable(insurantIdBuilder)
        .orElseGet(
            () -> {
              insurantIdBuilder = new InsurantIdBuilder();
              return insurantIdBuilder;
            });
  }

  @Bean(name = "defaultDataProvider")
  public DefaultdataProvider defaultDataProvider() {
    return Optional.ofNullable(defaultdataProvider)
        .orElseGet(
            () -> {
              defaultdataProvider = new DefaultdataProvider();
              defaultdataProvider.defaultdata(defaultdata);
              return defaultdataProvider;
            });
  }

  protected DocumentInterfacesCxfImpl createNewDocumentInterfaceAssembly() {
    var impl =
        new DocumentInterfacesCxfImpl(
            filePath ->
                SpringUtils.findReadableResource(resourceLoader, filePath).getInputStream());
    impl.update(documentConfiguration.connection(), appConfig);
    return impl;
  }
}
