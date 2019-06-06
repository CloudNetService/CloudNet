package de.dytanic.cloudnet.ext.bridge;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

public final class BridgeConfiguration extends BasicJsonDocPropertyable {

    public static final Type TYPE = new TypeToken<BridgeConfiguration>() {
    }.getType();

    private String prefix;

    private Collection<String> excludedOnlyProxyWalkableGroups, excludedGroups;

    private Collection<ProxyFallbackConfiguration> bungeeFallbackConfigurations;

    private Map<String, String> messages;

    private boolean logPlayerConnections = true;

    public BridgeConfiguration(String prefix, Collection<String> excludedOnlyProxyWalkableGroups, Collection<String> excludedGroups, Collection<ProxyFallbackConfiguration> bungeeFallbackConfigurations, Map<String, String> messages, boolean logPlayerConnections) {
        this.prefix = prefix;
        this.excludedOnlyProxyWalkableGroups = excludedOnlyProxyWalkableGroups;
        this.excludedGroups = excludedGroups;
        this.bungeeFallbackConfigurations = bungeeFallbackConfigurations;
        this.messages = messages;
        this.logPlayerConnections = logPlayerConnections;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public Collection<String> getExcludedOnlyProxyWalkableGroups() {
        return this.excludedOnlyProxyWalkableGroups;
    }

    public Collection<String> getExcludedGroups() {
        return this.excludedGroups;
    }

    public Collection<ProxyFallbackConfiguration> getBungeeFallbackConfigurations() {
        return this.bungeeFallbackConfigurations;
    }

    public Map<String, String> getMessages() {
        return this.messages;
    }

    public boolean isLogPlayerConnections() {
        return this.logPlayerConnections;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setExcludedOnlyProxyWalkableGroups(Collection<String> excludedOnlyProxyWalkableGroups) {
        this.excludedOnlyProxyWalkableGroups = excludedOnlyProxyWalkableGroups;
    }

    public void setExcludedGroups(Collection<String> excludedGroups) {
        this.excludedGroups = excludedGroups;
    }

    public void setBungeeFallbackConfigurations(Collection<ProxyFallbackConfiguration> bungeeFallbackConfigurations) {
        this.bungeeFallbackConfigurations = bungeeFallbackConfigurations;
    }

    public void setMessages(Map<String, String> messages) {
        this.messages = messages;
    }

    public void setLogPlayerConnections(boolean logPlayerConnections) {
        this.logPlayerConnections = logPlayerConnections;
    }

    public String toString() {
        return "BridgeConfiguration(prefix=" + this.getPrefix() + ", excludedOnlyProxyWalkableGroups=" + this.getExcludedOnlyProxyWalkableGroups() + ", excludedGroups=" + this.getExcludedGroups() + ", bungeeFallbackConfigurations=" + this.getBungeeFallbackConfigurations() + ", messages=" + this.getMessages() + ", logPlayerConnections=" + this.isLogPlayerConnections() + ")";
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof BridgeConfiguration)) return false;
        final BridgeConfiguration other = (BridgeConfiguration) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$prefix = this.getPrefix();
        final Object other$prefix = other.getPrefix();
        if (this$prefix == null ? other$prefix != null : !this$prefix.equals(other$prefix)) return false;
        final Object this$excludedOnlyProxyWalkableGroups = this.getExcludedOnlyProxyWalkableGroups();
        final Object other$excludedOnlyProxyWalkableGroups = other.getExcludedOnlyProxyWalkableGroups();
        if (this$excludedOnlyProxyWalkableGroups == null ? other$excludedOnlyProxyWalkableGroups != null : !this$excludedOnlyProxyWalkableGroups.equals(other$excludedOnlyProxyWalkableGroups))
            return false;
        final Object this$excludedGroups = this.getExcludedGroups();
        final Object other$excludedGroups = other.getExcludedGroups();
        if (this$excludedGroups == null ? other$excludedGroups != null : !this$excludedGroups.equals(other$excludedGroups))
            return false;
        final Object this$bungeeFallbackConfigurations = this.getBungeeFallbackConfigurations();
        final Object other$bungeeFallbackConfigurations = other.getBungeeFallbackConfigurations();
        if (this$bungeeFallbackConfigurations == null ? other$bungeeFallbackConfigurations != null : !this$bungeeFallbackConfigurations.equals(other$bungeeFallbackConfigurations))
            return false;
        final Object this$messages = this.getMessages();
        final Object other$messages = other.getMessages();
        if (this$messages == null ? other$messages != null : !this$messages.equals(other$messages)) return false;
        if (this.isLogPlayerConnections() != other.isLogPlayerConnections()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof BridgeConfiguration;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $prefix = this.getPrefix();
        result = result * PRIME + ($prefix == null ? 43 : $prefix.hashCode());
        final Object $excludedOnlyProxyWalkableGroups = this.getExcludedOnlyProxyWalkableGroups();
        result = result * PRIME + ($excludedOnlyProxyWalkableGroups == null ? 43 : $excludedOnlyProxyWalkableGroups.hashCode());
        final Object $excludedGroups = this.getExcludedGroups();
        result = result * PRIME + ($excludedGroups == null ? 43 : $excludedGroups.hashCode());
        final Object $bungeeFallbackConfigurations = this.getBungeeFallbackConfigurations();
        result = result * PRIME + ($bungeeFallbackConfigurations == null ? 43 : $bungeeFallbackConfigurations.hashCode());
        final Object $messages = this.getMessages();
        result = result * PRIME + ($messages == null ? 43 : $messages.hashCode());
        result = result * PRIME + (this.isLogPlayerConnections() ? 79 : 97);
        return result;
    }
}