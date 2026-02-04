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
package de.gematik.epa.ps.kob.util;

import de.gematik.epa.api.psTestdriver.dto.Action;
import de.gematik.epa.api.psTestdriver.dto.Action.TypeEnum;
import de.gematik.epa.api.psTestdriver.dto.ErrorMessage;
import de.gematik.epa.api.psTestdriver.dto.Status;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Slf4j
@Builder(toBuilder = true)
@Accessors(chain = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KobTestdriverAction implements Runnable {

  @EqualsAndHashCode.Exclude
  private final List<CompletableFuture<KobTestdriverAction>> completionFutures = new ArrayList<>();

  private Supplier<KobTestdriverAction> actionRunnable;
  @Builder.Default private UUID id = UUID.randomUUID();
  private Status status;
  private TypeEnum type;
  private String requestUrl;
  private ErrorMessage error;

  /**
   * Returns a CompletableFuture that will be completed when the action is finished (when the status
   * is not PENDING or PROCESSING anymore)
   */
  public CompletableFuture<KobTestdriverAction> retrieveCompletionFuture() {
    final CompletableFuture<KobTestdriverAction> newFuture = new CompletableFuture<>();
    synchronized (completionFutures) {
      completionFutures.add(newFuture);
    }
    // race condition: if the status is already not PENDING or PROCESSING anymore, we need to
    // complete the future immediately
    if (!Status.PENDING.equals(getStatus())
        && !Status.PROCESSING.equals(getStatus())
        && !newFuture.isDone()) {
      newFuture.complete(this);
    }
    return newFuture;
  }

  @Override
  public void run() {
    try {
      log.info("Starting action {}", id);
      val result = actionRunnable.get();
      log.info("Calculations for action {} finished with status {}", id, result.getStatus());
      setError(result.getError());
      if (result.getStatus() == null) {
        setStatus(Status.SUCCESSFUL);
      } else {
        setStatus(result.getStatus());
      }
      synchronized (completionFutures) {
        completionFutures.forEach(
            future -> {
              future.complete(this);
              log.info("Completed future {}", future);
            });
        completionFutures.clear();
      }
      log.info("Action {} finished with status {}", id, getStatus());
    } catch (Exception e) {
      log.error("Error while executing action", e);
      setStatus(Status.FAILED);
      setError(
          new ErrorMessage()
              .message("Error while executing action: " + e.getMessage())
              .details(ExceptionUtils.getStackTrace(e)));
    }
  }

  public static Action mapAction(KobTestdriverAction testdriverAction) {
    return new Action()
        .status(testdriverAction.getStatus())
        .error(testdriverAction.getError())
        .id(testdriverAction.getId())
        .requestUrl(testdriverAction.getRequestUrl())
        .type(testdriverAction.getType());
  }
}
