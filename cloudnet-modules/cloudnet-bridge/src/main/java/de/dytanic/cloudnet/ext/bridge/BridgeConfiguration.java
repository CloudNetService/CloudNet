package de.dytanic.cloudnet.ext.bridge;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@ToString
@EqualsAndHashCode(callSuper = false)
public final class BridgeConfiguration extends BasicJsonDocPropertyable {

    public static final Type TYPE = new TypeToken<BridgeConfiguration>() {
    }.getType();

    private String prefix;
    private boolean onlyProxyProtection = true;
    private Collection<String> excludedOnlyProxyWalkableGroups = new ArrayList<>(), excludedGroups;
    private Collection<ProxyFallbackConfiguration> bungeeFallbackConfigurations;
    private Map<String, String> messages;
    private boolean logPlayerConnections = true;
    private boolean hubCommandEnabled = true;
    private Collection<String> hubCommandAliases;

    public BridgeConfiguration(String prefix, boolean onlyProxyProtection, Collection<String> excludedOnlyProxyWalkableGroups, Collection<String> excludedGroups,
                               Collection<ProxyFallbackConfiguration> bungeeFallbackConfigurations, Map<String, String> messages,
                               boolean logPlayerConnections, boolean hubCommandEnabled, Collection<String> hubCommandAliases) {
        this.prefix = prefix;
        this.onlyProxyProtection = onlyProxyProtection;
        this.excludedOnlyProxyWalkableGroups = excludedOnlyProxyWalkableGroups;
        this.excludedGroups = excludedGroups;
        this.bungeeFallbackConfigurations = bungeeFallbackConfigurations;
        this.messages = messages;
        this.hubCommandEnabled = hubCommandEnabled;
        this.hubCommandAliases = hubCommandAliases;
    }

    public BridgeConfiguration() {
    }

    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public boolean isOnlyProxyProtection() {
        return this.onlyProxyProtection;
    }

    public void setOnlyProxyProtection(boolean onlyProxyProtection) {
        this.onlyProxyProtection = onlyProxyProtection;
    }

    public Collection<String> getExcludedOnlyProxyWalkableGroups() {
        return this.excludedOnlyProxyWalkableGroups;
    }

    public void setExcludedOnlyProxyWalkableGroups(Collection<String> excludedOnlyProxyWalkableGroups) {
        this.excludedOnlyProxyWalkableGroups = excludedOnlyProxyWalkableGroups;
    }

    public Collection<String> getExcludedGroups() {
        return this.excludedGroups;
    }

    public void setExcludedGroups(Collection<String> excludedGroups) {
        this.excludedGroups = excludedGroups;
    }

    public Collection<ProxyFallbackConfiguration> getBungeeFallbackConfigurations() {
        return this.bungeeFallbackConfigurations;
    }

    public void setBungeeFallbackConfigurations(Collection<ProxyFallbackConfiguration> bungeeFallbackConfigurations) {
        this.bungeeFallbackConfigurations = bungeeFallbackConfigurations;
    }

    public Map<String, String> getMessages() {
        return this.messages;
    }

    public void setMessages(Map<String, String> messages) {
        this.messages = messages;
    }

    public boolean isLogPlayerConnections() {
        return this.logPlayerConnections;
    }

    public void setLogPlayerConnections(boolean logPlayerConnections) {
        this.logPlayerConnections = logPlayerConnections;
    }

    public Collection<String> getLobbyCommandAliases() {
        return hubCommandAliases;
    }

    public boolean isHubCommandEnabled() {
        return hubCommandEnabled;
    }
}