package de.dytanic.cloudnet.driver.module;

public class DefaultModule implements Module {

    ModuleWrapper moduleWrapper;

    ClassLoader classLoader;

    ModuleConfiguration moduleConfig;

    public ModuleWrapper getModuleWrapper() {
        return this.moduleWrapper;
    }

    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    public ModuleConfiguration getModuleConfig() {
        return this.moduleConfig;
    }
}