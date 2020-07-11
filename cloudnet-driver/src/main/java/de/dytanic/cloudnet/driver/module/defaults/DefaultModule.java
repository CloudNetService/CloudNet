package de.dytanic.cloudnet.driver.module.defaults;

import de.dytanic.cloudnet.driver.module.IModule;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.module.ModuleConfiguration;

public class DefaultModule implements IModule {

    IModuleWrapper moduleWrapper;

    ClassLoader classLoader;

    ModuleConfiguration moduleConfig;

    public IModuleWrapper getModuleWrapper() {
        return this.moduleWrapper;
    }

    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    public ModuleConfiguration getModuleConfig() {
        return this.moduleConfig;
    }
}