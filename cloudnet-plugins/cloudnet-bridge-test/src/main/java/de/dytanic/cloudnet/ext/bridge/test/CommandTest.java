package de.dytanic.cloudnet.ext.bridge.test;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

final class CommandTest implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!command.testPermission(commandSender)) {
            return false;
        }

        switch (args.length) {
            case 1: {
                if (args[0].equalsIgnoreCase("test1")) {
                    int counter = 0;
                    for (ServiceInfoSnapshot serviceInfoSnapshot : CloudNetDriver.getInstance().getCloudServices()) {
                        commandSender.sendMessage(counter++ + ": " + serviceInfoSnapshot.getServiceId().getName());

                        ServiceInfoSnapshot snapshot = CloudNetDriver.getInstance().getCloudServiceByName(serviceInfoSnapshot.getServiceId().getName());
                        commandSender.sendMessage("Snapshot by Name: " + serviceInfoSnapshot.getServiceId().getName() +
                                " : " + (snapshot != null ? snapshot.getServiceId().getName() : "null"));
                    }

                    counter = 0;
                    for (GroupConfiguration groupConfiguration : CloudNetDriver.getInstance().getGroupConfigurations()) {
                        commandSender.sendMessage(counter++ + ": " + groupConfiguration.getName());
                    }

                    return true;
                }

                if (args[0].equalsIgnoreCase("test2")) {
                    ServiceInfoSnapshot serviceInfoSnapshot = CloudNetDriver.getInstance().createCloudService(CloudNetDriver.getInstance().getServiceTask(
                            Wrapper.getInstance().getServiceConfiguration().getServiceId().getTaskName()
                    ));

                    commandSender.sendMessage("New Service -> " + serviceInfoSnapshot.getServiceId().getName());

                    CloudNetDriver.getInstance().startCloudService(serviceInfoSnapshot);
                    return true;
                }

                if (args[0].equalsIgnoreCase("test3")) {
                    ServiceInfoSnapshot serviceInfoSnapshot = CloudNetDriver.getInstance().getCloudServiceByName("Lobby-54632");

                    if (serviceInfoSnapshot != null) {
                        commandSender.sendMessage("ServiceInfoSnapshot exist");
                    } else {
                        commandSender.sendMessage("ServiceInfoSnapshot doesn't exist");
                    }
                }
            }
            break;
        }

        return true;
    }
}