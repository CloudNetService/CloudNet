package de.dytanic.cloudnet.ext.syncproxy.configuration;

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

    private boolean ingameServiceStartStopMessages = true;

    public SyncProxyConfiguration(Collection<SyncProxyProxyLoginConfiguration> loginConfigurations, Collection<SyncProxyTabListConfiguration> tabListConfigurations, Map<String, String> messages, boolean ingameServiceStartStopMessages) {
        this.loginConfigurations = loginConfigurations;
        this.tabListConfigurations = tabListConfigurations;
        this.messages = messages;
        this.ingameServiceStartStopMessages = ingameServiceStartStopMessages;
    }

    public SyncProxyConfiguration() {
    }

    public Collection<SyncProxyProxyLoginConfiguration> getLoginConfigurations() {
        return this.loginConfigurations;
    }

    public void setLoginConfigurations(Collection<SyncProxyProxyLoginConfiguration> loginConfigurations) {
        this.loginConfigurations = loginConfigurations;
    }

    public Collection<SyncProxyTabListConfiguration> getTabListConfigurations() {
        return this.tabListConfigurations;
    }

    public void setTabListConfigurations(Collection<SyncProxyTabListConfiguration> tabListConfigurations) {
        this.tabListConfigurations = tabListConfigurations;
    }

    public Map<String, String> getMessages() {
        return this.messages;
    }

    public void setMessages(Map<String, String> messages) {
        this.messages = messages;
    }

    public boolean showIngameServicesStartStopMessages() {
        return ingameServiceStartStopMessages;
    }

}