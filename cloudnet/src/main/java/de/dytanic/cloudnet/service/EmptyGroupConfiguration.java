package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import java.util.ArrayList;

public final class EmptyGroupConfiguration extends GroupConfiguration {

  public EmptyGroupConfiguration(String name) {
    super(name);

    super.includes = new ArrayList<>();
    super.templates = new ArrayList<>();
    super.deployments = new ArrayList<>();
  }

}
