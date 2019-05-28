package de.dytanic.cloudnet.ext.smart.listener;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.event.cluster.NetworkChannelAuthClusterNodeSuccessEvent;
import de.dytanic.cloudnet.event.service.CloudServicePostDeleteEvent;
import de.dytanic.cloudnet.ext.smart.CloudNetSmartModule;
import de.dytanic.cloudnet.ext.smart.util.SmartServiceTaskConfig;
import java.lang.reflect.Type;
import java.util.Collection;

public final class CloudServiceListener {

  private static final Type TYPE = new TypeToken<Collection<SmartServiceTaskConfig>>() {
  }.getType();

  @EventListener
  public void handle(ChannelMessageReceiveEvent event) {
    if (event.getChannel().equals("cloudnet_smart_module") && event.getMessage()
      .equals("update_configuration")) {
      Collection<SmartServiceTaskConfig> configs = event.getData()
        .get("smartServiceTaskConfiguration", TYPE);
      CloudNetSmartModule.getInstance()
        .setSmartServiceTaskConfigurations(configs);
    }
  }

  @EventListener
  public void handle(NetworkChannelAuthClusterNodeSuccessEvent event) {
    CloudNetSmartModule.getInstance()
      .publishUpdateConfiguration(event.getChannel());
  }

  @EventListener
  public void handle(CloudServicePostDeleteEvent event) {
    CloudNetSmartModule.getInstance().getProvidedSmartServices()
      .remove(event.getCloudService().getServiceId().getUniqueId());
  }
}