package de.dytanic.cloudnet.ext.bridge;

import java.util.List;

public class ProxyFallbackConfiguration {

    protected String targetGroup, defaultFallbackTask;

    protected List<ProxyFallback> fallbacks;

    public ProxyFallbackConfiguration(String targetGroup, String defaultFallbackTask, List<ProxyFallback> fallbacks) {
        this.targetGroup = targetGroup;
        this.defaultFallbackTask = defaultFallbackTask;
        this.fallbacks = fallbacks;
    }

    public String getTargetGroup() {
        return this.targetGroup;
    }

    public String getDefaultFallbackTask() {
        return this.defaultFallbackTask;
    }

    public List<ProxyFallback> getFallbacks() {
        return this.fallbacks;
    }

    public void setTargetGroup(String targetGroup) {
        this.targetGroup = targetGroup;
    }

    public void setDefaultFallbackTask(String defaultFallbackTask) {
        this.defaultFallbackTask = defaultFallbackTask;
    }

    public void setFallbacks(List<ProxyFallback> fallbacks) {
        this.fallbacks = fallbacks;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ProxyFallbackConfiguration)) return false;
        final ProxyFallbackConfiguration other = (ProxyFallbackConfiguration) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$targetGroup = this.getTargetGroup();
        final Object other$targetGroup = other.getTargetGroup();
        if (this$targetGroup == null ? other$targetGroup != null : !this$targetGroup.equals(other$targetGroup))
            return false;
        final Object this$defaultFallbackTask = this.getDefaultFallbackTask();
        final Object other$defaultFallbackTask = other.getDefaultFallbackTask();
        if (this$defaultFallbackTask == null ? other$defaultFallbackTask != null : !this$defaultFallbackTask.equals(other$defaultFallbackTask))
            return false;
        final Object this$fallbacks = this.getFallbacks();
        final Object other$fallbacks = other.getFallbacks();
        if (this$fallbacks == null ? other$fallbacks != null : !this$fallbacks.equals(other$fallbacks)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ProxyFallbackConfiguration;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $targetGroup = this.getTargetGroup();
        result = result * PRIME + ($targetGroup == null ? 43 : $targetGroup.hashCode());
        final Object $defaultFallbackTask = this.getDefaultFallbackTask();
        result = result * PRIME + ($defaultFallbackTask == null ? 43 : $defaultFallbackTask.hashCode());
        final Object $fallbacks = this.getFallbacks();
        result = result * PRIME + ($fallbacks == null ? 43 : $fallbacks.hashCode());
        return result;
    }

    public String toString() {
        return "ProxyFallbackConfiguration(targetGroup=" + this.getTargetGroup() + ", defaultFallbackTask=" + this.getDefaultFallbackTask() + ", fallbacks=" + this.getFallbacks() + ")";
    }
}