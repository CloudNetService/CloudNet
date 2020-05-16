package de.dytanic.cloudnet.ext.signs.node.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.event.service.CloudServicePreStartEvent;
import de.dytanic.cloudnet.ext.signs.node.CloudNetSignsModule;

import java.io.File;
import java.util.Arrays;

public final class IncludePluginListener {

    @EventListener
    public void handle(CloudServicePreStartEvent event) {
        if (!event.getCloudService().getServiceConfiguration().getServiceId().getEnvironment().isMinecraftJavaServer()
                && !event.getCloudService().getServiceConfiguration().getServiceId().getEnvironment().isMinecraftBedrockServer()) {
            return;
        }

        boolean installPlugin = CloudNetSignsModule.getInstance().getSignConfiguration().getConfigurations().stream()
                .anyMatch(signConfigurationEntry -> Arrays.asList(event.getCloudService().getServiceConfiguration().getGroups()).contains(signConfigurationEntry.getTargetGroup()));

        new File(event.getCloudService().getDirectory(), "plugins").mkdirs();
        File file = new File(event.getCloudService().getDirectory(), "plugins/cloudnet-signs.jar");
        file.delete();

        if (installPlugin && DefaultModuleHelper.copyCurrentModuleInstanceFromClass(IncludePluginListener.class, file)) {
            DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(IncludePluginListener.class,
                    event.getCloudService().getServiceConfiguration().getProcessConfig().getEnvironment(), file);
        }
    }

}