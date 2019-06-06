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

    public NetworkClusterNodeInfoSnapshot(long creationTime, NetworkClusterNode node, String version, int currentServicesCount, int usedMemory, int reservedMemory, int maxMemory, ProcessSnapshot processSnapshot, Collection<NetworkClusterNodeExtensionSnapshot> extensions) {
        this.creationTime = creationTime;
        this.node = node;
        this.version = version;
        this.currentServicesCount = currentServicesCount;
        this.usedMemory = usedMemory;
        this.reservedMemory = reservedMemory;
        this.maxMemory = maxMemory;
        this.processSnapshot = processSnapshot;
        this.extensions = extensions;
    }

    public NetworkClusterNodeInfoSnapshot() {
    }

    public long getCreationTime() {
        return this.creationTime;
    }

    public NetworkClusterNode getNode() {
        return this.node;
    }

    public String getVersion() {
        return this.version;
    }

    public int getCurrentServicesCount() {
        return this.currentServicesCount;
    }

    public int getUsedMemory() {
        return this.usedMemory;
    }

    public int getReservedMemory() {
        return this.reservedMemory;
    }

    public int getMaxMemory() {
        return this.maxMemory;
    }

    public ProcessSnapshot getProcessSnapshot() {
        return this.processSnapshot;
    }

    public Collection<NetworkClusterNodeExtensionSnapshot> getExtensions() {
        return this.extensions;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public void setNode(NetworkClusterNode node) {
        this.node = node;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setCurrentServicesCount(int currentServicesCount) {
        this.currentServicesCount = currentServicesCount;
    }

    public void setUsedMemory(int usedMemory) {
        this.usedMemory = usedMemory;
    }

    public void setReservedMemory(int reservedMemory) {
        this.reservedMemory = reservedMemory;
    }

    public void setMaxMemory(int maxMemory) {
        this.maxMemory = maxMemory;
    }

    public void setProcessSnapshot(ProcessSnapshot processSnapshot) {
        this.processSnapshot = processSnapshot;
    }

    public void setExtensions(Collection<NetworkClusterNodeExtensionSnapshot> extensions) {
        this.extensions = extensions;
    }

}