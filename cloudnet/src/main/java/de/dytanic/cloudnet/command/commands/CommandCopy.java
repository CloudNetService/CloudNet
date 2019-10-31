package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandCopy extends CommandDefault {

    public CommandCopy() {
        super("copy", "cp");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        if (args.length == 0) {
            sender.sendMessage("cp <service> | template=storage:prefix/name");
            return;
        }

        CloudNet.getInstance().getCloudServiceManager().getCloudServices().values().stream()
                .filter(serviceInfoSnapshot ->
                        serviceInfoSnapshot.getServiceId().getName().equalsIgnoreCase(args[0]) ||
                                serviceInfoSnapshot.getServiceId().getUniqueId().toString().equals(args[0])
                ).findFirst().ifPresent(cloudService -> {

            ServiceTemplate targetTemplate = null;

            if (properties.containsKey("template")) {
                String[] base = properties.get("template").split(":");

                if (base.length == 2) {
                    String[] path = base[1].split("/");
                    if (path.length == 2) {
                        String storage = base[0];
                        String prefix = path[0];
                        String name = path[1];

                        targetTemplate = new ServiceTemplate(prefix, name, storage);
                    }
                }
            } else {
                targetTemplate = cloudService.getTemplates()
                        .stream()
                        .filter(serviceTemplate -> serviceTemplate.getPrefix().equalsIgnoreCase(cloudService.getServiceId().getTaskName())
                                && serviceTemplate.getName().equalsIgnoreCase("default"))
                        .findFirst().orElse(null);
            }

            if (targetTemplate == null) {
                sender.sendMessage(LanguageManager.getMessage("command-copy-service-no-default-template").replace("%name%", cloudService.getServiceId().getName()));
                return;
            }

            List<ServiceDeployment> oldDeployments = new ArrayList<>(cloudService.getDeployments());
            cloudService.getDeployments().clear();

            cloudService.getDeployments().add(new ServiceDeployment(targetTemplate, Collections.emptyList()));
            cloudService.deployResources();
            cloudService.getDeployments().clear();

            cloudService.getDeployments().addAll(oldDeployments);

            sender.sendMessage(
                    LanguageManager.getMessage("command-copy-success")
                            .replace("%name%", cloudService.getServiceId().getName())
                            .replace("%template%", targetTemplate.getStorage() + ":" + targetTemplate.getPrefix() + "/" + targetTemplate.getName())
            );
        });
    }

}
