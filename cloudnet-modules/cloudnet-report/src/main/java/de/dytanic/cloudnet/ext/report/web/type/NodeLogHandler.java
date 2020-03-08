package de.dytanic.cloudnet.ext.report.web.type;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.console.log.ColouredLogFormatter;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.ext.report.CloudNetReportModule;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class NodeLogHandler extends ReportHandler {
    @Override
    public Map<String, Object> load() {
        Map<String, Object> replacements = new HashMap<>();

        replacements.put("node.name", CloudNet.getInstance().getConfig().getIdentity().getUniqueId());

        Collection<String> logLines = CloudNetReportModule.getInstance().getNodeLog();
        replacements.put("node.log", String.join("<br>", logLines));

        return replacements;
    }
}
