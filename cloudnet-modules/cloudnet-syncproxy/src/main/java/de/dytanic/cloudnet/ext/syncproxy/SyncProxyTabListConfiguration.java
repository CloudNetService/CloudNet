package de.dytanic.cloudnet.ext.syncproxy;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@ToString
@EqualsAndHashCode
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

    public void setTargetGroup(String targetGroup) {
        this.targetGroup = targetGroup;
    }

    public List<SyncProxyTabList> getEntries() {
        return this.entries;
    }

    public void setEntries(List<SyncProxyTabList> entries) {
        this.entries = entries;
    }

    public int getAnimationsPerSecond() {
        return this.animationsPerSecond;
    }

    public void setAnimationsPerSecond(int animationsPerSecond) {
        this.animationsPerSecond = animationsPerSecond;
    }

}