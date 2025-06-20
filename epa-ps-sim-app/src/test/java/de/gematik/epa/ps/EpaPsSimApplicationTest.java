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
package de.gematik.epa.ps;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import de.gematik.epa.config.DefaultdataProvider;
import de.gematik.epa.document.cxf.DocumentInterfacesCxfImpl;
import de.gematik.epa.fhir.client.FhirClient;
import de.gematik.epa.konnektor.KonnektorConfigurationProvider;
import de.gematik.epa.konnektor.KonnektorContextProvider;
import de.gematik.epa.konnektor.cxf.KonnektorInterfacesCxfImpl;
import de.gematik.epa.ps.fhir.config.TestFhirClientProvider;
import de.gematik.epa.unit.TestDocumentClientConfiguration;
import de.gematik.epa.unit.TestKonnektorClientConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(
    classes = {
      TestKonnektorClientConfiguration.class,
      TestDocumentClientConfiguration.class,
      TestFhirClientProvider.class
    })
@ComponentScan(basePackages = {"de.gematik.epa.ps"})
class EpaPsSimApplicationTest {

  @Autowired JacksonJsonProvider jsonProvider;

  @Autowired KonnektorConfigurationProvider konnektorConfigurationProvider;

  @Autowired KonnektorInterfacesCxfImpl konnektorInterfaceAssembly;

  @Autowired KonnektorContextProvider konnektorContextProvider;

  @Autowired DocumentInterfacesCxfImpl documentInterfaceAssembly;

  @Autowired DefaultdataProvider defaultdataProvider;

  @Autowired FhirClient fhirClient;

  @Test
  void epaPsSimApplicationTest() {
    assertNotNull(jsonProvider);
    assertNotNull(konnektorConfigurationProvider);
    assertNotNull(konnektorInterfaceAssembly);
    assertNotNull(konnektorContextProvider);
    assertNotNull(defaultdataProvider);
    assertNotNull(documentInterfaceAssembly);
    assertNotNull(fhirClient);
  }
}
