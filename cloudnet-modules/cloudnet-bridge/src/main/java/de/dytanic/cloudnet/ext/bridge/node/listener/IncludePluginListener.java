package de.dytanic.cloudnet.ext.bridge.node.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.event.service.CloudServicePreStartEvent;
import de.dytanic.cloudnet.ext.bridge.node.CloudNetBridgeModule;

import java.io.File;
import java.util.Arrays;

public final class IncludePluginListener {

    @EventListener
    public void handle(CloudServicePreStartEvent event) {
        boolean installPlugin = CloudNetBridgeModule.getInstance().getBridgeConfiguration().getExcludedGroups()
                .stream()
                .noneMatch(excludedGroup -> Arrays.asList(event.getCloudService().getServiceConfiguration().getGroups()).contains(excludedGroup));

        new File(event.getCloudService().getDirectory(), "plugins").mkdirs();
        File file = new File(event.getCloudService().getDirectory(), "plugins/cloudnet-bridge.jar");
        file.delete();

        if (installPlugin && DefaultModuleHelper.copyCurrentModuleInstanceFromClass(IncludePluginListener.class, file)) {
            DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(IncludePluginListener.class,
                    event.getCloudService().getServiceConfiguration().getProcessConfig().getEnvironment(), file);
        }
    }
}