package de.dytanic.cloudnet.driver.module;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public interface IModuleWrapper {

    EnumMap<ModuleLifeCycle, List<IModuleTaskEntry>> getModuleTasks();

    IModule getModule();

    ModuleLifeCycle getModuleLifeCycle();

    IModuleProvider getModuleProvider();

    ModuleConfiguration getModuleConfiguration();

    JsonDocument getModuleConfigurationSource();

    ClassLoader getClassLoader();

    IModuleWrapper loadModule();

    IModuleWrapper startModule();

    IModuleWrapper stopModule();

    IModuleWrapper unloadModule();

    @Deprecated
    default File getDataFolder() {
        return this.getDataDirectory().toFile();
    }

    @NotNull
    Path getDataDirectory();

    Map<String, String> getDefaultRepositories();

}