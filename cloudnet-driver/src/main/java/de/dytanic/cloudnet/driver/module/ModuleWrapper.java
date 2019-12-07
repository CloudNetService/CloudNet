package de.dytanic.cloudnet.driver.module;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;

import java.io.File;
import java.util.EnumMap;
import java.util.List;

public interface ModuleWrapper {

    EnumMap<ModuleLifeCycle, List<ModuleTaskEntry>> getModuleTasks();

    Module getModule();

    ModuleLifeCycle getModuleLifeCycle();

    ModuleProvider getModuleProvider();

    ModuleConfiguration getModuleConfiguration();

    JsonDocument getModuleConfigurationSource();

    ClassLoader getClassLoader();

    ModuleWrapper loadModule();

    ModuleWrapper startModule();

    ModuleWrapper stopModule();

    ModuleWrapper unloadModule();

    File getDataFolder();
}