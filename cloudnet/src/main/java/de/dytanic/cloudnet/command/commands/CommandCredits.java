package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.command.ConsoleCommandSender;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.util.CreditsUtil;

public class CommandCredits extends CommandDefault {

    public CommandCredits() {
        super("credits", "credit");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        if (sender instanceof ConsoleCommandSender) {
            CreditsUtil.printContributorImages(sender, null);
        } else {
            CreditsUtil.printContributorNames(sender, null);
        }
    }
}
