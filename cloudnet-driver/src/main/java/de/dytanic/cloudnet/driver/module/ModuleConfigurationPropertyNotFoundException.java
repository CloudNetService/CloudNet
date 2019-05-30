package de.dytanic.cloudnet.driver.module;

public final class ModuleConfigurationPropertyNotFoundException extends
  Exception {

  public ModuleConfigurationPropertyNotFoundException(String field) {
    super("Required field not found: " + field);
  }
}