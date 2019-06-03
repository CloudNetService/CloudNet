package de.dytanic.cloudnet.network.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceConnectNetworkEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceDisconnectNetworkEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceRegisterEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStartEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStopEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceUnregisterEvent;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerServiceInfoPublisher;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.service.ICloudService;
import java.util.Map;
import java.util.UUID;

public final class PacketServerServiceInfoPublisherListener implements
    IPacketListener {

  @Override
  public void handle(INetworkChannel channel, IPacket packet) throws Exception {
    if (packet.getHeader().contains("serviceInfoSnapshot") && packet.getHeader()
        .contains("type")) {
      ServiceInfoSnapshot serviceInfoSnapshot = packet.getHeader()
          .get("serviceInfoSnapshot", ServiceInfoSnapshot.TYPE);
      PacketClientServerServiceInfoPublisher.PublisherType publisherType = packet
          .getHeader().get("type",
              PacketClientServerServiceInfoPublisher.PublisherType.class);

      if (serviceInfoSnapshot == null || publisherType == null) {
        return;
      }

      switch (publisherType) {
        case UPDATE:
          invokeEvent(new CloudServiceInfoUpdateEvent(serviceInfoSnapshot));
          getGlobalServiceInfoSnapshots()
              .put(serviceInfoSnapshot.getServiceId().getUniqueId(),
                  serviceInfoSnapshot);
          break;
        case REGISTER:
          invokeEvent(new CloudServiceRegisterEvent(serviceInfoSnapshot));
          getGlobalServiceInfoSnapshots()
              .put(serviceInfoSnapshot.getServiceId().getUniqueId(),
                  serviceInfoSnapshot);
          break;
        case CONNECTED:
          invokeEvent(new CloudServiceConnectNetworkEvent(serviceInfoSnapshot));
          getGlobalServiceInfoSnapshots()
              .put(serviceInfoSnapshot.getServiceId().getUniqueId(),
                  serviceInfoSnapshot);
          break;
        case DISCONNECTED:
          invokeEvent(
              new CloudServiceDisconnectNetworkEvent(serviceInfoSnapshot));
          getGlobalServiceInfoSnapshots()
              .put(serviceInfoSnapshot.getServiceId().getUniqueId(),
                  serviceInfoSnapshot);
          break;
        case UNREGISTER:
          invokeEvent(new CloudServiceUnregisterEvent(serviceInfoSnapshot));
          getGlobalServiceInfoSnapshots()
              .remove(serviceInfoSnapshot.getServiceId().getUniqueId());
          break;
        case STARTED:
          invokeEvent(new CloudServiceStartEvent(serviceInfoSnapshot));
          getGlobalServiceInfoSnapshots()
              .put(serviceInfoSnapshot.getServiceId().getUniqueId(),
                  serviceInfoSnapshot);
          break;
        case STOPPED:
          invokeEvent(new CloudServiceStopEvent(serviceInfoSnapshot));
          getGlobalServiceInfoSnapshots()
              .put(serviceInfoSnapshot.getServiceId().getUniqueId(),
                  serviceInfoSnapshot);
          break;
      }

      sendUpdateToAllServices(serviceInfoSnapshot, publisherType);
    }
  }

  private void invokeEvent(Event event) {
    CloudNetDriver.getInstance().getEventManager().callEvent(event);
  }

  private Map<UUID, ServiceInfoSnapshot> getGlobalServiceInfoSnapshots() {
    return CloudNet.getInstance().getCloudServiceManager()
        .getGlobalServiceInfoSnapshots();
  }

  private void sendUpdateToAllServices(ServiceInfoSnapshot serviceInfoSnapshot,
      PacketClientServerServiceInfoPublisher.PublisherType type) {
    for (ICloudService cloudService : CloudNet.getInstance()
        .getCloudServiceManager().getCloudServices().values()) {
      if (cloudService.getNetworkChannel() != null) {
        cloudService.getNetworkChannel().sendPacket(
            new PacketClientServerServiceInfoPublisher(serviceInfoSnapshot,
                type));
      }
    }
  }
}