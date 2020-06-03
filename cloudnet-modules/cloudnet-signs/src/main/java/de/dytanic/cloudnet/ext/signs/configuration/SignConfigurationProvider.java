package de.dytanic.cloudnet.ext.signs.configuration;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.ext.signs.SignConstants;
import de.dytanic.cloudnet.wrapper.Wrapper;

public final class SignConfigurationProvider {

    private static volatile SignConfiguration loadedConfiguration;

    private SignConfigurationProvider() {
        throw new UnsupportedOperationException();
    }

    public static void setLocal(SignConfiguration signConfiguration) {
        Preconditions.checkNotNull(signConfiguration);

        loadedConfiguration = signConfiguration;
    }

    public static SignConfiguration load() {
        if (loadedConfiguration == null) {
            loadedConfiguration = load0();
        }

        return loadedConfiguration;
    }

    private static SignConfiguration load0() {
        ChannelMessage response = ChannelMessage.builder()
                .channel(SignConstants.SIGN_CHANNEL_NAME)
                .message(SignConstants.SIGN_CHANNEL_GET_SIGNS_CONFIGURATION)
                .targetNode(Wrapper.getInstance().getServiceId().getNodeUniqueId())
                .build()
                .sendSingleQuery();

        if (response != null) {
            return response.getJson().get("signConfiguration", SignConfiguration.TYPE);
        }

        return null;
    }

}