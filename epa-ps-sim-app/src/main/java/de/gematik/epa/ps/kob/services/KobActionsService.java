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
package de.gematik.epa.ps.kob.services;

import de.gematik.epa.api.psTestdriver.dto.Action;
import de.gematik.epa.api.psTestdriver.dto.ErrorMessage;
import de.gematik.epa.api.psTestdriver.dto.Status;
import de.gematik.epa.ps.kob.util.KobTestdriverAction;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class KobActionsService {

  private final ExecutorService executors = Executors.newCachedThreadPool();
  private final Map<UUID, KobTestdriverAction> actions = new ConcurrentHashMap<>();

  public List<Action> listActiveActions() {
    return actions.values().stream().map(KobTestdriverAction::mapAction).toList();
  }

  public KobTestdriverAction retrieveAction(UUID id) {
    return Optional.ofNullable(actions.get(id))
        .orElseThrow(() -> new IllegalArgumentException("Action with id '" + id + "'not found"));
  }

  public KobTestdriverAction createTestdriverActionWithErrorOnly(
      Action.TypeEnum type, Supplier<Optional<ErrorMessage>> actualAction) {
    final KobTestdriverAction action =
        new KobTestdriverAction()
            .setActionRunnable(
                () ->
                    actualAction
                        .get()
                        .map(
                            error ->
                                new KobTestdriverAction().setError(error).setStatus(Status.FAILED))
                        .orElseGet(KobTestdriverAction::new));
    action.setType(type);
    return createTestdriverAction(action);
  }

  public KobTestdriverAction createTestdriverAction(KobTestdriverAction action) {
    actions.put(action.getId(), action);
    action.setStatus(Status.PROCESSING);
    executors.submit(action);
    return action;
  }

  public byte[] takeScreenshot(UUID id) {
    val action = actions.get(id);
    if (action == null) {
      throw new IllegalArgumentException("Action with id '" + id + "' not found");
    }
    return generateScreenshot(action.getId().toString() + ": " + action.getType());
  }

  @SneakyThrows
  private byte[] generateScreenshot(String text) {
    int width = 200;
    int height = 50;

    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = image.createGraphics();

    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setColor(Color.WHITE); // Background color
    g2d.fillRect(0, 0, width, height);
    g2d.setColor(Color.BLACK); // Text color
    g2d.setFont(new Font("Arial", Font.BOLD, 20));
    g2d.drawString(text, 30, 30);
    g2d.dispose();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(image, "png", baos);

    return baos.toByteArray();
  }
}
