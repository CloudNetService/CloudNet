package de.dytanic.cloudnet.driver.network.cluster;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.lang.reflect.Type;
import java.util.Collection;

@ToString
@EqualsAndHashCode(callSuper = false)
public class NetworkClusterNodeInfoSnapshot extends BasicJsonDocPropertyable {

    public static final Type TYPE = new TypeToken<NetworkClusterNodeInfoSnapshot>() {
    }.getType();

    protected long creationTime;

    protected NetworkClusterNode node;

    protected String version;

    protected int currentServicesCount, usedMemory, reservedMemory, maxMemory;
    protected ProcessSnapshot processSnapshot;
    protected Collection<NetworkClusterNodeExtensionSnapshot> extensions;
    private double systemCpuUsage;

    public NetworkClusterNodeInfoSnapshot(long creationTime, NetworkClusterNode node, String version, int currentServicesCount, int usedMemory, int reservedMemory, int maxMemory, ProcessSnapshot processSnapshot, Collection<NetworkClusterNodeExtensionSnapshot> extensions, double systemCpuUsage) {
        this.creationTime = creationTime;
        this.node = node;
        this.version = version;
        this.currentServicesCount = currentServicesCount;
        this.usedMemory = usedMemory;
        this.reservedMemory = reservedMemory;
        this.maxMemory = maxMemory;
        this.processSnapshot = processSnapshot;
        this.extensions = extensions;
        this.systemCpuUsage = systemCpuUsage;
    }

    public NetworkClusterNodeInfoSnapshot() {
    }

    public long getCreationTime() {
        return this.creationTime;
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

    public Collection<NetworkClusterNodeExtensionSnapshot> getExtensions() {
        return this.extensions;
    }

    public void setExtensions(Collection<NetworkClusterNodeExtensionSnapshot> extensions) {
        this.extensions = extensions;
    }

    public double getSystemCpuUsage() {
        return this.systemCpuUsage;
    }

    public void setSystemCpuUsage(double systemCpuUsage) {
        this.systemCpuUsage = systemCpuUsage;
    }
}