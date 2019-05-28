package de.dytanic.cloudnet.launcher.cnl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public class CNLInterpreter {

  private static final Map<String, CNLCommand> COMMANDS = new HashMap<>();

  public static void registerCommand(CNLCommand command) {
    COMMANDS.put(command.name.toLowerCase(), command);
  }

  public static void clearCommands() {
    COMMANDS.clear();
  }


  public static void runInterpreter(Path path) throws Exception {
    runInterpreter(path, new HashMap<>());
  }


  public static void runInterpreter(Path path, Map<String, String> variables)
    throws Exception {
    runInterpreter(path.toFile(), variables);
  }


  public static void runInterpreter(File file) throws Exception {
    runInterpreter(file, new HashMap<>());
  }


  public static void runInterpreter(File file, Map<String, String> variables)
    throws Exception {
    try (InputStream inputStream = new FileInputStream(file)) {
      runInterpreter(inputStream, variables);
    }
  }


  public static void runInterpreter(InputStream inputStream) throws Exception {
    runInterpreter(inputStream, new HashMap<>());
  }


  public static void runInterpreter(InputStream inputStream,
    Map<String, String> variables) throws Exception {
    try (InputStreamReader inputStreamReader = new InputStreamReader(
      inputStream, StandardCharsets.UTF_8)) {
      runInterpreter(inputStreamReader, variables);
    }
  }


  public static void runInterpreter(InputStreamReader inputStreamReader)
    throws Exception {
    runInterpreter(inputStreamReader, new HashMap<>());
  }


  public static void runInterpreter(InputStreamReader inputStreamReader,
    Map<String, String> variables) throws Exception {

    try (BufferedReader bufferedReader = new BufferedReader(
      inputStreamReader)) {

      String commandLine;
      while ((commandLine = bufferedReader.readLine()) != null) {
        runCommand(variables, commandLine);
      }
    }
  }


  public static void runCommand(Map<String, String> variables,
    String commandLine) throws CNLCommandExecuteException {
    if (commandLine.startsWith(" ") || commandLine.startsWith("#")
      || commandLine.trim().isEmpty()) {
      return;
    }

    String[] args = commandLine.split(" ");

    String name = args[0];

    if (COMMANDS.containsKey(name.toLowerCase())) {
      CNLCommand command = COMMANDS.get(name.toLowerCase());

      if (args.length == 1) {
        args = new String[0];
      } else {
        List<String> list = Arrays
          .asList(commandLine.replaceFirst(name + " ", "").split(" "));
        list.replaceAll(new UnaryOperator<String>() {

          @Override
          public String apply(String text) {

            for (String variable : variables.keySet()) {
              if (text.contains("$" + variable)) {
                text = text.replace("$" + variable, variables.get(variable));
              }
            }

            return text;
          }
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