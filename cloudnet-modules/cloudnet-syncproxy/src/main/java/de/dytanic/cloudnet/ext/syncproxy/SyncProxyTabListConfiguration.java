package de.dytanic.cloudnet.ext.syncproxy;

import java.util.List;

public class SyncProxyTabListConfiguration {

    protected String targetGroup;

    protected List<SyncProxyTabList> entries;

    protected int animationsPerSecond;

    public SyncProxyTabListConfiguration(String targetGroup, List<SyncProxyTabList> entries, int animationsPerSecond) {
        this.targetGroup = targetGroup;
        this.entries = entries;
        this.animationsPerSecond = animationsPerSecond;
    }

    public SyncProxyTabListConfiguration() {
    }

    public String getTargetGroup() {
        return this.targetGroup;
    }

    public List<SyncProxyTabList> getEntries() {
        return this.entries;
    }

    public int getAnimationsPerSecond() {
        return this.animationsPerSecond;
    }

    public void setTargetGroup(String targetGroup) {
        this.targetGroup = targetGroup;
    }

    public void setEntries(List<SyncProxyTabList> entries) {
        this.entries = entries;
    }

    public void setAnimationsPerSecond(int animationsPerSecond) {
        this.animationsPerSecond = animationsPerSecond;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof SyncProxyTabListConfiguration)) return false;
        final SyncProxyTabListConfiguration other = (SyncProxyTabListConfiguration) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$targetGroup = this.getTargetGroup();
        final Object other$targetGroup = other.getTargetGroup();
        if (this$targetGroup == null ? other$targetGroup != null : !this$targetGroup.equals(other$targetGroup))
            return false;
        final Object this$entries = this.getEntries();
        final Object other$entries = other.getEntries();
        if (this$entries == null ? other$entries != null : !this$entries.equals(other$entries)) return false;
        if (this.getAnimationsPerSecond() != other.getAnimationsPerSecond()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof SyncProxyTabListConfiguration;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $targetGroup = this.getTargetGroup();
        result = result * PRIME + ($targetGroup == null ? 43 : $targetGroup.hashCode());
        final Object $entries = this.getEntries();
        result = result * PRIME + ($entries == null ? 43 : $entries.hashCode());
        result = result * PRIME + this.getAnimationsPerSecond();
        return result;
    }

    public String toString() {
        return "SyncProxyTabListConfiguration(targetGroup=" + this.getTargetGroup() + ", entries=" + this.getEntries() + ", animationsPerSecond=" + this.getAnimationsPerSecond() + ")";
    }
}