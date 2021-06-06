package de.dytanic.cloudnet.launcher.cnl.defaults;

import de.dytanic.cloudnet.launcher.cnl.CNLCommand;
import java.util.Map;

public final class CNLCommandEcho extends CNLCommand {

  public CNLCommandEcho() {
    super("echo");
  }

  @Override
  public void execute(Map<String, String> variables, String commandLine, String... args) {
    System.out.println(String.join(" ", args));
  }
}
