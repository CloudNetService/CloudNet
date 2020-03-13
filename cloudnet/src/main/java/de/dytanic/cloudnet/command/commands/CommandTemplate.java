package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.JavaVersion;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeEnum;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.TemplateStorageUtil;
import de.dytanic.cloudnet.template.install.ServiceVersion;
import de.dytanic.cloudnet.template.install.ServiceVersionType;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.*;

public class CommandTemplate extends SubCommandHandler {
    public CommandTemplate() {
        super(
                SubCommandBuilder.create()

                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                                    String storageName = (String) args.argument("storage").orElse("local");
                                    ITemplateStorage storage = CloudNet.getInstance().getServicesRegistry().getService(ITemplateStorage.class, storageName);

                                    List<String> messages = new ArrayList<>();
                                    messages.add(LanguageManager.getMessage("command-template-list-templates").replace("%storage%", storageName));

                                    for (ServiceTemplate template : storage.getTemplates()) {
                                        messages.add("  " + template.getTemplatePath());
                                    }

                                    sender.sendMessage(messages.toArray(new String[0]));
                                },
                                subCommand -> subCommand.setMinArgs(subCommand.getRequiredArguments().length - 1).setMaxArgs(subCommand.getRequiredArguments().length),
                                exactStringIgnoreCase("list"),
                                dynamicString(
                                        "storage",
                                        LanguageManager.getMessage("command-template-storage-not-found"),
                                        input -> CloudNet.getInstance().getServicesRegistry().containsService(ITemplateStorage.class, input)
                                )
                        )

                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                                    List<String> messages = new ArrayList<>();
                                    messages.add(LanguageManager.getMessage("command-template-list-versions"));

                                    for (ServiceVersionType versionType : CloudNet.getInstance().getServiceVersionProvider().getServiceVersionTypes().values()) {
                                        messages.add("  " + versionType.getName() + ":");

                                        for (ServiceVersion version : versionType.getVersions()) {
                                            messages.add("    " + version.getName());
                                        }
                                    }

