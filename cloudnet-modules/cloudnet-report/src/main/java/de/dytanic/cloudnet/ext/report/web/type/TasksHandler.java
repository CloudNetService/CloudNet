package de.dytanic.cloudnet.ext.report.web.type;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTask;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class TasksHandler extends ReportHandler {

    @Override
    public String load(IHttpContext context) {
        String rawTaskFile = super.loadFile("task.html");
        Collection<String> tasks = new ArrayList<>();

        for (ServiceTask task : CloudNet.getInstance().getServiceTaskProvider().getPermanentServiceTasks()) {
            Map<String, Object> taskReplacements = new HashMap<>();

            taskReplacements.put("task.name", task.getName());

            taskReplacements.put("task.minServiceCount", task.getMinServiceCount());
            taskReplacements.put("task.maintenance", task.isMaintenance() ? "yes" : "no");
            taskReplacements.put("task.nodes", String.join(", ", task.getAssociatedNodes()));
            
            taskReplacements.put("task.maxMemory", task.getProcessConfiguration().getMaxHeapMemorySize());
            taskReplacements.put("task.environment", task.getProcessConfiguration().getEnvironment());

            taskReplacements.put("task.runtime", task.getRuntime());

            taskReplacements.put("task.autoDeleteOnStop", task.isAutoDeleteOnStop() ? "yes" : "no");
            taskReplacements.put("task.static", task.isStaticServices() ? "yes" : " no");

            taskReplacements.put("task.groups", String.join(", ", task.getGroups()));
            taskReplacements.put("task.jvmOptions", String.join(" ", task.getProcessConfiguration().getJvmOptions()));
            taskReplacements.put("task.deletedFilesAfterStop", String.join(", ", task.getDeletedFilesAfterStop()));

            taskReplacements.put("task.templates", task.getTemplates().stream()
                    .map(template -> template.toString() + " (Always copy to static services: " + (template.shouldAlwaysCopyToStaticServices() ? "yes" : "no") + ")")
                    .collect(Collectors.joining("<br>"))
            );
            taskReplacements.put("task.deployments", task.getDeployments().stream()
                    .map(deployment -> deployment.getTemplate().toString() + " (Excluded: " + String.join(", ", deployment.getExcludes()) + ")")
                    .collect(Collectors.joining("<br>"))
            );
            taskReplacements.put("task.includes", task.getIncludes().stream()
                    .map(inclusion -> inclusion.getUrl() + " -> " + inclusion.getDestination())
                    .collect(Collectors.joining("<br>"))
            );
            
            taskReplacements.put("task.properties", task.getProperties().toPrettyJson().replace("\n", "<br>").replace(" ", "&nbsp;"));
            
            tasks.add(super.replace(rawTaskFile, taskReplacements));
        }

        Map<String, Object> replacements = new HashMap<>();

        replacements.put("tasks.count", CloudNet.getInstance().getServiceTaskProvider().getPermanentServiceTasks().size());
        replacements.put("tasks.list", String.join("\n", tasks));

        String file = super.loadFile("tasks.html");
        return super.replace(file, replacements);
    }
}
