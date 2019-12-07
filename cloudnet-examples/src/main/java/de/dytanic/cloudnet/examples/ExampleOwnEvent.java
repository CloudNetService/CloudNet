package de.dytanic.cloudnet.examples;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.module.ModuleWrapper;

//Generates a constructor with the moduleWrapper as parameter
public final class ExampleOwnEvent extends Event { //Create a own event based of the Event class

    private final ModuleWrapper moduleWrapper;

    public ExampleOwnEvent(ModuleWrapper moduleWrapper) {
        this.moduleWrapper = moduleWrapper;
    }

    public ModuleWrapper getModuleWrapper() {
        return this.moduleWrapper;
    }
}