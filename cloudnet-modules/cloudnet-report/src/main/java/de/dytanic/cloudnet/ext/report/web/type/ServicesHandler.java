package de.dytanic.cloudnet.ext.report.web.type;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.ext.report.CloudNetReportModule;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ServicesHandler extends ReportHandler {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    @Override
    public Map<String, Object> load() {
        String rawServiceFile = super.loadFile("service.html");
        Collection<String> services = new ArrayList<>();

        for (ServiceInfoSnapshot service : CloudNet.getInstance().getCloudServiceProvider().getCloudServices()) {
            Map<String, Object> serviceReplacements = new HashMap<>();

            serviceReplacements.put("service.name", service.getName());
            serviceReplacements.put("service.uniqueId", service.getServiceId().getUniqueId());
            serviceReplacements.put("service.node", service.getServiceId().getNodeUniqueId());

            serviceReplacements.put("service.creationTime", DATE_FORMAT.format(service.getCreationTime()));
            serviceReplacements.put("service.connectedTime", DATE_FORMAT.format(service.getConnectedTime()));

            serviceReplacements.put("service.maxMemory", service.getConfiguration().getProcessConfig().getMaxHeapMemorySize());
            serviceReplacements.put("service.environment", service.getConfiguration().getProcessConfig().getEnvironment());

            serviceReplacements.put("service.runtime", service.getConfiguration().getRuntime());

            serviceReplacements.put("service.autoDeleteOnStop", service.getConfiguration().isAutoDeleteOnStop() ? "yes" : "no");
            serviceReplacements.put("service.static", service.getConfiguration().isStaticService() ? "yes" : " no");

            serviceReplacements.put("service.groups", String.join(", ", service.getConfiguration().getGroups()));
            serviceReplacements.put("service.jvmOptions", String.join(" ", service.getConfiguration().getProcessConfig().getJvmOptions()));
            serviceReplacements.put("service.deletedFilesAfterStop", String.join(", ", service.getConfiguration().getDeletedFilesAfterStop()));

            serviceReplacements.put("service.templates", Arrays.stream(service.getConfiguration().getTemplates())
                    .map(template -> template.toString() + " (Always copy to static services: " + (template.shouldAlwaysCopyToStaticServices() ? "yes" : "no") + ")")
                    .collect(Collectors.joining("<br>"))
            );
            serviceReplacements.put("service.deployments", Arrays.stream(service.getConfiguration().getDeployments())
                    .map(deployment -> deployment.getTemplate().toString() + " (Excluded: " + String.join(", ", deployment.getExcludes()) + ")")
                    .collect(Collectors.joining("<br>"))
            );
            serviceReplacements.put("service.includes", Arrays.stream(service.getConfiguration().getIncludes())
                    .map(inclusion -> inclusion.getUrl() + " -> " + inclusion.getDestination())
                    .collect(Collectors.joining("<br>"))
            );

            serviceReplacements.put("service.process.heapUsage", service.getProcessSnapshot().getHeapUsageMemory());
            serviceReplacements.put("service.process.noHeapUsage", service.getProcessSnapshot().getNoHeapUsageMemory());
            serviceReplacements.put("service.process.maxHeap", service.getProcessSnapshot().getMaxHeapMemory());
            serviceReplacements.put("service.process.currentClasses", service.getProcessSnapshot().getCurrentLoadedClassCount());
            serviceReplacements.put("service.process.totalClasses", service.getProcessSnapshot().getTotalLoadedClassCount());
            serviceReplacements.put("service.process.unloadedClasses", service.getProcessSnapshot().getUnloadedClassCount());
            serviceReplacements.put("service.process.threadCount", service.getProcessSnapshot().getThreads().size());
            serviceReplacements.put("service.process.cpuUsage", service.getProcessSnapshot().getCpuUsage());
            serviceReplacements.put("service.process.pid", service.getProcessSnapshot().getPid());

            services.add(super.replace(rawServiceFile, serviceReplacements));
        }

        Map<String, Object> replacements = new HashMap<>();

        replacements.put("services.count.global", CloudNet.getInstance().getCloudServiceProvider().getCloudServices().size());
        replacements.put("services.count.local", CloudNet.getInstance().getCloudServiceManager().getLocalCloudServices().size());
        replacements.put("services.list", String.join("\n", services));

        return replacements;
    }
}
