package de.dytanic.cloudnet.driver.event.events.module;

import de.dytanic.cloudnet.driver.module.IModuleProvider;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.dependency.ModuleDependency;

/**
 * This event is being called before a dependency for a module is being downloaded.
 * {@link IModuleWrapper#getModuleLifeCycle()} is still {@link ModuleLifeCycle#UNLOADED}
 */
public final class ModulePreInstallDependencyEvent extends ModuleEvent {

    private final ModuleDependency moduleDependency;

    public ModulePreInstallDependencyEvent(IModuleProvider moduleProvider, IModuleWrapper module, ModuleDependency moduleDependency) {
        super(moduleProvider, module);

        this.moduleDependency = moduleDependency;
    }

    public ModuleDependency getModuleDependency() {
        return this.moduleDependency;
    }
}