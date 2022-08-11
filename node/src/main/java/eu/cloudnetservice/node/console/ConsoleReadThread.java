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

package eu.cloudnetservice.node.console;

import eu.cloudnetservice.common.concurrent.Task;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;

public class ConsoleReadThread extends Thread {

  private final JLine3Console console;
  private Task<String> currentTask;

  public ConsoleReadThread(@NonNull JLine3Console console) {
    this.console = console;
  }

  @Override
  public void run() {
    String line;
    while (!Thread.currentThread().isInterrupted() && (line = this.readLine()) != null) {
      if (this.currentTask != null) {
        this.currentTask.complete(line);
        this.currentTask = null;
      }

      for (var value : this.console.consoleInputHandler().values()) {
        if (value.enabled()) {
          value.handleInput(line);
        }
      }

      for (var animation : this.console.runningAnimations()) {
        animation.addToCursor(1);
      }
    }
  }

  private @Nullable String readLine() {
    try {
      return this.console.lineReader().readLine(this.console.prompt());
    } catch (EndOfFileException ignored) {
    } catch (UserInterruptException exception) {
      System.exit(-1);
    }

    return null;
  }

  protected @NonNull Task<String> currentTask() {
    if (this.currentTask == null) {
      this.currentTask = new Task<>();
    }

    return this.currentTask;
  }
}
