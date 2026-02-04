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
package de.gematik.epa.document.cxf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.epa.api.testdriver.config.AddressConfig;
import de.gematik.epa.config.AppConfig;
import de.gematik.epa.document.cxf.DocumentInterfacesCxfImpl.FileLoader;
import de.gematik.epa.unit.util.ResourceLoader;
import de.gematik.epa.unit.util.TestDataFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DocumentInterfacesCxfImplTest {

  @Test
  void updateTest() {
    var docCfg = TestDataFactory.createDocumentConnectionConfiguration();
    var tstObj = new DocumentInterfacesCxfImplForTest();

    var alsoTstObj = Assertions.assertDoesNotThrow(() -> tstObj.update(docCfg, new AppConfig()));
    assertEquals(docCfg, alsoTstObj.configuration);
  }

  @Accessors(fluent = true)
  static class DocumentInterfacesCxfImplForTest extends DocumentInterfacesCxfImpl {

    public DocumentInterfacesCxfImplForTest() {
      super(new TestFileLoader());
    }
  }

  static class TestFileLoader implements FileLoader {

    @Override
    public InputStream process(String filePath) {
      return new ByteArrayInputStream(ResourceLoader.readBytesFromResource(filePath));
    }
  }

  @Test
  void createURlTest() throws MalformedURLException {
    var tstObj = new DocumentInterfacesCxfImplForTest();

    AddressConfig mockedAddressConfig = mock(AddressConfig.class);
    when(mockedAddressConfig.createUrl()).thenReturn(new URL("http://example.com/"));

    String res = tstObj.createServiceUrl(mockedAddressConfig);
    assertEquals("http://example.com/I_Document_Management", res);
  }

  @Test
  void createURlExceptionTest() throws MalformedURLException {
    var tstObj = new DocumentInterfacesCxfImplForTest();
    AddressConfig mockedAddressConfig = mock(AddressConfig.class);
    when(mockedAddressConfig.createUrl())
        .thenThrow(new MalformedURLException("Mocked MalformedURLException"));

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class, () -> tstObj.createServiceUrl(mockedAddressConfig));
    assertEquals("Error creating the service URL", exception.getMessage());
    assertInstanceOf(MalformedURLException.class, exception.getCause());
    assertEquals("Mocked MalformedURLException", exception.getCause().getMessage());
  }
}
