package de.dytanic.cloudnet.driver.network.cluster;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.driver.module.ModuleConfiguration;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.driver.serialization.json.SerializableJsonDocPropertyable;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Collection;

@ToString
@EqualsAndHashCode(callSuper = false)
public class NetworkClusterNodeInfoSnapshot extends SerializableJsonDocPropertyable implements SerializableObject {

    public static final Type TYPE = new TypeToken<NetworkClusterNodeInfoSnapshot>() {
    }.getType();

    protected long creationTime;
    protected long startupNanos;
    protected NetworkClusterNode node;
    protected String version;
    protected int currentServicesCount;
    protected int usedMemory;
    protected int reservedMemory;
    protected int maxMemory;
    protected ProcessSnapshot processSnapshot;
    protected Collection<ModuleConfiguration> modules;
    private double systemCpuUsage;

    /**
     * @deprecated Use {@link #NetworkClusterNodeInfoSnapshot(long, long, NetworkClusterNode, String, int, int, int, int, ProcessSnapshot, Collection, double)} instead
     */
    @Deprecated
    public NetworkClusterNodeInfoSnapshot(long creationTime,
                                          NetworkClusterNode node,
                                          String version,
                                          int currentServicesCount,
                                          int usedMemory,
                                          int reservedMemory,
                                          int maxMemory,
                                          ProcessSnapshot processSnapshot,
                                          Collection<ModuleConfiguration> modules,
                                          double systemCpuUsage) {
        this(
                creationTime,
                System.nanoTime(),
                node,
                version,
                currentServicesCount,
                usedMemory,
                reservedMemory,
                maxMemory,
                processSnapshot,
                modules,
                systemCpuUsage
        );
    }

    public NetworkClusterNodeInfoSnapshot(long creationTime,
                                          long startupNanos,
                                          NetworkClusterNode node,
                                          String version,
                                          int currentServicesCount,
                                          int usedMemory,
                                          int reservedMemory,
                                          int maxMemory,
                                          ProcessSnapshot processSnapshot,
                                          Collection<ModuleConfiguration> modules,
                                          double systemCpuUsage) {
        this.creationTime = creationTime;
        this.startupNanos = startupNanos;
        this.node = node;
        this.version = version;
        this.currentServicesCount = currentServicesCount;
        this.usedMemory = usedMemory;
        this.reservedMemory = reservedMemory;
        this.maxMemory = maxMemory;
        this.processSnapshot = processSnapshot;
        this.modules = modules;
        this.systemCpuUsage = systemCpuUsage;
    }

    public NetworkClusterNodeInfoSnapshot() {
    }

    public long getCreationTime() {
        return this.creationTime;
    }

    public long getStartupNanos() {
        return this.startupNanos;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public NetworkClusterNode getNode() {
        return this.node;
    }

    public void setNode(NetworkClusterNode node) {
        this.node = node;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getCurrentServicesCount() {
        return this.currentServicesCount;
    }

    public void setCurrentServicesCount(int currentServicesCount) {
        this.currentServicesCount = currentServicesCount;
    }

    public int getUsedMemory() {
        return this.usedMemory;
    }

    public void setUsedMemory(int usedMemory) {
        this.usedMemory = usedMemory;
    }

    public int getReservedMemory() {
        return this.reservedMemory;
    }

    public void setReservedMemory(int reservedMemory) {
        this.reservedMemory = reservedMemory;
    }

    public int getMaxMemory() {
        return this.maxMemory;
    }

    public void setMaxMemory(int maxMemory) {
        this.maxMemory = maxMemory;
    }

    public ProcessSnapshot getProcessSnapshot() {
        return this.processSnapshot;
    }

    public void setProcessSnapshot(ProcessSnapshot processSnapshot) {
        this.processSnapshot = processSnapshot;
    }

    public Collection<ModuleConfiguration> getModules() {
        return this.modules;
    }

    public void setModules(Collection<ModuleConfiguration> modules) {
        this.modules = modules;
    }

    public double getSystemCpuUsage() {
        return this.systemCpuUsage;
    }

    public void setSystemCpuUsage(double systemCpuUsage) {
        this.systemCpuUsage = systemCpuUsage;
    }

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeLong(this.creationTime);
        buffer.writeLong(this.startupNanos);
        buffer.writeObject(this.node);
        buffer.writeString(this.version);
        buffer.writeInt(this.currentServicesCount);
        buffer.writeInt(this.usedMemory);
        buffer.writeInt(this.reservedMemory);
        buffer.writeInt(this.maxMemory);
        buffer.writeObject(this.processSnapshot);
        buffer.writeObjectCollection(this.modules);
        buffer.writeDouble(this.systemCpuUsage);

        super.write(buffer);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.creationTime = buffer.readLong();
        this.startupNanos = buffer.readLong();
        this.node = buffer.readObject(NetworkClusterNode.class);
        this.version = buffer.readString();
        this.currentServicesCount = buffer.readInt();
        this.usedMemory = buffer.readInt();
        this.reservedMemory = buffer.readInt();
        this.maxMemory = buffer.readInt();
        this.processSnapshot = buffer.readObject(ProcessSnapshot.class);
        this.modules = buffer.readObjectCollection(ModuleConfiguration.class);
        this.systemCpuUsage = buffer.readDouble();

        super.read(buffer);
    }
}