package de.dytanic.cloudnet.driver.event.events.module;

import de.dytanic.cloudnet.driver.module.IModuleProvider;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.module.ModuleDependency;
import lombok.Getter;

@Getter
public final class ModulePreInstallDependencyEvent extends ModuleEvent {

    private final ModuleDependency moduleDependency;

    public ModulePreInstallDependencyEvent(IModuleProvider moduleProvider, IModuleWrapper module, ModuleDependency moduleDependency)
    {
        super(moduleProvider, module);

        this.moduleDependency = moduleDependency;
    }
}