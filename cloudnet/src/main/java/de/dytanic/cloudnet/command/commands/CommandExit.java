package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.language.LanguageManager;

public final class CommandExit extends CommandDefault {

    private static final long CONFIRMATION_INTERVAL = 10000;

    public CommandExit() {
        super("exit", "shutdown", "stop");
    }

    private long lastExecution = -1;

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        if (args.length != 0) {
            sender.sendMessage(LanguageManager.getMessage("command-exit-no-args"));
            return;
        }
        if (this.lastExecution == -1 || this.lastExecution + CONFIRMATION_INTERVAL < System.currentTimeMillis()) {
            sender.sendMessage(LanguageManager.getMessage("command-exit-confirm").replace("%seconds%", String.valueOf(CONFIRMATION_INTERVAL / 1000)));
            this.lastExecution = System.currentTimeMillis();
            return;
        }

        this.getCloudNet().stop();
    }
}