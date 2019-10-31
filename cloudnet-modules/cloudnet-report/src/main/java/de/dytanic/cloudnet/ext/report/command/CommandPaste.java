package de.dytanic.cloudnet.ext.report.command;

import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.report.CloudNetReportModule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CommandPaste extends Command {

    public CommandPaste() {
        super("paste", "haste");

        this.usage = "paste <name>";
        this.permission = "cloudnet.console.command.paste";
        this.prefix = "cloudnet-report";
        this.description = LanguageManager.getMessage("module-report-command-paste-description");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        if (args.length == 0) {
            sender.sendMessage("paste <name : uniqueId>");
            return;
        }

        ServiceInfoSnapshot serviceInfoSnapshot = getServiceInfoSnapshot(args[0]);

        if (serviceInfoSnapshot != null) {
            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                 PrintWriter printWriter = new PrintWriter(byteArrayOutputStream, true)) {
                for (String line : CloudNetDriver.getInstance().getCachedLogMessagesFromService(serviceInfoSnapshot.getServiceId().getUniqueId())) {
                    printWriter.println(line);
                }

                printWriter.println();
                printWriter.println("ServiceInfoSnapshot");
                printWriter.println();

                printWriter.println(new JsonDocument(serviceInfoSnapshot).toPrettyJson());

                sender.sendMessage(LanguageManager.getMessage("module-report-command-paste-success")
                        .replace("%url%", CloudNetReportModule.getInstance()
                                .executePaste(new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)))
                );

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private ServiceInfoSnapshot getServiceInfoSnapshot(String argument) {
        Validate.checkNotNull(argument);

        ServiceInfoSnapshot serviceInfoSnapshot = Iterables.first(CloudNetDriver.getInstance().getCloudServices(), serviceInfoSnapshot13 -> serviceInfoSnapshot13.getServiceId().getUniqueId().toString().toLowerCase().contains(argument.toLowerCase()));

        if (serviceInfoSnapshot == null) {
            List<ServiceInfoSnapshot> serviceInfoSnapshots = Iterables.filter(CloudNetDriver.getInstance().getCloudServices(), serviceInfoSnapshot12 -> serviceInfoSnapshot12.getServiceId().getName().toLowerCase().contains(argument.toLowerCase()));

            if (!serviceInfoSnapshots.isEmpty()) {
                if (serviceInfoSnapshots.size() > 1) {
                    serviceInfoSnapshot = Iterables.first(serviceInfoSnapshots, serviceInfoSnapshot1 -> serviceInfoSnapshot1.getServiceId().getName().equalsIgnoreCase(argument));
                } else {
                    serviceInfoSnapshot = serviceInfoSnapshots.get(0);
                }
            }
        }

        return serviceInfoSnapshot;
    }
}