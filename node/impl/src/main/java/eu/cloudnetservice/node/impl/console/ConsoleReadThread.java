/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.console;

import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;

public final class ConsoleReadThread extends Thread {

  private final JLine3Console console;
  private CompletableFuture<String> currentTask;

  public ConsoleReadThread(@NonNull JLine3Console console) {
    this.console = console;
  }

  @Override
  public void run() {
    String line;
    while (!Thread.currentThread().isInterrupted()) {
      try {
        // blocking-reads the current line that's typed in by the user
        // this is done here to handle the thrown exceptions by the method correctly (see catch blocks below)
        // todo(derklaro): this can also throw an IOError, not sure how to handle that
        line = this.console.lineReader().readLine(this.console.prompt());
        if (!line.isBlank()) {
          // complete the current read task if any is awaiting console input
          if (this.currentTask != null) {
            this.currentTask.complete(line);
            this.currentTask = null;
          }

          // post the command line to all input handlers that are enabled at the moment
          for (var value : this.console.consoleInputHandler().values()) {
            if (value.enabled()) {
              value.handleInput(line);
            }
          }

          // notify all animations that the cursor line has been moved one down
          // this is required to allow them to react when f. ex. re-drawing styled console input
          for (var animation : this.console.runningAnimations()) {
            animation.addToCursor(1);
          }
        }
      } catch (EndOfFileException ignored) {
        // just continue reading after EOT (CTRL-D)
      } catch (UserInterruptException exception) {
        // interrupt (CTRL-C)
        System.exit(-1);
      }
    }
  }

  @NonNull
  CompletableFuture<String> currentTask() {
    if (this.currentTask == null) {
      this.currentTask = new CompletableFuture<>();
    }

    return this.currentTask;
  }
}
