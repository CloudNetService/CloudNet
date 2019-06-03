package de.dytanic.cloudnet.examples;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor //Generates a constructor with the moduleWrapper as parameter
public final class ExampleOwnEvent extends Event { //Create a own event based of the Event class

    private final IModuleWrapper moduleWrapper;

}