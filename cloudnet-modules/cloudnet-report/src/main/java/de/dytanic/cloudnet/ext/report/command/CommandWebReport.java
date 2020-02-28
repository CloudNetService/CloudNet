package de.dytanic.cloudnet.ext.report.command;

import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.ext.report.CloudNetReportModule;

public class CommandWebReport extends Command {

    public CommandWebReport() {
        super("webreport");

        this.usage = "webreport";
        this.permission = "cloudnet.command.webreport";
        this.prefix = "cloudnet-report";
        this.description = LanguageManager.getMessage("module-report-command-webreport-description");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        CloudNetReportModule.getInstance().setWebEnabled(!CloudNetReportModule.getInstance().isWebEnabled());
        if (CloudNetReportModule.getInstance().isWebEnabled()) {
            String[] webReportURLs = CloudNetReportModule.getInstance().getWebReportURLs();
            if (webReportURLs.length == 0) {
                CloudNetReportModule.getInstance().setWebEnabled(false);
                sender.sendMessage(LanguageManager.getMessage("module-report-command-webreport-enable-failed"));
                return;
            }
            sender.sendMessage(LanguageManager.getMessage("module-report-command-webreport-enabled"));
            for (String webReportURL : webReportURLs) {
                sender.sendMessage(" - " + webReportURL);
            }
        } else {
            sender.sendMessage(LanguageManager.getMessage("module-report-command-webreport-disabled"));
        }
    }
}
