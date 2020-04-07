package de.dytanic.cloudnet.ext.syncproxy.node.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.event.service.task.ServiceTaskAddEvent;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfigurationWriterAndReader;
import de.dytanic.cloudnet.ext.syncproxy.node.CloudNetSyncProxyModule;

public class SyncProxyDefaultConfigurationListener {

    @EventListener
    public void handleTaskAdd(ServiceTaskAddEvent event) {
        ServiceTask task = event.getTask();

        if (!task.getProcessConfiguration().getEnvironment().isMinecraftJavaProxy() &&
                !task.getProcessConfiguration().getEnvironment().isMinecraftBedrockProxy()) {
            return;
        }

        SyncProxyConfiguration configuration = CloudNetSyncProxyModule.getInstance().getSyncProxyConfiguration();
        boolean modified = false;

        if (configuration.getLoginConfigurations().stream()
                .noneMatch(loginConfiguration -> loginConfiguration.getTargetGroup().equals(task.getName()))) {
            configuration.getLoginConfigurations().add(SyncProxyConfigurationWriterAndReader.createDefaultLoginConfiguration(task.getName()));
            modified = true;
        }

        if (configuration.getTabListConfigurations().stream()
                .noneMatch(tabListConfiguration -> tabListConfiguration.getTargetGroup().equals(task.getName()))) {
            configuration.getTabListConfigurations().add(SyncProxyConfigurationWriterAndReader.createDefaultTabListConfiguration(task.getName()));
            modified = true;
        }

        if (modified) {
            SyncProxyConfigurationWriterAndReader.write(configuration, CloudNetSyncProxyModule.getInstance().getConfigurationFile());
        }

    }

}
