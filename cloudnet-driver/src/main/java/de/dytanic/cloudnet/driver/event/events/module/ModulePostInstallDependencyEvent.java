package de.dytanic.cloudnet.driver.event.events.module;

import de.dytanic.cloudnet.driver.module.IModuleProvider;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.module.ModuleDependency;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;

/**
 * This event is being called after a dependency for a module has been downloaded.
 * {@link IModuleWrapper#getModuleLifeCycle()} is still {@link ModuleLifeCycle#UNLOADED}
 */
public final class ModulePostInstallDependencyEvent extends ModuleEvent {

    private final ModuleDependency moduleDependency;

    public ModulePostInstallDependencyEvent(IModuleProvider moduleProvider, IModuleWrapper module, ModuleDependency moduleDependency) {
        super(moduleProvider, module);

        this.moduleDependency = moduleDependency;
    }

    public ModuleDependency getModuleDependency() {
        return this.moduleDependency;
    }
}