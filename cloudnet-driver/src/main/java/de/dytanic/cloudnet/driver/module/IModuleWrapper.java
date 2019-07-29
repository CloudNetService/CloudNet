package de.dytanic.cloudnet.driver.module;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;

import java.io.File;
import java.util.EnumMap;
import java.util.List;

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


    default File getDataFolder() {
        return this.getModuleConfigurationSource() != null && this.getModuleConfigurationSource().contains("dataFolder") ?
                new File(this.getModuleConfigurationSource().getString("dataFolder"))
                :
                new File("modules/" + this.getModuleConfiguration().getName()
                );
    }
}