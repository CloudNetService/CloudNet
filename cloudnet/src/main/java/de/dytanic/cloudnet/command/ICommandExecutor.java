package de.dytanic.cloudnet.command;

import de.dytanic.cloudnet.common.Properties;

/**
 * This interface represents the handling a input commandLine as args
 */
public interface ICommandExecutor {

    /**
     * Handles an incoming commandLine which are already split and fetched for all extra properties
     *
     * @param sender      the sender, that execute the command
     * @param command     the command name, that is used for
     * @param args        all important arguments
     * @param commandLine the full commandline from the sender
     * @param properties  all properties, that are parsed from the command line
     */
    void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties);
}