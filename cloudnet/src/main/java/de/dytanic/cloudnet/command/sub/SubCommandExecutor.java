package de.dytanic.cloudnet.command.sub;

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;

import java.util.Map;

public interface SubCommandExecutor {

    void execute(SubCommand subCommand, ICommandSender sender, String command, SubCommandArgumentWrapper args, String commandLine, Properties properties, Map<String, Object> internalProperties);

}
