package de.dytanic.cloudnet.driver.module;

import lombok.Getter;

@Getter
public class DefaultModule implements IModule {

  IModuleWrapper moduleWrapper;

  ClassLoader classLoader;

  ModuleConfiguration moduleConfig;

}