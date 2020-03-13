package de.dytanic.cloudnet.driver.event.events.module;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.module.IModuleProvider;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;

public final class ModulePreStopEvent extends ModuleEvent implements ICancelable {

    public ModulePreStopEvent(IModuleProvider moduleProvider, IModuleWrapper module) {
        super(moduleProvider, module);
    }

    private boolean cancelled = false;

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean value) {
        this.cancelled = value;
    }
}