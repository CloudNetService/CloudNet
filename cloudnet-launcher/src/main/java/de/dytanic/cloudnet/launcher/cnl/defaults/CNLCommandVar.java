package de.dytanic.cloudnet.launcher.cnl.defaults;

import de.dytanic.cloudnet.launcher.cnl.CNLCommand;
import java.util.Map;

public final class CNLCommandVar extends CNLCommand {

  public CNLCommandVar() {
    super("var");
  }


  @Override
  public void execute(Map<String, String> variables, String commandLine,
      String... args) {

    if (args.length > 1) {
      StringBuilder stringBuilder = new StringBuilder();

      for (int i = 1; i < args.length;
          stringBuilder.append(args[i++]).append(" ")) {
        ;
      }

      variables
          .put(args[0], stringBuilder.substring(0, stringBuilder.length() - 1));
    }
  }
}