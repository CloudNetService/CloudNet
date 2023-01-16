/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.launcher.java17.cnl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import lombok.NonNull;

public final class CnlInterpreter {

  private final Map<String, CnlCommand> commands = new HashMap<>();

  public void registerCommand(@NonNull String alias, @NonNull CnlCommand command) {
    this.commands.put(alias.toLowerCase(Locale.ROOT), command);
  }

  public void interpret(@NonNull Path filePath) throws Exception {
    var lines = Files.readAllLines(filePath);
    for (var line : lines) {
      if (!line.isEmpty() && !line.startsWith("#")) {
        var parts = line.split(" ");
        Objects.checkIndex(0, parts.length);
        // get the command of the line
        var command = this.commands.get(parts[0].toLowerCase(Locale.ROOT));
        Objects.requireNonNull(command, "Cnl file at " + filePath + " used unknown cnl command " + parts[0]);
        // prepare the arguments
        Queue<String> args;
        if (parts.length == 1) {
          // no arguments provided
          args = new LinkedList<>();
        } else {
          // copy all arguments to a new list
          args = new LinkedList<>(Arrays.asList(Arrays.copyOfRange(parts, 1, parts.length)));
        }
        // execute the command
        command.execute(args);
        if (!args.isEmpty()) {
          throw new IllegalStateException(
            "Cnl command " + parts[0] + " did not consume all arguments of line " + String.join(" ", parts));
        }
      }
    }
  }
}
