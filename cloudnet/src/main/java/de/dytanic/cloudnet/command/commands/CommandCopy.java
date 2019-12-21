package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.service.ICloudService;

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
            sender.sendMessage("cp <local service uniqueId | name> | template=storage:prefix/name");
            return;
        }

        ServiceInfoSnapshot serviceInfoSnapshot = super.getCloudNet().getCloudServiceByNameOrUniqueId(args[0]);

        if (serviceInfoSnapshot != null) {

            ICloudService cloudService = super.getCloudNet().getCloudServiceManager().getCloudService(serviceInfoSnapshot.getServiceId().getUniqueId());

            if (cloudService != null) {

                ServiceTemplate targetTemplate;

                if (properties.containsKey("template")) {
                    targetTemplate = ServiceTemplate.parse(properties.get("template"));
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
            }
        }
    }

}
