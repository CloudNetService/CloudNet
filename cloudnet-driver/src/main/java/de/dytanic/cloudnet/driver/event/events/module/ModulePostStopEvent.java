package de.dytanic.cloudnet.driver.event.events.module;

import de.dytanic.cloudnet.driver.module.IModuleProvider;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;

public final class ModulePostStopEvent extends ModuleEvent {

    public ModulePostStopEvent(IModuleProvider moduleProvider, IModuleWrapper module)
    {
        super(moduleProvider, module);
    }
}