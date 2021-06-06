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

package de.dytanic.cloudnet.console;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.console.animation.AbstractConsoleAnimation;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;

public class ConsoleReadThread extends Thread {

  private final JLine3Console console;
  private CompletableTask<String> currentTask;

  public ConsoleReadThread(JLine3Console console) {
    this.console = console;
  }

  @Override
  public void run() {
    String line;
    while (!Thread.interrupted() && (line = this.readLine()) != null) {
      if (this.currentTask != null) {
        this.currentTask.complete(line);
        this.currentTask = null;
      }

      for (ConsoleHandler<Consumer<String>> value : this.console.getConsoleInputHandler().values()) {
        if (value.isEnabled()) {
          value.getHandler().accept(line);
        }
      }

      for (AbstractConsoleAnimation animation : this.console.getRunningAnimations()) {
        animation.addToCursor(1);
      }
    }
  }

  private @Nullable String readLine() {
    try {
      return this.console.getLineReader().readLine(this.console.getPrompt());
    } catch (EndOfFileException ignored) {
    } catch (UserInterruptException exception) {
      System.exit(-1);
    }

    return null;
  }

  protected @NotNull ITask<String> getCurrentTask() {
    if (this.currentTask == null) {
      this.currentTask = new CompletableTask<>();
    }

    return this.currentTask;
  }
}
