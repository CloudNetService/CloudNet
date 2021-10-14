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

package de.dytanic.cloudnet.launcher.cnl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CNLInterpreter {

  private static final Map<String, CNLCommand> COMMANDS = new HashMap<>();

  public static void registerCommand(CNLCommand command) {
    COMMANDS.put(command.name.toLowerCase(), command);
  }

  public static void clearCommands() {
    COMMANDS.clear();
  }

  public static void runInterpreter(Path path) throws IOException, CNLCommandExecuteException {
    runInterpreter(path, new HashMap<>());
  }

  public static void runInterpreter(Path path, Map<String, String> variables)
    throws IOException, CNLCommandExecuteException {
    try (InputStream stream = Files.newInputStream(path)) {
      runInterpreter(stream, variables);
    }
  }

  @Deprecated
  public static void runInterpreter(File file) throws IOException, CNLCommandExecuteException {
    runInterpreter(file.toPath());
  }

  @Deprecated
  public static void runInterpreter(File file, Map<String, String> variables)
    throws IOException, CNLCommandExecuteException {
    runInterpreter(file.toPath(), variables);
  }

  public static void runInterpreter(InputStream inputStream) throws IOException, CNLCommandExecuteException {
    runInterpreter(inputStream, new HashMap<>());
  }

  public static void runInterpreter(InputStream inputStream, Map<String, String> variables)
    throws IOException, CNLCommandExecuteException {
    try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
      runInterpreter(inputStreamReader, variables);
    }
  }

  public static void runInterpreter(InputStreamReader inputStreamReader)
    throws IOException, CNLCommandExecuteException {
    runInterpreter(inputStreamReader, new HashMap<>());
  }

  public static void runInterpreter(InputStreamReader inputStreamReader, Map<String, String> variables)
    throws IOException, CNLCommandExecuteException {
    try (BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
      String commandLine;
      while ((commandLine = bufferedReader.readLine()) != null) {
        runCommand(variables, commandLine);
      }
    }
  }

  public static void runCommand(Map<String, String> variables, String commandLine) throws CNLCommandExecuteException {
    if (commandLine.startsWith(" ") || commandLine.startsWith("#") || commandLine.trim().isEmpty()) {
      return;
    }

    String[] args = commandLine.split(" ");
    String name = args[0];

    if (COMMANDS.containsKey(name.toLowerCase())) {
      CNLCommand command = COMMANDS.get(name.toLowerCase());

      if (args.length == 1) {
        args = new String[0];
      } else {
        List<String> list = Arrays.asList(commandLine.replaceFirst(name + " ", "").split(" "));
        list.replaceAll(text -> {

          for (Map.Entry<String, String> entry : variables.entrySet()) {
            if (text.contains("$" + entry.getKey())) {
              text = text.replace("$" + entry.getKey(), entry.getValue());
            }
          }

          return text;
        });

        args = list.toArray(new String[0]);
      }

      try {
        command.execute(variables, commandLine, args);
      } catch (Exception ex) {
        throw new CNLCommandExecuteException(ex);
      }
    }
  }
}
