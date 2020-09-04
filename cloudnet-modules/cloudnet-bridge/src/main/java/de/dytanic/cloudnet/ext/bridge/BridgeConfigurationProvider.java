package de.dytanic.cloudnet.ext.bridge;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.wrapper.Wrapper;

public final class BridgeConfigurationProvider {

    private static BridgeConfiguration loadedConfiguration;

    private BridgeConfigurationProvider() {
        throw new UnsupportedOperationException();
    }

    public static BridgeConfiguration update(BridgeConfiguration bridgeConfiguration) {
        Preconditions.checkNotNull(bridgeConfiguration);

        BridgeHelper.messageBuilder()
                .message(BridgeConstants.BRIDGE_NETWORK_CHANNEL_CLUSTER_MESSAGE_UPDATE_BRIDGE_CONFIGURATION_LISTENER)
                .json(JsonDocument.newDocument("bridgeConfiguration", bridgeConfiguration))
                .targetAll()
                .build()
                .send();
        loadedConfiguration = bridgeConfiguration;

        return bridgeConfiguration;
    }

    public static void setLocal(BridgeConfiguration bridgeConfiguration) {
        Preconditions.checkNotNull(bridgeConfiguration);

        loadedConfiguration = bridgeConfiguration;
    }

    public static BridgeConfiguration load() {
        if (loadedConfiguration == null) {
            loadedConfiguration = load0();
        }

        return loadedConfiguration;
    }

    private static BridgeConfiguration load0() {
        ChannelMessage response = BridgeHelper.messageBuilder()
                .message(BridgeConstants.BRIDGE_NETWORK_CHANNEL_MESSAGE_GET_BRIDGE_CONFIGURATION)
                .targetNode(Wrapper.getInstance().getServiceId().getNodeUniqueId())
                .build()
                .sendSingleQuery();

        return response != null ? response.getJson().get("bridgeConfig", BridgeConfiguration.TYPE) : null;
    }
}