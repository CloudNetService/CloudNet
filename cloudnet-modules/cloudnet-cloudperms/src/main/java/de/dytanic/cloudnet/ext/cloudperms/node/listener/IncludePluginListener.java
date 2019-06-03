package de.dytanic.cloudnet.ext.cloudperms.node.listener;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.event.service.CloudServicePreStartEvent;
import de.dytanic.cloudnet.ext.cloudperms.node.CloudNetCloudPermissionsModule;

import java.io.File;

public final class IncludePluginListener {

    @EventListener
    public void handle(CloudServicePreStartEvent event) {
        if (!CloudNetCloudPermissionsModule.getInstance().getConfig().getBoolean("enabled")) return;

        try {
            for (String group : CloudNetCloudPermissionsModule.getInstance().getExcludedGroups())
                if (Iterables.contains(group, event.getCloudService().getServiceConfiguration().getGroups()))
                    return;

        } catch (Exception ignored) {
        }

        new File(event.getCloudService().getDirectory(), "plugins").mkdirs();
        File file = new File(event.getCloudService().getDirectory(), "plugins/cloudnet-cloudperms.jar");
        file.delete();

        if (DefaultModuleHelper.copyCurrentModuleInstanceFromClass(IncludePluginListener.class, file))
            DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(IncludePluginListener.class,
                    event.getCloudService().getServiceConfiguration().getProcessConfig().getEnvironment(), file);
    }
}