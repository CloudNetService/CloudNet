package de.dytanic.cloudnet.ext.syncproxy;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

public class SyncProxyConfiguration {

    public static final Type TYPE = new TypeToken<SyncProxyConfiguration>() {
    }.getType();

    protected Collection<SyncProxyProxyLoginConfiguration> loginConfigurations;

    protected Collection<SyncProxyTabListConfiguration> tabListConfigurations;

    protected Map<String, String> messages;

    public SyncProxyConfiguration(Collection<SyncProxyProxyLoginConfiguration> loginConfigurations, Collection<SyncProxyTabListConfiguration> tabListConfigurations, Map<String, String> messages) {
        this.loginConfigurations = loginConfigurations;
        this.tabListConfigurations = tabListConfigurations;
        this.messages = messages;
    }

    public SyncProxyConfiguration() {
    }

    public Collection<SyncProxyProxyLoginConfiguration> getLoginConfigurations() {
        return this.loginConfigurations;
    }

    public Collection<SyncProxyTabListConfiguration> getTabListConfigurations() {
        return this.tabListConfigurations;
    }

    public Map<String, String> getMessages() {
        return this.messages;
    }

    public void setLoginConfigurations(Collection<SyncProxyProxyLoginConfiguration> loginConfigurations) {
        this.loginConfigurations = loginConfigurations;
    }

    public void setTabListConfigurations(Collection<SyncProxyTabListConfiguration> tabListConfigurations) {
        this.tabListConfigurations = tabListConfigurations;
    }

    public void setMessages(Map<String, String> messages) {
        this.messages = messages;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof SyncProxyConfiguration)) return false;
        final SyncProxyConfiguration other = (SyncProxyConfiguration) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$loginConfigurations = this.getLoginConfigurations();
        final Object other$loginConfigurations = other.getLoginConfigurations();
        if (this$loginConfigurations == null ? other$loginConfigurations != null : !this$loginConfigurations.equals(other$loginConfigurations))
            return false;
        final Object this$tabListConfigurations = this.getTabListConfigurations();
        final Object other$tabListConfigurations = other.getTabListConfigurations();
        if (this$tabListConfigurations == null ? other$tabListConfigurations != null : !this$tabListConfigurations.equals(other$tabListConfigurations))
            return false;
        final Object this$messages = this.getMessages();
        final Object other$messages = other.getMessages();
        if (this$messages == null ? other$messages != null : !this$messages.equals(other$messages)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof SyncProxyConfiguration;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $loginConfigurations = this.getLoginConfigurations();
        result = result * PRIME + ($loginConfigurations == null ? 43 : $loginConfigurations.hashCode());
        final Object $tabListConfigurations = this.getTabListConfigurations();
        result = result * PRIME + ($tabListConfigurations == null ? 43 : $tabListConfigurations.hashCode());
        final Object $messages = this.getMessages();
        result = result * PRIME + ($messages == null ? 43 : $messages.hashCode());
        return result;
    }

    public String toString() {
        return "SyncProxyConfiguration(loginConfigurations=" + this.getLoginConfigurations() + ", tabListConfigurations=" + this.getTabListConfigurations() + ", messages=" + this.getMessages() + ")";
    }
}