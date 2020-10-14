package de.dytanic.cloudnet.wrapper.network.listener;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.events.service.*;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerServiceInfoPublisher;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;

public final class PacketServerServiceInfoPublisherListener implements IPacketListener {

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        ServiceInfoSnapshot serviceInfoSnapshot = packet.getBuffer().readObject(ServiceInfoSnapshot.class);
        PacketClientServerServiceInfoPublisher.PublisherType publisherType = packet.getBuffer().readEnumConstant(PacketClientServerServiceInfoPublisher.PublisherType.class);

        switch (publisherType) {
            case UPDATE:
                this.invoke0(new CloudServiceInfoUpdateEvent(serviceInfoSnapshot));
                break;
            case REGISTER:
                this.invoke0(new CloudServiceRegisterEvent(serviceInfoSnapshot));
                break;
            case CONNECTED:
                this.invoke0(new CloudServiceConnectNetworkEvent(serviceInfoSnapshot));
                break;
            case UNREGISTER:
                this.invoke0(new CloudServiceUnregisterEvent(serviceInfoSnapshot));
                break;
            case DISCONNECTED:
                this.invoke0(new CloudServiceDisconnectNetworkEvent(serviceInfoSnapshot));
                break;
            case STARTED:
                this.invoke0(new CloudServiceStartEvent(serviceInfoSnapshot));
                break;
            case STOPPED:
                this.invoke0(new CloudServiceStopEvent(serviceInfoSnapshot));
                break;
        }
    }

    private void invoke0(Event event) {
        CloudNetDriver.getInstance().getEventManager().callEvent(event);
    }
}