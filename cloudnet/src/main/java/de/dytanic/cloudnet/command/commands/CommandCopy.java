package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;

import java.util.*;
import java.util.stream.Collectors;

public class CommandCopy extends CommandDefault implements ITabCompleter {

    public CommandCopy() {
        super("copy", "cp");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        if (args.length == 0) {
            sender.sendMessage("cp <local service uniqueId | name> [excludes: spigot.jar;logs;plugins] | template=storage:prefix/name");
            return;
        }

        ServiceInfoSnapshot serviceInfoSnapshot = super.getCloudNet().getCloudServiceByNameOrUniqueId(args[0]);

        if (serviceInfoSnapshot != null) {
            SpecificCloudServiceProvider cloudServiceProvider = serviceInfoSnapshot.provider();
            ServiceTemplate targetTemplate;

            if (properties.containsKey("template")) {
                targetTemplate = ServiceTemplate.parse(properties.get("template"));
            } else {
                targetTemplate = Arrays.stream(serviceInfoSnapshot.getConfiguration().getTemplates())
                        .filter(serviceTemplate -> serviceTemplate.getPrefix().equalsIgnoreCase(serviceInfoSnapshot.getServiceId().getTaskName())
                                && serviceTemplate.getName().equalsIgnoreCase("default"))
                        .findFirst().orElse(null);
            }

            if (targetTemplate == null) {
                sender.sendMessage(LanguageManager.getMessage("command-copy-service-no-default-template").replace("%name%", serviceInfoSnapshot.getServiceId().getName()));
                return;
            }

            List<ServiceDeployment> oldDeployments = new ArrayList<>(Arrays.asList(serviceInfoSnapshot.getConfiguration().getDeployments()));

            List<String> excludes = args.length == 2 ? Arrays.asList(args[1].split(";")) : Collections.emptyList();

            cloudServiceProvider.addServiceDeployment(new ServiceDeployment(targetTemplate, excludes));
            cloudServiceProvider.deployResources(true);

            oldDeployments.forEach(cloudServiceProvider::addServiceDeployment);

            sender.sendMessage(
                    LanguageManager.getMessage("command-copy-success")
                            .replace("%name%", serviceInfoSnapshot.getServiceId().getName())
                            .replace("%template%", targetTemplate.getStorage() + ":" + targetTemplate.getPrefix() + "/" + targetTemplate.getName())
            );

        }
    }

    @Override
    public Collection<String> complete(String commandLine, String[] args, Properties properties) {
        if (args.length <= 1) {
            return super.getCloudNet().getCloudServiceProvider().getCloudServices().stream().map(ServiceInfoSnapshot::getName).collect(Collectors.toList());
        }
        return super.getCloudNet().getLocalTemplateStorageTemplates().stream().map(ServiceTemplate::getTemplatePath).map(path -> "template=" + path).collect(Collectors.toList());
    }
}
