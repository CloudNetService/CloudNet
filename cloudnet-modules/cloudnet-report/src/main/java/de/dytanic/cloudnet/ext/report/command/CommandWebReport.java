package de.dytanic.cloudnet.ext.report.command;

import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.ext.report.CloudNetReportModule;
import de.dytanic.cloudnet.ext.report.web.remote.WebReport;
import de.dytanic.cloudnet.ext.report.web.remote.WebReportUploadResult;

import java.io.IOException;

public class CommandWebReport extends Command {

    public CommandWebReport() {
        super("webreport");

        this.usage = "webreport --local";
        this.permission = "cloudnet.command.webreport";
        this.prefix = "cloudnet-report";
        this.description = LanguageManager.getMessage("module-report-command-webreport-description");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        if (properties.containsKey("local")) {
            CloudNetReportModule.getInstance().getWebReportProvider().setWebEnabled(!CloudNetReportModule.getInstance().getWebReportProvider().isWebEnabled());
            if (CloudNetReportModule.getInstance().getWebReportProvider().isWebEnabled()) {
                String[] webReportURLs = CloudNetReportModule.getInstance().getWebReportURLs();
                if (webReportURLs.length == 0) {
                    CloudNetReportModule.getInstance().getWebReportProvider().setWebEnabled(false);
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
            return;
        }

        WebReport webReport = CloudNetReportModule.getInstance().getWebReportProvider().createReport();
        try {
            WebReportUploadResult result = CloudNetReportModule.getInstance().getWebReportProvider().uploadReport(webReport);

            long timeoutMinutes = (result.getTimeout() - System.currentTimeMillis()) / 1000 / 60;
            sender.sendMessage(LanguageManager.getMessage("module-report-command-webreport-upload-success")
                    .replace("%url%", result.getUrl())
                    .replace("%timeout%", String.valueOf(timeoutMinutes))
            );
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
