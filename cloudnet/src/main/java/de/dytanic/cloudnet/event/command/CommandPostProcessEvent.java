package de.dytanic.cloudnet.event.command;

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.driver.event.Event;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CommandPostProcessEvent extends Event {

    private final String commandLine;

    private final ICommandSender commandSender;

}