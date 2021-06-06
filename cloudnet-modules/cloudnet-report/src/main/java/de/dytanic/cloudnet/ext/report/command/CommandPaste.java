package de.dytanic.cloudnet.ext.report.command;

import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.dynamicString;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.exactStringIgnoreCase;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.sub.SubCommand;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.report.CloudNetReportModule;

public final class CommandPaste extends SubCommandHandler {

  public CommandPaste() {
    super(
      SubCommandBuilder.create()

        .postExecute((subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          String content = (String) internalProperties.get("content");

          String url = CloudNetReportModule.getInstance().executePaste(content);

          if (url != null) {
            sender.sendMessage(LanguageManager.getMessage("module-report-command-paste-success").replace("%url%", url));
          } else {
            sender.sendMessage(LanguageManager.getMessage("module-report-command-paste-failed")
              .replace("%url%", CloudNetReportModule.getInstance().getPasteURL()));
          }
        })

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            ServiceInfoSnapshot serviceInfoSnapshot = CloudNet.getInstance().getCloudServiceProvider()
              .getCloudServiceByName((String) args.argument("name").get());
            internalProperties
              .put("content", CloudNetReportModule.getInstance().createPasteContentService(serviceInfoSnapshot));
          },
          exactStringIgnoreCase("service"),
          dynamicString(
            "name",
            LanguageManager.getMessage("module-report-command-paste-service-not-found"),
            input -> CloudNet.getInstance().getCloudServiceProvider().getCloudServiceByName(input) != null
          )
        )
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) ->
            internalProperties.put("content", CloudNetReportModule.getInstance().createPasteContentNode(properties)),
          SubCommand::enableProperties,
          exactStringIgnoreCase("node")
        )
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) ->
            internalProperties.put("content", CloudNetReportModule.getInstance().createPasteContentModules()),
          exactStringIgnoreCase("modules")
        )
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) ->
            internalProperties.put("content", CloudNetReportModule.getInstance().createPasteContentTasks()),
          exactStringIgnoreCase("tasks")
        )
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) ->
            internalProperties.put("content", CloudNetReportModule.getInstance().createPasteContentAll()),
          exactStringIgnoreCase("all")
        )

        .getSubCommands(),
      "paste", "haste"
    );

    this.usage = "paste service|node|modules|tasks|all";
    this.permission = "cloudnet.command.paste";
    this.prefix = "cloudnet-report";
    this.description = LanguageManager.getMessage("module-report-command-paste-description");
  }

}
