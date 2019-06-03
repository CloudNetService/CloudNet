package de.dytanic.cloudnet.event.command;

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.ICancelable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public class CommandPreProcessEvent extends Event implements ICancelable {

    private final String commandLine;

    private final ICommandSender commandSender;

    @Setter
    private boolean cancelled = false;
}