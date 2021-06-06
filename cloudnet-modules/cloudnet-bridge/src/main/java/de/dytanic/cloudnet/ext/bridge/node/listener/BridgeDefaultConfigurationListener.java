package de.dytanic.cloudnet.ext.bridge.node.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.event.service.task.ServiceTaskAddEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.node.CloudNetBridgeModule;

public class BridgeDefaultConfigurationListener {

  @EventListener
  public void handleTaskAdd(ServiceTaskAddEvent event) {
    ServiceTask task = event.getTask();

    if (!task.getProcessConfiguration().getEnvironment().isMinecraftJavaProxy() &&
      !task.getProcessConfiguration().getEnvironment().isMinecraftBedrockProxy()) {
      return;
    }

    BridgeConfiguration configuration = CloudNetBridgeModule.getInstance().getBridgeConfiguration();
    if (configuration.getBungeeFallbackConfigurations().stream()
      .noneMatch(proxyFallbackConfiguration -> proxyFallbackConfiguration.getTargetGroup().equals(task.getName()))) {
      configuration.getBungeeFallbackConfigurations().add(
        CloudNetBridgeModule.getInstance().createDefaultFallbackConfiguration(task.getName())
      );
      CloudNetBridgeModule.getInstance().writeConfiguration(configuration);
    }
  }

}
