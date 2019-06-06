package de.dytanic.cloudnet.ext.smart;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class CloudNetServiceSmartProfile extends BasicJsonDocPropertyable {

    private final UUID uniqueId;

    private final AtomicInteger autoStopCount;

    public CloudNetServiceSmartProfile(UUID uniqueId, AtomicInteger autoStopCount) {
        this.uniqueId = uniqueId;
        this.autoStopCount = autoStopCount;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public AtomicInteger getAutoStopCount() {
        return this.autoStopCount;
    }

    public String toString() {
        return "CloudNetServiceSmartProfile(uniqueId=" + this.getUniqueId() + ", autoStopCount=" + this.getAutoStopCount() + ")";
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof CloudNetServiceSmartProfile)) return false;
        final CloudNetServiceSmartProfile other = (CloudNetServiceSmartProfile) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$uniqueId = this.getUniqueId();
        final Object other$uniqueId = other.getUniqueId();
        if (this$uniqueId == null ? other$uniqueId != null : !this$uniqueId.equals(other$uniqueId)) return false;
        final Object this$autoStopCount = this.getAutoStopCount();
        final Object other$autoStopCount = other.getAutoStopCount();
        if (this$autoStopCount == null ? other$autoStopCount != null : !this$autoStopCount.equals(other$autoStopCount))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof CloudNetServiceSmartProfile;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $uniqueId = this.getUniqueId();
        result = result * PRIME + ($uniqueId == null ? 43 : $uniqueId.hashCode());
        final Object $autoStopCount = this.getAutoStopCount();
        result = result * PRIME + ($autoStopCount == null ? 43 : $autoStopCount.hashCode());
        return result;
    }
}