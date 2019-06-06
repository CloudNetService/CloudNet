package de.dytanic.cloudnet.driver.network.cluster;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;

import java.lang.reflect.Type;
import java.util.Collection;

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

    public String toString() {
        return "NetworkClusterNodeInfoSnapshot(creationTime=" + this.getCreationTime() + ", node=" + this.getNode() + ", version=" + this.getVersion() + ", currentServicesCount=" + this.getCurrentServicesCount() + ", usedMemory=" + this.getUsedMemory() + ", reservedMemory=" + this.getReservedMemory() + ", maxMemory=" + this.getMaxMemory() + ", processSnapshot=" + this.getProcessSnapshot() + ", extensions=" + this.getExtensions() + ")";
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof NetworkClusterNodeInfoSnapshot)) return false;
        final NetworkClusterNodeInfoSnapshot other = (NetworkClusterNodeInfoSnapshot) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.getCreationTime() != other.getCreationTime()) return false;
        final Object this$node = this.getNode();
        final Object other$node = other.getNode();
        if (this$node == null ? other$node != null : !this$node.equals(other$node)) return false;
        final Object this$version = this.getVersion();
        final Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
        if (this.getCurrentServicesCount() != other.getCurrentServicesCount()) return false;
        if (this.getUsedMemory() != other.getUsedMemory()) return false;
        if (this.getReservedMemory() != other.getReservedMemory()) return false;
        if (this.getMaxMemory() != other.getMaxMemory()) return false;
        final Object this$processSnapshot = this.getProcessSnapshot();
        final Object other$processSnapshot = other.getProcessSnapshot();
        if (this$processSnapshot == null ? other$processSnapshot != null : !this$processSnapshot.equals(other$processSnapshot))
            return false;
        final Object this$extensions = this.getExtensions();
        final Object other$extensions = other.getExtensions();
        if (this$extensions == null ? other$extensions != null : !this$extensions.equals(other$extensions))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof NetworkClusterNodeInfoSnapshot;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $creationTime = this.getCreationTime();
        result = result * PRIME + (int) ($creationTime >>> 32 ^ $creationTime);
        final Object $node = this.getNode();
        result = result * PRIME + ($node == null ? 43 : $node.hashCode());
        final Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        result = result * PRIME + this.getCurrentServicesCount();
        result = result * PRIME + this.getUsedMemory();
        result = result * PRIME + this.getReservedMemory();
        result = result * PRIME + this.getMaxMemory();
        final Object $processSnapshot = this.getProcessSnapshot();
        result = result * PRIME + ($processSnapshot == null ? 43 : $processSnapshot.hashCode());
        final Object $extensions = this.getExtensions();
        result = result * PRIME + ($extensions == null ? 43 : $extensions.hashCode());
        return result;
    }
}