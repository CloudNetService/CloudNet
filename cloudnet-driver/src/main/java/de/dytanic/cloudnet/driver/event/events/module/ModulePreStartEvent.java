package de.dytanic.cloudnet.driver.event.events.module;

import de.dytanic.cloudnet.driver.module.ModuleProvider;
import de.dytanic.cloudnet.driver.module.ModuleWrapper;

public final class ModulePreStartEvent extends ModuleEvent {

    public ModulePreStartEvent(ModuleProvider moduleProvider, ModuleWrapper module) {
        super(moduleProvider, module);
    }
}