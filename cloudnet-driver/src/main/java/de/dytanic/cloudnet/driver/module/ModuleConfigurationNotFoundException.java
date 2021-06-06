package de.dytanic.cloudnet.driver.module;

import java.net.URL;

public final class ModuleConfigurationNotFoundException extends Exception {

  public ModuleConfigurationNotFoundException(URL url) {
    super("module configuration not found in " + url.toString());
  }
}
