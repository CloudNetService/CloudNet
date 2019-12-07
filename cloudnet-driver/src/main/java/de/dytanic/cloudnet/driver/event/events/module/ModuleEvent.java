package de.dytanic.cloudnet.driver.event.events.module;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.driver.module.ModuleProvider;
import de.dytanic.cloudnet.driver.module.ModuleWrapper;

public abstract class ModuleEvent extends DriverEvent {

    private ModuleProvider moduleProvider;

    private ModuleWrapper module;

    public ModuleEvent(ModuleProvider moduleProvider, ModuleWrapper module) {
        this.moduleProvider = moduleProvider;
        this.module = module;
    }

    public ModuleProvider getModuleProvider() {
        return this.moduleProvider;
    }

    public ModuleWrapper getModule() {
        return this.module;
    }
}