package de.dytanic.cloudnet.ext.syncproxy;

import com.google.gson.reflect.TypeToken;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

@ToString
@EqualsAndHashCode
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

}