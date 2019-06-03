package de.dytanic.cloudnet.ext.signs.node.listener;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.event.cluster.NetworkChannelAuthClusterNodeSuccessEvent;
import de.dytanic.cloudnet.event.network.NetworkChannelReceiveCallablePacketEvent;
import de.dytanic.cloudnet.ext.signs.Sign;
import de.dytanic.cloudnet.ext.signs.SignConfiguration;
import de.dytanic.cloudnet.ext.signs.SignConfigurationReaderAndWriter;
import de.dytanic.cloudnet.ext.signs.SignConstants;
import de.dytanic.cloudnet.ext.signs.node.CloudNetSignsModule;

import java.util.Collection;

public final class CloudNetSignsModuleListener {

    @EventListener
    public void handle(NetworkChannelAuthClusterNodeSuccessEvent event)
    {
        event.getNode().sendCustomChannelMessage(
            SignConstants.SIGN_CLUSTER_CHANNEL_NAME,
            SignConstants.SIGN_CHANNEL_UPDATE_SIGN_CONFIGURATION,
            new JsonDocument()
                .append("signConfiguration", CloudNetSignsModule.getInstance().getSignConfiguration())
                .append("signs", CloudNetSignsModule.getInstance().loadSigns())
        );
    }

    @EventListener
    public void handle(NetworkChannelReceiveCallablePacketEvent event)
    {
        if (!event.getChannelName().equalsIgnoreCase(SignConstants.SIGN_CHANNEL_SYNC_CHANNEL_PROPERTY)) return;

        switch (event.getId().toLowerCase())
        {
            case SignConstants.SIGN_CHANNEL_SYNC_ID_GET_SIGNS_COLLECTION_PROPERTY:
            {
                event.setCallbackPacket(new JsonDocument("signs", CloudNetSignsModule.getInstance().loadSigns()));
            }
            break;
            case SignConstants.SIGN_CHANNEL_SYNC_ID_GET_SIGNS_CONFIGURATION_PROPERTY:
            {
                event.setCallbackPacket(new JsonDocument("signConfiguration", CloudNetSignsModule.getInstance().getSignConfiguration()));
            }
            break;
        }
    }

    @EventListener
    public void handle(ChannelMessageReceiveEvent event)
    {
        if (event.getChannel().equalsIgnoreCase(SignConstants.SIGN_CLUSTER_CHANNEL_NAME))
            switch (event.getMessage().toLowerCase())
            {
                case SignConstants.SIGN_CHANNEL_UPDATE_SIGN_CONFIGURATION:
                {
                    SignConfiguration signConfiguration = event.getData().get("signConfiguration", SignConfiguration.TYPE);
                    Collection<Sign> signs = event.getData().get("signs", SignConstants.COLLECTION_SIGNS);

                    CloudNetSignsModule.getInstance().setSignConfiguration(signConfiguration);
                    SignConfigurationReaderAndWriter.write(signConfiguration, CloudNetSignsModule.getInstance().getConfigurationFile());

                    CloudNetSignsModule.getInstance().write(signs);
                }
                break;
            }

        if (event.getChannel().equals(SignConstants.SIGN_CHANNEL_NAME))
            switch (event.getMessage().toLowerCase())
            {
                case SignConstants.SIGN_CHANNEL_ADD_SIGN_MESSAGE:
                {
                    Sign sign = event.getData().get("sign", Sign.TYPE);

                    if (sign != null)
                    {
                        CloudNetSignsModule.getInstance().addSignToFile(sign);
                    }
                }
                break;
                case SignConstants.SIGN_CHANNEL_REMOVE_SIGN_MESSAGE:
                {
                    Sign sign = event.getData().get("sign", Sign.TYPE);

                    if (sign != null)
                    {
                        CloudNetSignsModule.getInstance().removeSignToFile(sign);
                    }
                }
                break;
            }
    }
}