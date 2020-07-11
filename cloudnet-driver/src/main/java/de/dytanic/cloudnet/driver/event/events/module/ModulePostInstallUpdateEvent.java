package de.dytanic.cloudnet.driver.event.events.module;

import de.dytanic.cloudnet.driver.module.IModuleProvider;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.repository.RepositoryModuleInfo;

/**
 * This event is being called after CloudNet has installed the new jar of the given {@link IModuleWrapper}.
 * {@link IModuleWrapper#getModuleLifeCycle()} is still {@link ModuleLifeCycle#UNLOADED}
 */
public final class ModulePostInstallUpdateEvent extends ModuleEvent {

    private final RepositoryModuleInfo moduleInfo;

    public ModulePostInstallUpdateEvent(IModuleProvider moduleProvider, IModuleWrapper module, RepositoryModuleInfo moduleInfo) {
        super(moduleProvider, module);
        this.moduleInfo = moduleInfo;
    }

    public RepositoryModuleInfo getModuleInfo() {
        return this.moduleInfo;
    }
}