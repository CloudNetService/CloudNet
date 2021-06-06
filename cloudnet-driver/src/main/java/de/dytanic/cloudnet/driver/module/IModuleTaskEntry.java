package de.dytanic.cloudnet.driver.module;

import java.lang.reflect.Method;

public interface IModuleTaskEntry {

  IModuleWrapper getModuleWrapper();

  IModule getModule();

  ModuleTask getTaskInfo();

  Method getHandler();

}