                                    sender.sendMessage(messages.toArray(new String[0]));
                                },
                                exactStringIgnoreCase("versions")
                        )

                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {

                                    ServiceTemplate template = (ServiceTemplate) args.argument("template").get();
                                    ITemplateStorage storage = CloudNet.getInstance().getServicesRegistry().getService(ITemplateStorage.class, template.getStorage());
                                    ServiceVersionType versionType = CloudNet.getInstance().getServiceVersionProvider().getServiceVersionType((String) args.argument("versionType").get()).get();

                                    Optional<ServiceVersion> optionalVersion = versionType.getVersion((String) args.argument("version").get());

                                    if (!optionalVersion.isPresent()) { //the version might be available, but not for the given version type
                                        sender.sendMessage(LanguageManager.getMessage("command-template-invalid-version"));
                                        return;
                                    }

                                    ServiceVersion version = optionalVersion.get();

                                    if (!versionType.getInstallerType().canInstall(version)) {
                                        sender.sendMessage(LanguageManager.getMessage("command-template-install-wrong-java")
                                                .replace("%version%", versionType.getName() + "-" + version.getName())
                                                .replace("%java%", JavaVersion.getRuntimeVersion().getName())
                                        );
                                        return;
                                    }

                                    CloudNet.getInstance().scheduleTask(() -> {
                                        sender.sendMessage(LanguageManager.getMessage("command-template-install-try")
                                                .replace("%version%", versionType.getName() + "-" + version.getName())
                                                .replace("%template%", template.toString())
                                        );

                                        if (CloudNet.getInstance().getServiceVersionProvider().installServiceVersion(versionType, version, storage, template)) {
                                            sender.sendMessage(LanguageManager.getMessage("command-template-install-success")
                                                    .replace("%version%", versionType.getName() + "-" + version.getName())
                                                    .replace("%template%", template.toString())
                                            );
                                        } else {
                                            sender.sendMessage(LanguageManager.getMessage("command-template-install-failed")
                                                    .replace("%version%", versionType.getName() + "-" + version.getName())
                                                    .replace("%template%", template.toString())
                                            );
                                        }
                                        return null;
                                    });
                                },
                                subCommand -> subCommand.appendUsage("| example: template install Lobby/default paperspigot 1.13.2"),
                                exactStringIgnoreCase("install"),
                                template("template", true),
                                dynamicString(
                                        "versionType",
                                        LanguageManager.getMessage("command-template-invalid-version-type"),
                                        input -> CloudNet.getInstance().getServiceVersionProvider().getServiceVersionType(input).isPresent(),
                                        () -> CloudNet.getInstance().getServiceVersionProvider().getServiceVersionTypes().keySet()
                                ),
                                dynamicString(
                                        "version",
                                        LanguageManager.getMessage("command-template-invalid-version"),
                                        input -> CloudNet.getInstance().getServiceVersionProvider().getServiceVersionTypes().values().stream()
                                                .map(ServiceVersionType::getVersions)
                                                .flatMap(Collection::parallelStream)
                                                .map(ServiceVersion::getName)
                                                .anyMatch(name -> name.equalsIgnoreCase(input)),
                                        () -> CloudNet.getInstance().getServiceVersionProvider().getServiceVersionTypes().values().stream()
                                                .map(ServiceVersionType::getVersions)
                                                .flatMap(Collection::parallelStream)
                                                .map(ServiceVersion::getName)
                                                .collect(Collectors.toList())
                                )
                        )

                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                                    ServiceTemplate template = (ServiceTemplate) args.argument("template").get();
                                    ITemplateStorage storage = CloudNet.getInstance().getServicesRegistry().getService(ITemplateStorage.class, template.getStorage());

                                    if (!storage.has(template)) {
                                        sender.sendMessage(LanguageManager.getMessage("command-template-delete-template-not-found")
                                                .replace("%template%", template.getTemplatePath())
                                                .replace("%storage%", template.getStorage())
                                        );
                                        return;
                                    }

                                    storage.delete(template);
                                    sender.sendMessage(LanguageManager.getMessage("command-template-delete-success")
                                            .replace("%template%", template.getTemplatePath())
                                            .replace("%storage%", template.getStorage())
                                    );
                                },
                                anyStringIgnoreCase("delete", "remove", "rm"),
                                template("template", true)
                        )

                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                                    ServiceTemplate template = (ServiceTemplate) args.argument("template").get();
                                    ITemplateStorage storage = CloudNet.getInstance().getServicesRegistry().getService(ITemplateStorage.class, template.getStorage());
                                    ServiceEnvironmentType environment = (ServiceEnvironmentType) args.argument(QuestionAnswerTypeEnum.class).get();

                                    if (storage.has(template)) {
                                        sender.sendMessage(LanguageManager.getMessage("command-template-create-template-already-exists")
                                                .replace("%template%", template.getTemplatePath())
                                                .replace("%storage%", template.getStorage())
                                        );
                                        return;
                                    }

                                    try {
                                        if (TemplateStorageUtil.createAndPrepareTemplate(storage, template.getPrefix(), template.getName(), environment)) {
                                            sender.sendMessage(LanguageManager.getMessage("command-template-create-success")
                                                    .replace("%template%", template.getTemplatePath())
                                                    .replace("%storage%", template.getStorage())
                                            );
                                        }
                                    } catch (IOException exception) {
                                        sender.sendMessage(LanguageManager.getMessage("command-template-create-failed")
                                                .replace("%template%", template.getTemplatePath())
                                                .replace("%storage%", template.getStorage())
                                        );
                                        exception.printStackTrace();
                                    }
                                },
                                anyStringIgnoreCase("create", "new"),
                                template("template"),
                                exactEnum(ServiceEnvironmentType.class)
                        )

                        .generateCommand(
                                (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                                    ServiceTemplate sourceTemplate = (ServiceTemplate) args.argument("storage:prefix/name (sourceTemplate)").get();
                                    ServiceTemplate targetTemplate = (ServiceTemplate) args.argument("storage:prefix/name (targetTemplate)").get();
                                    ITemplateStorage sourceStorage = CloudNet.getInstance().getServicesRegistry().getService(ITemplateStorage.class, sourceTemplate.getStorage());
                                    ITemplateStorage targetStorage = CloudNet.getInstance().getServicesRegistry().getService(ITemplateStorage.class, targetTemplate.getStorage());

                                    if (sourceTemplate.equals(targetTemplate)) {
                                        sender.sendMessage(LanguageManager.getMessage("command-template-copy-same-source-and-target"));
                                        return;
                                    }

                                    CloudNet.getInstance().scheduleTask(() -> {
                                        sender.sendMessage(LanguageManager.getMessage("command-template-copy")
                                                .replace("%sourceTemplate%", sourceTemplate.toString())
                                                .replace("%targetTemplate%", targetTemplate.toString())
                                        );

                                        targetStorage.delete(targetTemplate);
                                        targetStorage.create(targetTemplate);

                                        InputStream stream = sourceStorage.asZipInputStream(sourceTemplate);
                                        if (stream == null) {
                                            sender.sendMessage(LanguageManager.getMessage("command-template-copy-failed"));
                                            return null;
                                        }

                                        targetStorage.deploy(stream, targetTemplate);
                                        sender.sendMessage(LanguageManager.getMessage("command-template-copy-success")
                                                .replace("%sourceTemplate%", sourceTemplate.toString())
                                                .replace("%targetTemplate%", targetTemplate.toString())
                                        );
                                        return null;
                                    });
                                },
                                anyStringIgnoreCase("copy", "cp"),
                                template("storage:prefix/name (sourceTemplate)"),
                                template("storage:prefix/name (targetTemplate)")
                        )

                        .getSubCommands(),
                "template", "t"
        );
        super.prefix = "cloudnet";
        super.permission = "cloudnet.command." + super.names[0];
        super.description = LanguageManager.getMessage("command-description-template");
    }
}
