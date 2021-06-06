package de.dytanic.cloudnet.driver.event.events.module;

import de.dytanic.cloudnet.driver.module.IModuleProvider;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;

/**
 * This event is being called after a module has been started and the tasks with the lifecycle {@link
 * ModuleLifeCycle#STARTED} of this module have been fired. {@link IModuleWrapper#getModuleLifeCycle()} is still {@link
 * ModuleLifeCycle#LOADED} or {@link ModuleLifeCycle#STOPPED}.
 */
public final class ModulePostStartEvent extends ModuleEvent {

  public ModulePostStartEvent(IModuleProvider moduleProvider, IModuleWrapper module) {
    super(moduleProvider, module);
  }
}
