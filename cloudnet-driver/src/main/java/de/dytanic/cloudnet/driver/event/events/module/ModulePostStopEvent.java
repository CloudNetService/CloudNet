package de.dytanic.cloudnet.driver.event.events.module;

import de.dytanic.cloudnet.driver.module.IModuleProvider;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;

/**
 * This event is being called after a module has been stopped and the tasks with the lifecycle {@link ModuleLifeCycle#STOPPED} of this module have been fired.
 * {@link IModuleWrapper#getModuleLifeCycle()} is still {@link ModuleLifeCycle#STARTED} or {@link ModuleLifeCycle#LOADED}.
 */
public final class ModulePostStopEvent extends ModuleEvent {

    public ModulePostStopEvent(IModuleProvider moduleProvider, IModuleWrapper module) {
        super(moduleProvider, module);
    }
}