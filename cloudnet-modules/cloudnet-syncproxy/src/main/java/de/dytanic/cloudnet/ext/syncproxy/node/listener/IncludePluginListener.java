package de.dytanic.cloudnet.ext.syncproxy.node.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.event.service.CloudServicePreStartEvent;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.node.CloudNetSyncProxyModule;
import de.dytanic.cloudnet.service.ICloudService;

import java.io.File;

public final class IncludePluginListener {

    @EventListener
    public void handle(CloudServicePreStartEvent event) {
        ICloudService service = event.getCloudService();

        if (!service.getServiceId().getEnvironment().isMinecraftProxy()) {
            return;
        }

        SyncProxyConfiguration syncProxyConfiguration = CloudNetSyncProxyModule.getInstance().getSyncProxyConfiguration();

        boolean groupEntryExists = syncProxyConfiguration.getLoginConfigurations().stream()
                .anyMatch(loginConfiguration -> service.getGroups().contains(loginConfiguration.getTargetGroup()))
                || syncProxyConfiguration.getTabListConfigurations().stream()
                .anyMatch(tabListConfiguration -> service.getGroups().contains(tabListConfiguration.getTargetGroup()));

        if (groupEntryExists) {
            new File(event.getCloudService().getDirectory(), "plugins").mkdirs();
            File file = new File(event.getCloudService().getDirectory(), "plugins/cloudnet-syncproxy.jar");
            file.delete();

            if (DefaultModuleHelper.copyCurrentModuleInstanceFromClass(IncludePluginListener.class, file)) {
                DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(IncludePluginListener.class,
                        event.getCloudService().getServiceConfiguration().getProcessConfig().getEnvironment(), file);
            }
        }
    }

}