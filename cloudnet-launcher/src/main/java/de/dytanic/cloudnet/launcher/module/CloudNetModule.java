package de.dytanic.cloudnet.launcher.module;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public final class CloudNetModule {

  protected final String name;

  protected final String fileName;

  public CloudNetModule(String name, String fileName) {
    this.name = name;
    this.fileName = fileName;
  }

  public String getName() {
    return this.name;
  }

  public String getFileName() {
    return this.fileName;
  }

}
