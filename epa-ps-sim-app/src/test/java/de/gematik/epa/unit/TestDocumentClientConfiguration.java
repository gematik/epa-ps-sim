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
package de.gematik.epa.unit;

import static de.gematik.epa.unit.AppTestDataFactory.getAdhocQueryResponse;

import de.gematik.epa.document.cxf.DocumentInterfacesCxfImpl;
import de.gematik.epa.ps.config.DefaultdataConfig;
import de.gematik.epa.ps.document.DocumentClientConfiguration;
import de.gematik.epa.ps.document.config.DocumentConfigurationData;
import org.mockito.Mockito;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;
import xds.document.wsdl.IDocumentManagementPortType;

@TestConfiguration
@ComponentScan("de.gematik.epa.ps")
@Profile("test")
@EnableConfigurationProperties({DocumentConfigurationData.class})
public class TestDocumentClientConfiguration extends DocumentClientConfiguration {

  public TestDocumentClientConfiguration(
      DocumentConfigurationData documentConfigurationData,
      DefaultdataConfig defaultdataConfig,
      ResourceLoader resourceLoader) {
    super(documentConfigurationData, defaultdataConfig, resourceLoader);
  }

  @Override
  protected DocumentInterfacesCxfImpl createNewDocumentInterfaceAssembly() {
    return createDocumentInterfaceCxfImpl();
  }

  protected DocumentInterfacesCxfImpl createDocumentInterfaceCxfImpl() {
    DocumentInterfacesCxfImpl documentInterfacesCxf =
        new DocumentInterfacesCxfImpl(
            filePath -> resourceLoader.getResource(filePath).getInputStream());
    var documentService = Mockito.mock(IDocumentManagementPortType.class);
    documentInterfacesCxf.documentService(documentService);

    Mockito.when(documentService.documentRegistryRegistryStoredQuery(Mockito.any()))
        .thenReturn(getAdhocQueryResponse());
    return documentInterfacesCxf;
  }
}
