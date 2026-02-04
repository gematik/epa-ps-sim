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
package de.gematik.epa.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.JsonParser;
import ca.uhn.fhir.rest.api.SearchTotalModeEnum;
import java.util.Collections;
import java.util.List;
import lombok.SneakyThrows;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FhirUtilsTest {
  private IParser jsonParser;
  private IParser xmlParser;

  @BeforeEach
  void setUp() {
    jsonParser = mock(JsonParser.class);
    xmlParser = mock(IParser.class);
    FhirUtils.setJsonParser(jsonParser);
    FhirUtils.setXmlParser(xmlParser);
  }

  @ParameterizedTest
  @ValueSource(strings = {"accurate", "estimated", "none"})
  void calculateTotalMode(String total) {
    SearchTotalModeEnum expected =
        switch (total) {
          case "accurate" -> SearchTotalModeEnum.ACCURATE;
          case "estimated" -> SearchTotalModeEnum.ESTIMATED;
          default -> SearchTotalModeEnum.NONE;
        };
    assertEquals(expected, FhirUtils.calculateTotalMode(total));
  }

  @SneakyThrows
  @Test
  void extractDataAsJsonShouldReturnJsonStringsList() {
    // given
    Bundle bundle = mock(Bundle.class);
    Bundle.BundleEntryComponent entry1 = mock(Bundle.BundleEntryComponent.class);
    Bundle.BundleEntryComponent entry2 = mock(Bundle.BundleEntryComponent.class);
    AuditEvent resource1 = mock(AuditEvent.class);
    AuditEvent resource2 = mock(AuditEvent.class);

    when(bundle.getEntry()).thenReturn(List.of(entry1, entry2));
    when(entry1.getResource()).thenReturn(resource1);
    when(entry2.getResource()).thenReturn(resource2);

    when(jsonParser.encodeResourceToString(resource1)).thenReturn("json1");
    when(jsonParser.encodeResourceToString(resource2)).thenReturn("json2");

    // when
    List<String> actualResult = FhirUtils.extractDataAsJson(bundle);

    // then
    assertEquals(List.of("json1", "json2"), actualResult);
  }

  @Test
  void extractDataAsJsonShouldReturnEmptyListForEmptyBundle() {
    // given
    Bundle bundle = mock(Bundle.class);
    when(bundle.getEntry()).thenReturn(Collections.emptyList());

    // when
    List<String> actualResult = FhirUtils.extractDataAsJson(bundle);

    // then
    assertEquals(Collections.emptyList(), actualResult);
  }

  @Test
  void asJson() {
    // given
    Resource resource = mock(Resource.class);
    String expectedJson = "{\"resourceType\":\"AuditEvent\"}";

    when(jsonParser.encodeResourceToString(resource)).thenReturn(expectedJson);

    // when
    String actualJson = FhirUtils.asJson(resource);

    // then
    assertEquals(expectedJson, actualJson);
  }

  @Test
  void fromString() {
    // given
    final String resourceString =
        """
                      {
                      "resourceType": "Bundle",
                      "type": "transaction",
                      "entry": []
                    }
                    """;

    // when
    FhirUtils.fromString(resourceString);

    // then
    verify(jsonParser, times(1)).parseResource(resourceString);
  }

  @SneakyThrows
  @Test
  void extractDataAsXmlShouldReturnXml() {
    // given
    Bundle bundle = mock(Bundle.class);
    Bundle.BundleEntryComponent entry1 = mock(Bundle.BundleEntryComponent.class);
    Bundle.BundleEntryComponent entry2 = mock(Bundle.BundleEntryComponent.class);
    AuditEvent resource1 = mock(AuditEvent.class);
    AuditEvent resource2 = mock(AuditEvent.class);

    when(bundle.getEntry()).thenReturn(List.of(entry1, entry2));
    when(entry1.getResource()).thenReturn(resource1);
    when(entry2.getResource()).thenReturn(resource2);

    when(xmlParser.encodeResourceToString(resource1)).thenReturn("xml1");
    when(xmlParser.encodeResourceToString(resource2)).thenReturn("xml2");

    // when
    List<String> actualResult = FhirUtils.extractDataAsXml(bundle);

    // then
    assertEquals(List.of("xml1", "xml2"), actualResult);
  }

  @Test
  void extractDataAsXmlShouldReturnEmptyListForEmptyBundle() {
    // given
    Bundle bundle = mock(Bundle.class);
    when(bundle.getEntry()).thenReturn(Collections.emptyList());

    // when
    List<String> actualResult = FhirUtils.extractDataAsXml(bundle);

    // then
    assertEquals(Collections.emptyList(), actualResult);
  }

  @Test
  void extractDataShouldReturnJson() {
    // given
    Bundle bundle = mock(Bundle.class);
    Bundle.BundleEntryComponent entry1 = mock(Bundle.BundleEntryComponent.class);
    AuditEvent resource1 = mock(AuditEvent.class);

    when(bundle.getEntry()).thenReturn(List.of(entry1));
    when(entry1.getResource()).thenReturn(resource1);
    when(jsonParser.encodeResourceToString(resource1)).thenReturn("json1");

    // when
    List<String> actualResult = FhirUtils.extractData(bundle, "application/fhir+json");

    // then
    assertEquals(List.of("json1"), actualResult);
  }

  @Test
  void extractDataShouldReturnXml() {
    // given
    Bundle bundle = mock(Bundle.class);
    Bundle.BundleEntryComponent entry1 = mock(Bundle.BundleEntryComponent.class);
    AuditEvent resource1 = mock(AuditEvent.class);

    when(bundle.getEntry()).thenReturn(List.of(entry1));
    when(entry1.getResource()).thenReturn(resource1);
    when(xmlParser.encodeResourceToString(resource1)).thenReturn("xml1");

    // when
    List<String> actualResult = FhirUtils.extractData(bundle, "application/fhir+xml");

    // then
    assertEquals(List.of("xml1"), actualResult);
  }

  @Test
  void extractDataShouldDefaultToJson() {
    // given
    Bundle bundle = mock(Bundle.class);
    Bundle.BundleEntryComponent entry1 = mock(Bundle.BundleEntryComponent.class);
    AuditEvent resource1 = mock(AuditEvent.class);

    when(bundle.getEntry()).thenReturn(List.of(entry1));
    when(entry1.getResource()).thenReturn(resource1);
    when(jsonParser.encodeResourceToString(resource1)).thenReturn("json1");

    // when
    List<String> actualResult = FhirUtils.extractData(bundle, "unknown/format");

    // then
    assertEquals(List.of("json1"), actualResult);
  }

  @Test
  void shouldGiveAsXml() {
    // given
    Resource resource = mock(Resource.class);
    String expectedXml = "<AuditEvent xmlns=\"http://hl7.org/fhir\"/>";

    when(xmlParser.encodeResourceToString(resource)).thenReturn(expectedXml);

    // when
    String actualXml = FhirUtils.asXml(resource);

    // then
    assertEquals(expectedXml, actualXml);
  }
}
