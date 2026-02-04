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

import static de.gematik.epa.unit.util.TestDataFactory.getStatusOk;
import static de.gematik.epa.unit.util.TestDataFactory.getTelematikError;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import telematik.ws.conn.connectorcommon.xsd.v5_0.Status;
import telematik.ws.conn.eventservice.wsdl.v6_1.FaultMessage;

class KonnektorUtilsTest {

  private Logger logger;

  private Status statusWarning;

  @BeforeEach
  void initialize() {
    logger = mock(Logger.class);

    statusWarning = new Status();
    statusWarning.setResult(KonnektorUtils.STATUS_WARNING);
    statusWarning.setError(getTelematikError());

    when(logger.isWarnEnabled()).thenReturn(Boolean.TRUE);
  }

  @Test
  void warnMsgWithOperationNameTest() {
    var operation = "operateNow";

    var result = assertDoesNotThrow(() -> KonnektorUtils.warnMsgWithOperationName(operation));

    assertNotNull(result);
    assertTrue(result.contains(operation));
  }

  @Test
  void logWarningTest() {
    var msg = "This is a message";
    var arg1Capture = ArgumentCaptor.forClass(Object.class);
    var arg2Capture = ArgumentCaptor.forClass(Object.class);

    assertDoesNotThrow(() -> KonnektorUtils.logWarning(logger, statusWarning, msg));

    Mockito.verify(logger).warn(Mockito.anyString(), arg1Capture.capture(), arg2Capture.capture());

    assertEquals(msg + ": ", arg1Capture.getValue());
    assertEquals(statusWarning.getError(), arg2Capture.getValue());
  }

  @Test
  void logWarningNoMsgTest() {
    var arg1Capture = ArgumentCaptor.forClass(Object.class);
    var arg2Capture = ArgumentCaptor.forClass(Object.class);

    assertDoesNotThrow(() -> KonnektorUtils.logWarning(logger, statusWarning));

    Mockito.verify(logger).warn(Mockito.anyString(), arg1Capture.capture(), arg2Capture.capture());

    assertEquals("The Konnektor responded with a warning! Warning:", arg1Capture.getValue());
    assertEquals(statusWarning.getError(), arg2Capture.getValue());
  }

  @Test
  void logWarningIfPresentTest() {
    var msg = "This is a message";
    var arg1Capture = ArgumentCaptor.forClass(Object.class);
    var arg2Capture = ArgumentCaptor.forClass(Object.class);

    assertDoesNotThrow(() -> KonnektorUtils.logWarningIfPresent(logger, statusWarning, msg));

    Mockito.verify(logger).warn(Mockito.anyString(), arg1Capture.capture(), arg2Capture.capture());

    assertEquals(msg + ": ", arg1Capture.getValue());
    assertEquals(statusWarning.getError(), arg2Capture.getValue());
  }

  @Test
  void logWarningIfPresentResultOkTest() {
    assertDoesNotThrow(() -> KonnektorUtils.logWarningIfPresent(logger, getStatusOk()));

    Mockito.verify(logger, Mockito.never()).warn(Mockito.anyString(), Mockito.any(), Mockito.any());
  }

  @Test
  void fromStatusOkTest() {
    var result = assertDoesNotThrow(() -> KonnektorUtils.fromStatus(getStatusOk()));

    assertNotNull(result);
    assertTrue(result.success());
    assertNull(result.statusMessage());
  }

  @Test
  void fromStatusWarningTest() {
    var result = assertDoesNotThrow(() -> KonnektorUtils.fromStatus(statusWarning));

    assertNotNull(result);
    assertTrue(result.success());
    assertNotNull(result.statusMessage());
    assertEquals(statusWarning.getError().toString(), result.statusMessage());
  }

  @Test
  void fromThrowableTest() {
    var throwable = new FaultMessage("Done for today", getTelematikError());

    var result = assertDoesNotThrow(() -> KonnektorUtils.fromThrowable(throwable));

    assertNotNull(result);
    assertFalse(result.success());
    assertNotNull(result.statusMessage());
    assertTrue(result.statusMessage().contains(throwable.getFaultInfo().toString()));
    assertTrue(result.statusMessage().contains(throwable.getMessage()));
  }
}
