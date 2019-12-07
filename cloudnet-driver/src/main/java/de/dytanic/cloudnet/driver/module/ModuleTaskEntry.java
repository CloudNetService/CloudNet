package de.dytanic.cloudnet.driver.module;

import java.lang.reflect.Method;

public interface ModuleTaskEntry {

    ModuleWrapper getModuleWrapper();

    Module getModule();

    ModuleTask getTaskInfo();

    Method getHandler();

}