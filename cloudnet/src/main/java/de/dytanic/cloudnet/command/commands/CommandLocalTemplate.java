package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.LocalTemplateStorage;
import de.dytanic.cloudnet.template.LocalTemplateStorageUtil;
import de.dytanic.cloudnet.util.InstallableAppVersion;

import java.util.Arrays;

public final class CommandLocalTemplate extends CommandDefault {

    public CommandLocalTemplate() {
        super("local-template", "localt", "lt");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        if (args.length == 0) {
            sender.sendMessage(
                    "lt list | prefix=<name> name=<name>",
                    "lt install <" + Arrays.toString(ServiceEnvironmentType.values()) + ">",
                    "lt install <prefix> <name> <" + Arrays.toString(ServiceEnvironmentType.values()) + "> <version>",
                    "lt delete <prefix> <name>",
                    "lt create <prefix> <name> <" + Arrays.toString(ServiceEnvironmentType.values()) + ">"
            );
            return;
        }

        ITemplateStorage storage = getCloudNet().getServicesRegistry().getService(ITemplateStorage.class, LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE);

        if (storage == null) {
            throw new UnsupportedOperationException("Storage cannot be found!");
        }

        if (args[0].equalsIgnoreCase("list")) {
            for (ServiceTemplate serviceTemplate : storage.getTemplates()) {
                if (properties.containsKey("prefix") && serviceTemplate.getPrefix().toLowerCase().contains(properties.get("prefix"))) {
                    continue;
                }

                if (properties.containsKey("name") && serviceTemplate.getName().toLowerCase().contains(properties.get("name"))) {
                    continue;
                }

                this.displayTemplate(sender, serviceTemplate);
            }
        }

        if (args[0].equalsIgnoreCase("install")) {
            if (args.length == 2) {
                try {
                    ServiceEnvironmentType environmentType = ServiceEnvironmentType.valueOf(args[1].toUpperCase());

                    sender.sendMessage("ServiceType: " + environmentType);

                    for (InstallableAppVersion installableAppVersion : InstallableAppVersion.VERSIONS) {
                        if (installableAppVersion.getServiceEnvironment() == environmentType) {
                            sender.sendMessage("- " + installableAppVersion.getVersion() + " * Environment: " + installableAppVersion.getEnvironmentType());
                        }
                    }

                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                return;
            }

            if (args.length == 5) {
                ServiceTemplate serviceTemplate = new ServiceTemplate(args[1], args[2], LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE);

                try {
                    InstallableAppVersion installableAppVersion = InstallableAppVersion.getVersion(ServiceEnvironmentType.valueOf(args[3].toUpperCase()), args[4]);
                    if (installableAppVersion != null) {
                        sender.sendMessage(LanguageManager.getMessage("command-local-template-install-try")
                                .replace("%environment%", installableAppVersion.getServiceEnvironment().name())
                                .replace("%version%", installableAppVersion.getVersion())
                        );
                        if (LocalTemplateStorageUtil.installApplicationJar(storage, serviceTemplate, installableAppVersion)) {
                            sender.sendMessage(LanguageManager.getMessage("command-local-template-install-success")
                                    .replace("%environment%", installableAppVersion.getServiceEnvironment().name())
                                    .replace("%version%", installableAppVersion.getVersion())
                            );
                        } else {
                            sender.sendMessage(LanguageManager.getMessage("command-local-template-install-failed")
                                    .replace("%environment%", installableAppVersion.getServiceEnvironment().name())
                                    .replace("%version%", installableAppVersion.getVersion())
                            );
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }

                getCloudNet().deployTemplateInCluster(serviceTemplate, storage.toZipByteArray(serviceTemplate));
            }
        }

        if (args[0].equalsIgnoreCase("delete") && args.length == 3) {
            storage.delete(new ServiceTemplate(args[1], args[2], LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE));
            sender.sendMessage(LanguageManager.getMessage("command-local-template-delete-template-success"));
        }

        if (args[0].equalsIgnoreCase("create") && args.length == 4) {
            try {
                ServiceEnvironmentType environment = ServiceEnvironmentType.valueOf(args[3].toUpperCase());

                if (LocalTemplateStorageUtil.createAndPrepareTemplate(storage, args[1], args[2], environment)) {
                    sender.sendMessage(LanguageManager.getMessage("command-local-template-create-template-success"));
                }

            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    private void displayTemplate(ICommandSender sender, ServiceTemplate serviceTemplate) {
        sender.sendMessage("- " + serviceTemplate.getStorage() + ":" + serviceTemplate.getPrefix() + "/" + serviceTemplate.getName());
    }

}