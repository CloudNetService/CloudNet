package de.dytanic.cloudnet.driver.event.events.module;

import de.dytanic.cloudnet.driver.module.ModuleProvider;
import de.dytanic.cloudnet.driver.module.ModuleWrapper;
import de.dytanic.cloudnet.driver.module.ModuleDependency;

public final class ModulePostInstallDependencyEvent extends ModuleEvent {

    private final ModuleDependency moduleDependency;

    public ModulePostInstallDependencyEvent(ModuleProvider moduleProvider, ModuleWrapper module, ModuleDependency moduleDependency) {
        super(moduleProvider, module);

        this.moduleDependency = moduleDependency;
    }

    public ModuleDependency getModuleDependency() {
        return this.moduleDependency;
    }
}