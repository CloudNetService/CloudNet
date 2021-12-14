/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
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
 */

package de.dytanic.cloudnet.console.animation;

import com.google.common.base.Verify;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.console.IConsole;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractConsoleAnimation implements Runnable {

  protected static final Logger LOGGER = LogManager.getLogger(AbstractConsoleAnimation.class);

  protected final int updateInterval;
  protected final Collection<Runnable> finishHandler = new ArrayList<>();

  protected int cursorUp = 1;
  protected boolean staticCursor;

  protected IConsole console;
  protected Instant startInstant;

  public AbstractConsoleAnimation(int updateInterval) {
    this.updateInterval = updateInterval;
  }

  public void addToCursor(int cursor) {
    if (!this.isStaticCursor()) {
      this.cursorUp += cursor;
    }
  }

  public void addFinishHandler(@NotNull Runnable finishHandler) {
    this.finishHandler.add(finishHandler);
  }

  protected void print(String @NotNull ... input) {
    if (input.length != 0) {
      var ansi = Ansi.ansi().saveCursorPosition().cursorUp(this.cursorUp).eraseLine(Ansi.Erase.ALL);
      for (var a : input) {
        ansi.a(a);
      }

      this.console.forceWrite(ansi.restoreCursorPosition().toString());
    }
  }

  protected void eraseLastLine() {
    this.console.writeRaw(Ansi.ansi().reset().cursorUp(1).eraseLine().toString());
  }

  /**
   * @return if the animation is finished and should be cancelled
   */
  protected abstract boolean handleTick();

  @Override
  public final void run() {
    // set the start time
    this.startInstant = Instant.now();
    // run the animation as long as the animation needs to run
    while (!Thread.interrupted() && !this.handleTick()) {
      try {
        //noinspection BusyWait
        Thread.sleep(this.updateInterval);
      } catch (InterruptedException exception) {
        LOGGER.severe("Exception while awaiting console update", exception);
      }
    }
  }

  public void handleDone() {
    // post the result to the finish handlers
    for (var runnable : this.finishHandler) {
      runnable.run();
    }
  }

  public boolean isStaticCursor() {
    return this.staticCursor;
  }

  public int getUpdateInterval() {
    return this.updateInterval;
  }

  public @NotNull IConsole getConsole() {
    return this.console;
  }

  public void setConsole(@NotNull IConsole console) {
    Verify.verify(this.console == null, "Cannot set console of animation twice");
    this.console = console;
  }

  public void resetConsole() {
    Verify.verify(this.console != null, "Console is not set");
    this.console = null;
  }
}
