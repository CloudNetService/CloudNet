package de.dytanic.cloudnet.command.sub;

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;

import java.util.Map;

public interface SubCommandExecutor {

    void execute(ICommandSender sender, String command, Object[] args, String commandLine, Properties properties, Map<String, Object> internalProperties);

}
