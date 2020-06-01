package de.dytanic.cloudnet.ext.syncproxy.configuration;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConstants;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ToString
@EqualsAndHashCode
public class SyncProxyConfiguration {

    public static SyncProxyConfiguration getConfigurationFromNode() {
        ITask<SyncProxyConfiguration> task = CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacket(CloudNetDriver.getInstance().getNetworkClient().getFirstChannel(),
                SyncProxyConstants.SYNC_PROXY_SYNC_CHANNEL_PROPERTY,
                SyncProxyConstants.SIGN_CHANNEL_SYNC_ID_GET_SYNC_PROXY_CONFIGURATION_PROPERTY,
                new JsonDocument(),
                documentPair -> documentPair.get("syncProxyConfiguration", SyncProxyConfiguration.TYPE));

        try {
            return task.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    public static void updateSyncProxyConfigurationInNetwork(@NotNull SyncProxyConfiguration syncProxyConfiguration) {
        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                SyncProxyConstants.SYNC_PROXY_CHANNEL_NAME,
                SyncProxyConstants.SYNC_PROXY_UPDATE_CONFIGURATION,
                new JsonDocument(
                        "syncProxyConfiguration",
                        syncProxyConfiguration
                )
        );
    }

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
        return this.ingameServiceStartStopMessages;
    }

}