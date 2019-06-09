package de.dytanic.cloudnet.ext.bridge.node.listener;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.event.service.CloudServicePreStartEvent;
import de.dytanic.cloudnet.ext.bridge.node.CloudNetBridgeModule;

import java.io.File;

public final class IncludePluginListener {

    @EventListener
    public void handle(CloudServicePreStartEvent event) {
        try {
            for (String group : CloudNetBridgeModule.getInstance().getBridgeConfiguration().getExcludedGroups())
                if (Iterables.contains(group, event.getCloudService().getServiceConfiguration().getGroups()))
                    return;
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        new File(event.getCloudService().getDirectory(), "plugins").mkdirs();
        File file = new File(event.getCloudService().getDirectory(), "plugins/cloudnet-bridge.jar");
        file.delete();

        if (DefaultModuleHelper.copyCurrentModuleInstanceFromClass(IncludePluginListener.class, file))
            DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(IncludePluginListener.class,
                    event.getCloudService().getServiceConfiguration().getProcessConfig().getEnvironment(), file);
    }
}