package de.dytanic.cloudnet.ext.report.web.type;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.module.driver.DriverModule;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.ext.report.CloudNetReportModule;

import java.util.*;
import java.util.stream.Collectors;

public class ModuleHandler extends ReportHandler {
    @Override
    public Map<String, Object> load() {
        String rawModuleFile = super.loadFile("module.html");
        Collection<String> modules = new ArrayList<>();

        for (IModuleWrapper module : CloudNet.getInstance().getModuleProvider().getModules()) {
            Map<String, Object> moduleReplacements = new HashMap<>();

            moduleReplacements.put("module.name", module.getModuleConfiguration().getName());
            moduleReplacements.put("module.version", module.getModuleConfiguration().getVersion());
            moduleReplacements.put("module.group", module.getModuleConfiguration().getGroup());
            moduleReplacements.put("module.desc", module.getModuleConfiguration().getDescription());
            moduleReplacements.put("module.website", module.getModuleConfiguration().getWebsite());
            moduleReplacements.put("module.author", module.getModuleConfiguration().getAuthor());
            moduleReplacements.put("module.main", module.getModuleConfiguration().getMain());
            moduleReplacements.put("module.dependencies", module.getModuleConfiguration().getDependencies() == null ? "" :
                    Arrays.stream(module.getModuleConfiguration().getDependencies())
                            .map(dependency -> dependency.getGroup() + ":" + dependency.getName() + ":" + dependency.getVersion() +
                                    (dependency.getRepo() == null ? " (Module)" : " (Maven Repository: " + dependency.getRepo() + ")"))
                            .collect(Collectors.joining("<br>"))
            );
            moduleReplacements.put("module.repositories", module.getModuleConfiguration().getRepos() == null ? "" :
                    Arrays.stream(module.getModuleConfiguration().getRepos())
                            .map(repository -> repository.getName() + ": " + repository.getUrl())
                            .collect(Collectors.joining("<br>"))
            );
            moduleReplacements.put("module.restartOnReload", !module.getModuleConfiguration().isRuntimeModule() ? "yes" : "no");
            if (module.getModuleConfiguration().storesSensitiveData()) {
                moduleReplacements.put("module.config", "Hidden because this may contain sensitive data");
            } else if (!(module.getModule() instanceof DriverModule)) {
                moduleReplacements.put("module.config", "This module is no driver module and therefore it has no config");
            } else {
                moduleReplacements.put("module.config", ((DriverModule) module.getModule()).getConfig().toPrettyJson().replace("\n", "<br>").replace(" ", "&nbsp;"));
            }

            modules.add(super.replace(rawModuleFile, moduleReplacements));
        }

        Map<String, Object> replacements = new HashMap<>();

        replacements.put("modules.count", CloudNet.getInstance().getModuleProvider().getModules().size());
        replacements.put("modules.list", String.join("\n", modules));

        return replacements;
    }
}
