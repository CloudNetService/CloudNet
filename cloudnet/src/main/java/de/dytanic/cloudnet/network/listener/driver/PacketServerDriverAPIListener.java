package de.dytanic.cloudnet.network.listener.driver;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.api.DriverAPICategory;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;

import java.util.HashMap;
import java.util.Map;

public class PacketServerDriverAPIListener implements IPacketListener {

    private final Map<DriverAPICategory, CategorizedDriverAPIListener> listeners = new HashMap<>();

    public PacketServerDriverAPIListener() {
        this.registerListener(new DriverPermissionManagementListener());

        this.registerListener(new DriverSpecificServiceListener());
        this.registerListener(new DriverGeneralServiceListener());
        this.registerListener(new DriverServiceFactoryListener());

        this.registerListener(new DriverServiceTaskListener());
        this.registerListener(new DriverGroupListener());

        this.registerListener(new DriverNodeInfoListener());
        this.registerListener(new DriverTemplateStorageListener());

        int handlerCount = this.listeners.values().stream().mapToInt(CategorizedDriverAPIListener::getHandlerCount).sum();
        Preconditions.checkArgument(handlerCount == DriverAPIRequestType.values().length, "Invalid handlers registered");
    }

    private void registerListener(CategorizedDriverAPIListener listener) {
        this.listeners.put(listener.getCategory(), listener);
    }

    @Override
    public void handle(INetworkChannel channel, IPacket packet) {
        ProtocolBuffer input = packet.getBuffer();
        DriverAPIRequestType requestType = input.readEnumConstant(DriverAPIRequestType.class);

        CategorizedDriverAPIListener listener = this.listeners.get(requestType.getCategory());
        Preconditions.checkNotNull(listener, "No listener for category " + requestType.getCategory() + " found");

        listener.handleDriverRequest(requestType, channel, packet);
    }

}
