package de.dytanic.cloudnet.driver.event.events.module;

import de.dytanic.cloudnet.driver.module.ModuleProvider;
import de.dytanic.cloudnet.driver.module.ModuleWrapper;

public final class ModulePostUnloadEvent extends ModuleEvent {

    public ModulePostUnloadEvent(ModuleProvider moduleProvider, ModuleWrapper module) {
        super(moduleProvider, module);
    }
}