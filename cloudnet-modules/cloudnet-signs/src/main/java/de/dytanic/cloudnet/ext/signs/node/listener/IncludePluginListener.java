package de.dytanic.cloudnet.ext.signs.node.listener;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.event.service.CloudServicePreStartEvent;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationEntry;
import de.dytanic.cloudnet.ext.signs.node.CloudNetSignsModule;

import java.io.File;

public final class IncludePluginListener {

    @EventListener
    public void handle(CloudServicePreStartEvent event) {
        if (!event.getCloudService().getServiceConfiguration().getServiceId().getEnvironment().isMinecraftJavaServer()
                && !event.getCloudService().getServiceConfiguration().getServiceId().getEnvironment().isMinecraftBedrockServer()) {
            return;
        }

        SignConfigurationEntry signConfigurationEntry = Iterables.first(CloudNetSignsModule.getInstance().getSignConfiguration().getConfigurations(),
                signConfigurationEntry1 -> Iterables.contains(signConfigurationEntry1.getTargetGroup(), event.getCloudService().getServiceConfiguration().getGroups()));

        if (signConfigurationEntry == null) {
            return;
        }

        new File(event.getCloudService().getDirectory(), "plugins").mkdirs();
        File file = new File(event.getCloudService().getDirectory(), "plugins/cloudnet-signs.jar");
        file.delete();

        if (DefaultModuleHelper.copyCurrentModuleInstanceFromClass(IncludePluginListener.class, file)) {
            DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(IncludePluginListener.class,
                    event.getCloudService().getServiceConfiguration().getProcessConfig().getEnvironment(), file);
        }
    }
}