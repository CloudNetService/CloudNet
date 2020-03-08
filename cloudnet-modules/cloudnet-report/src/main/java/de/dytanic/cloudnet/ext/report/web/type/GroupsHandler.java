package de.dytanic.cloudnet.ext.report.web.type;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class GroupsHandler extends ReportHandler {

    @Override
    public Map<String, Object> load() {
        String rawGroupFile = super.loadFile("group.html");
        Collection<String> groups = new ArrayList<>();

        for (GroupConfiguration group : CloudNet.getInstance().getGroupConfigurationProvider().getGroupConfigurations()) {
            Map<String, Object> taskReplacements = new HashMap<>();

            taskReplacements.put("group.name", group.getName());

            taskReplacements.put("group.templates", group.getTemplates().stream()
                    .map(template -> template.toString() + " (Always copy to static services: " + (template.shouldAlwaysCopyToStaticServices() ? "yes" : "no") + ")")
                    .collect(Collectors.joining("<br>"))
            );
            taskReplacements.put("group.deployments", group.getDeployments().stream()
                    .map(deployment -> deployment.getTemplate().toString() + " (Excluded: " + String.join(", ", deployment.getExcludes()) + ")")
                    .collect(Collectors.joining("<br>"))
            );
            taskReplacements.put("group.includes", group.getIncludes().stream()
                    .map(inclusion -> inclusion.getUrl() + " -> " + inclusion.getDestination())
                    .collect(Collectors.joining("<br>"))
            );
            
            taskReplacements.put("group.properties", group.getProperties().toPrettyJson().replace("\n", "<br>").replace(" ", "&nbsp;"));

            groups.add(super.replace(rawGroupFile, taskReplacements));
        }

        Map<String, Object> replacements = new HashMap<>();

        replacements.put("groups.count", groups.size());
        replacements.put("groups.list", String.join("\n", groups));

        return replacements;
    }
}
