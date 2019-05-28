package de.dytanic.cloudnet.ext.syncproxy.node.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.event.service.CloudServicePreStartEvent;
import java.io.File;

public final class IncludePluginListener {

  @EventListener
  public void handle(CloudServicePreStartEvent event) {
    if (!event.getCloudService().getServiceId().getEnvironment()
        .isMinecraftJavaProxy() &&
        !event.getCloudService().getServiceId().getEnvironment()
            .isMinecraftBedrockProxy()) {
      return;
    }

    new File(event.getCloudService().getDirectory(), "plugins").mkdirs();
    File file = new File(event.getCloudService().getDirectory(),
        "plugins/cloudnet-syncproxy.jar");
    file.delete();

    if (DefaultModuleHelper
        .copyCurrentModuleInstanceFromClass(IncludePluginListener.class,
            file)) {
      DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(
          IncludePluginListener.class,
          event.getCloudService().getServiceConfiguration().getProcessConfig()
              .getEnvironment(), file);
    }
  }
}