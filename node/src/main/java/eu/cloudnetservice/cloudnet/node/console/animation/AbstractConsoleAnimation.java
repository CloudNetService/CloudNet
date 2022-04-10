/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.node.console.animation;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.node.console.Console;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import lombok.NonNull;
import org.fusesource.jansi.Ansi;

public abstract class AbstractConsoleAnimation implements Runnable {

  protected static final Logger LOGGER = LogManager.logger(AbstractConsoleAnimation.class);

  protected final int updateInterval;
  protected final Collection<Runnable> finishHandler = new ArrayList<>();

  protected int cursorUp = 0;
  protected boolean staticCursor;

  protected Console console;
  protected Instant startInstant;

  public AbstractConsoleAnimation(int updateInterval) {
    this.updateInterval = updateInterval;
  }

  public void addToCursor(int cursor) {
    if (!this.staticCursor()) {
      this.cursorUp += cursor;
    }
  }

  public void addFinishHandler(@NonNull Runnable finishHandler) {
    this.finishHandler.add(finishHandler);
  }

  protected void print(String @NonNull ... input) {
    if (input.length != 0) {
      this.console.writeRaw(() -> {
        var ansi = Ansi.ansi().saveCursorPosition().cursorUp(this.cursorUp).eraseLine(Ansi.Erase.ALL);
        for (var a : input) {
          ansi.a(a);
        }

        return ansi.restoreCursorPosition().toString();
      });
    }
  }

  protected void eraseLastLine() {
    this.console.writeRaw(() -> Ansi.ansi().reset().cursorUp(1).eraseLine().toString());
  }

  /**
   * @return if the animation is finished and should be cancelled
   */
  protected abstract boolean handleTick();

  @Override
  public final void run() {
    // set the start time
    this.startInstant = Instant.now();
    // move the input one down to keep the last line in the console
    this.console.forceWriteLine(System.lineSeparator());
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
    // clear all finish handlers after the setup is done
    this.finishHandler.clear();
  }

  public boolean staticCursor() {
    return this.staticCursor;
  }

  public int updateInterval() {
    return this.updateInterval;
  }

  public @NonNull Console console() {
    return this.console;
  }

  public void console(@NonNull Console console) {
    Preconditions.checkState(this.console == null, "Cannot set console of animation twice");
    this.console = console;
  }

  public void resetConsole() {
    Preconditions.checkState(this.console != null, "Console is not set");
    this.console = null;
  }
}
