package de.dytanic.cloudnet.ext.cloudperms.node.listener;

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.event.service.CloudServicePreStartEvent;
import de.dytanic.cloudnet.ext.cloudperms.node.CloudNetCloudPermissionsModule;

import java.nio.file.Path;
import java.util.Arrays;

public final class IncludePluginListener {

    @EventListener
    public void handle(CloudServicePreStartEvent event) {
        boolean installPlugin = CloudNetCloudPermissionsModule.getInstance().isEnabled() && CloudNetCloudPermissionsModule.getInstance().getExcludedGroups()
                .stream()
                .noneMatch(excludedGroup -> Arrays.asList(event.getCloudService().getServiceConfiguration().getGroups()).contains(excludedGroup));

        Path pluginsFolder = event.getCloudService().getDirectoryPath().resolve("plugins");
        FileUtils.createDirectoryReported(pluginsFolder);

        Path targetFile = pluginsFolder.resolve("cloudnet-cloudperms.jar");
        FileUtils.deleteFileReported(targetFile);

        if (installPlugin && DefaultModuleHelper.copyCurrentModuleInstanceFromClass(IncludePluginListener.class, targetFile)) {
            DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(
                    IncludePluginListener.class,
                    event.getCloudService().getServiceConfiguration().getProcessConfig().getEnvironment(),
                    targetFile
            );
        }
    }
}