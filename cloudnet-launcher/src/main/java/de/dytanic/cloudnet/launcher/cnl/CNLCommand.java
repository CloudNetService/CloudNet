package de.dytanic.cloudnet.launcher.cnl;

import java.util.Map;

public abstract class CNLCommand {

  protected final String name;

  public CNLCommand(String name) {
    this.name = name;
  }

  public abstract void execute(Map<String, String> variables, String commandLine, String... args) throws Exception;

  public String getName() {
    return this.name;
  }
}
