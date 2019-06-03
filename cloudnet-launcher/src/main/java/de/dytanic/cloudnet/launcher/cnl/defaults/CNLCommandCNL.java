package de.dytanic.cloudnet.launcher.cnl.defaults;

import de.dytanic.cloudnet.launcher.cnl.CNLCommand;
import de.dytanic.cloudnet.launcher.cnl.CNLInterpreter;
import java.io.File;
import java.util.Map;

public final class CNLCommandCNL extends CNLCommand {

  public CNLCommandCNL() {
    super("cnl");
  }


  @Override
  public void execute(Map<String, String> variables, String commandLine,
      String... args) throws Exception {
    if (args.length > 0) {

      StringBuilder stringBuilder = new StringBuilder();
      for (int i = 1; i < args.length;
          stringBuilder.append(args[i++]).append(" ")) {
        ;
      }

      CNLInterpreter.runInterpreter(
          new File(stringBuilder.substring(0, stringBuilder.length() - 1)),
          variables);
    }
  }
}