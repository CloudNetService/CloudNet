package de.dytanic.cloudnet.ext.report.web.type;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class IndexHandler extends ReportHandler {
    @Override
    public String load(IHttpContext context) {
        Map<String, Object> replacements = new HashMap<>();

        replacements.put("node.name", CloudNet.getInstance().getConfig().getIdentity().getUniqueId());
        replacements.put("node.version", "CloudNet " + CloudNet.class.getPackage().getImplementationTitle()
                + " " + CloudNet.class.getPackage().getImplementationVersion() + " by Dytanic & the CloudNet Community");

        replacements.put("node.cluster.id", CloudNet.getInstance().getConfig().getClusterConfig().getClusterId());
        replacements.put("node.cluster.nodes.count", CloudNet.getInstance().getConfig().getClusterConfig().getNodes().size());
        replacements.put("node.cluster.nodes.list", CloudNet.getInstance().getConfig().getClusterConfig().getNodes().stream()
                .map(node -> node.getUniqueId() + "@" + Arrays.toString(node.getListeners()))
                .collect(Collectors.joining("<br>"))
        );

        replacements.put("node.memory.free", CloudNet.getInstance().getCurrentNetworkClusterNodeInfoSnapshot().getMaxMemory() -
                CloudNet.getInstance().getCurrentNetworkClusterNodeInfoSnapshot().getUsedMemory());
        replacements.put("node.memory.used", CloudNet.getInstance().getCurrentNetworkClusterNodeInfoSnapshot().getUsedMemory());
        replacements.put("node.memory.reserved", CloudNet.getInstance().getCurrentNetworkClusterNodeInfoSnapshot().getReservedMemory());
        replacements.put("node.memory.max", CloudNet.getInstance().getCurrentNetworkClusterNodeInfoSnapshot().getMaxMemory());

        replacements.put("node.ipWhitelist", String.join("<br>", CloudNet.getInstance().getConfig().getIpWhitelist()));

        replacements.put("node.cpu.process", CPUUsageResolver.CPU_USAGE_OUTPUT_FORMAT.format(CPUUsageResolver.getProcessCPUUsage()));
        replacements.put("node.cpu.system", CPUUsageResolver.CPU_USAGE_OUTPUT_FORMAT.format(CPUUsageResolver.getSystemCPUUsage()));

        replacements.put("node.maxCPUUsageToStartServices", CloudNet.getInstance().getConfig().getMaxCPUUsageToStartServices());
        replacements.put("node.maxServiceConsoleLogLines", CloudNet.getInstance().getConfig().getMaxServiceConsoleLogCacheSize());
        replacements.put("node.jvmCommand", CloudNet.getInstance().getConfig().getJVMCommand());
        replacements.put("node.defaultJVMOptions", CloudNet.getInstance().getConfig().isDefaultJVMOptionParameters() ? "yes" : "no");

        replacements.put("node.httpListeners", CloudNet.getInstance().getConfig().getHttpListeners().stream()
                .map(HostAndPort::toString)
                .collect(Collectors.joining("<br>"))
        );
        replacements.put("node.listeners", Arrays.stream(CloudNet.getInstance().getConfig().getIdentity().getListeners())
                .map(HostAndPort::toString)
                .collect(Collectors.joining("<br>"))
        );
        replacements.put("node.hostAddress", CloudNet.getInstance().getConfig().getHostAddress());


        String file = super.loadFile("index.html");
        return super.replace(file, replacements);
    }
}
