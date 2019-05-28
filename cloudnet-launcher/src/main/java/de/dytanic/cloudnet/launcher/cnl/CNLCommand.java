package de.dytanic.cloudnet.launcher.cnl;

import java.util.Map;
import lombok.Getter;

@Getter
public abstract class CNLCommand {

  protected String name;

  public CNLCommand(String name) {
    this.name = name;
  }

  public abstract void execute(Map<String, String> variables,
      String commandLine, String... args) throws Exception;
}