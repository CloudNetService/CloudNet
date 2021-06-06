package de.dytanic.cloudnet.driver.event.events.module;

import de.dytanic.cloudnet.driver.module.IModuleProvider;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;

/**
 * This event is being called after a module has been unloaded and the tasks with the lifecycle {@link
 * ModuleLifeCycle#UNLOADED} of this module have been fired. {@link IModuleWrapper#getModuleLifeCycle()} is either
 * {@link ModuleLifeCycle#UNLOADED} or {@link ModuleLifeCycle#STOPPED}
 */
public final class ModulePreUnloadEvent extends ModuleEvent {

  public ModulePreUnloadEvent(IModuleProvider moduleProvider, IModuleWrapper module) {
    super(moduleProvider, module);
  }
}
