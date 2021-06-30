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

import de.dytanic.cloudnet.console.IConsole;
import java.util.ArrayList;
import java.util.Collection;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractConsoleAnimation implements Runnable {

  private final Collection<Runnable> finishHandler = new ArrayList<>();
  private String name;
  private IConsole console;
  private int updateInterval = 25;
  private long startTime;
  private int cursorUp = 1;
  private boolean staticCursor;

  public AbstractConsoleAnimation() {
  }

  public AbstractConsoleAnimation(String name) {
    this.name = name;
  }

  public @NotNull String getName() {
    return this.name;
  }

  public long getTimeElapsed() {
    return System.currentTimeMillis() - this.startTime;
  }

  public boolean isStaticCursor() {
    return this.staticCursor;
  }

  public void setStaticCursor(boolean staticCursor) {
    this.staticCursor = staticCursor;
  }

  public @NotNull IConsole getConsole() {
    return this.console;
  }

  public void setConsole(@NotNull IConsole console) {
    if (this.console != null) {
      throw new IllegalStateException("Cannot set console twice");
    }

    this.console = console;
  }

  public void addToCursor(int cursor) {
    if (!this.isStaticCursor()) {
      this.cursorUp += cursor;
    }
  }

  public void setCursor(int cursor) {
    this.cursorUp = cursor;
  }

  public long getStartTime() {
    return this.startTime;
  }

  public int getUpdateInterval() {
    return this.updateInterval;
  }

  public void setUpdateInterval(int updateInterval) {
    this.updateInterval = updateInterval;
  }

  public void addFinishHandler(@NotNull Runnable finishHandler) {
    this.finishHandler.add(finishHandler);
  }

  protected void print(@NonNls String... input) {
    if (input.length == 0) {
      return;
    }

    Ansi ansi = Ansi.ansi().saveCursorPosition().cursorUp(this.cursorUp).eraseLine(Ansi.Erase.ALL);
    for (String a : input) {
      ansi.a(a);
    }

    this.console.forceWrite(ansi.restoreCursorPosition().toString());
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
    this.startTime = System.currentTimeMillis();
    while (!Thread.interrupted() && !this.handleTick()) {
      try {
        Thread.sleep(this.updateInterval);
      } catch (InterruptedException exception) {
        exception.printStackTrace();
      }
    }

    for (Runnable runnable : this.finishHandler) {
      runnable.run();
    }
  }
}
