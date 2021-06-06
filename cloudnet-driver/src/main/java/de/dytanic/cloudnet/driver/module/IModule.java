package de.dytanic.cloudnet.driver.module;

public interface IModule {

  IModuleWrapper getModuleWrapper();

  ClassLoader getClassLoader();

  ModuleConfiguration getModuleConfig();

  default String getGroup() {
    return this.getModuleConfig().group;
  }

  default String getName() {
    return this.getModuleConfig().name;
  }

  default String getVersion() {
    return this.getModuleConfig().version;
  }

  default String getWebsite() {
    return this.getModuleConfig().website;
  }

  default String getDescription() {
    return this.getModuleConfig().description;
  }

}
