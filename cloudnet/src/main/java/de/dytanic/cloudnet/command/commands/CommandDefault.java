package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.common.language.LanguageManager;

abstract class CommandDefault extends Command {

    protected CommandDefault(String... names) {
        this.names = names;
        this.prefix = "cloudnet";
        this.permission = "cloudnet.command." + names[0];
        this.description = LanguageManager.getMessage("command-description-" + names[0]);
        this.usage = names[0];
    }

    protected final CloudNet getCloudNet() {
        return CloudNet.getInstance();
    }
}