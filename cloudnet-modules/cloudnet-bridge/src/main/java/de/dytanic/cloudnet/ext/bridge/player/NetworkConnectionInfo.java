package de.dytanic.cloudnet.ext.bridge.player;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.UUID;

@ToString
@EqualsAndHashCode
public class NetworkConnectionInfo implements SerializableObject {

    public static final Type TYPE = new TypeToken<NetworkConnectionInfo>() {
    }.getType();

    protected UUID uniqueId;

    protected String name;

    protected int version;

    protected HostAndPort address, listener;

    protected boolean onlineMode, legacy;

    protected NetworkServiceInfo networkService;

    public NetworkConnectionInfo(UUID uniqueId, String name, int version, HostAndPort address, HostAndPort listener, boolean onlineMode, boolean legacy, NetworkServiceInfo networkService) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.version = version;
        this.address = address;
        this.listener = listener;
        this.onlineMode = onlineMode;
        this.legacy = legacy;
        this.networkService = networkService;
    }

    public NetworkConnectionInfo() {
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public void setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getVersion() {
        return this.version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public HostAndPort getAddress() {
        return this.address;
    }

    public void setAddress(HostAndPort address) {
        this.address = address;
    }

    public HostAndPort getListener() {
        return this.listener;
    }

    public void setListener(HostAndPort listener) {
        this.listener = listener;
    }

    public boolean isOnlineMode() {
        return this.onlineMode;
    }

    public void setOnlineMode(boolean onlineMode) {
        this.onlineMode = onlineMode;
    }

    public boolean isLegacy() {
        return this.legacy;
    }

    public void setLegacy(boolean legacy) {
        this.legacy = legacy;
    }

    public NetworkServiceInfo getNetworkService() {
        return this.networkService;
    }

    public void setNetworkService(NetworkServiceInfo networkService) {
        this.networkService = networkService;
    }

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeUUID(this.uniqueId);
        buffer.writeString(this.name);
        buffer.writeInt(this.version);
        buffer.writeObject(this.address);
        buffer.writeObject(this.listener);
        buffer.writeObject(this.networkService);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.uniqueId = buffer.readUUID();
        this.name = buffer.readString();
        this.version = buffer.readInt();
        this.address = buffer.readObject(HostAndPort.class);
        this.listener = buffer.readObject(HostAndPort.class);
        this.networkService = buffer.readObject(NetworkServiceInfo.class);
    }

}