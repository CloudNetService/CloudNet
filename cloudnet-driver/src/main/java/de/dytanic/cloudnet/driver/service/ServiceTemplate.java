package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.Validate;
import lombok.Getter;

@Getter
public class ServiceTemplate implements INameable {

  private final String prefix, name, storage;

  public ServiceTemplate(String prefix, String name, String storage) {
    Validate.checkNotNull(prefix);
    Validate.checkNotNull(name);
    Validate.checkNotNull(storage);

    this.prefix = prefix;
    this.name = name;
    this.storage = storage;
  }

  public String getTemplatePath() {
    return prefix + "/" + name;
  }
}