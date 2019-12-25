package de.dytanic.cloudnet.examples.node;

import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;

public final class ExampleCommand extends Command {

    public ExampleCommand() {
        super("example", "exm");

        this.permission = "cloudnet.command.example";
        this.usage = "example <test>";
        this.prefix = "cloudnet-example-module";
        this.description = "This is an example command";
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        if (args.length == 0) {
            sender.sendMessage("example <test>");
            return;
        }

        //exm test-1 get=my_argument
        if (args[0].equalsIgnoreCase("test-1")) {
            sender.sendMessage("Starting Test-1 with the following" + (properties.containsKey("get") ? " default argument: " + properties.get("get") : " no arguments"));
            sender.sendMessage(
                    "Argument is " + properties.get("get")
            );
        }
    }
}