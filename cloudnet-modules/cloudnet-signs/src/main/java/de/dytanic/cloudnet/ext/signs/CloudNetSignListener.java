package de.dytanic.cloudnet.ext.signs;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.service.*;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfiguration;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfigurationProvider;

public final class CloudNetSignListener {

    @EventListener
    public void handle(CloudServiceRegisterEvent event) {
        AbstractSignManagement.getInstance().onRegisterService(event.getServiceInfo());
    }

    @EventListener
    public void handle(CloudServiceStartEvent event) {
        AbstractSignManagement.getInstance().onStartService(event.getServiceInfo());
    }

    @EventListener
    public void handle(CloudServiceConnectNetworkEvent event) {
        AbstractSignManagement.getInstance().onConnectService(event.getServiceInfo());
    }

    @EventListener
    public void handle(CloudServiceDisconnectNetworkEvent event) {
        AbstractSignManagement.getInstance().onDisconnectService(event.getServiceInfo());
    }

    @EventListener
    public void handle(CloudServiceInfoUpdateEvent event) {
        AbstractSignManagement.getInstance().onUpdateServiceInfo(event.getServiceInfo());
    }

    @EventListener
    public void handle(CloudServiceUnregisterEvent event) {
        AbstractSignManagement.getInstance().onUnregisterService(event.getServiceInfo());
    }

    @EventListener
    public void handle(CloudServiceStopEvent event) {
        AbstractSignManagement.getInstance().onStopService(event.getServiceInfo());
    }

    @EventListener
    public void handle(ChannelMessageReceiveEvent event) {
        if (!event.getChannel().equals(SignConstants.SIGN_CHANNEL_NAME)) {
            return;
        }

        switch (event.getMessage().toLowerCase()) {
            case SignConstants.SIGN_CHANNEL_UPDATE_SIGN_CONFIGURATION: {
                SignConfiguration signConfiguration = event.getData().get("signConfiguration", SignConfiguration.TYPE);
                SignConfigurationProvider.setLocal(signConfiguration);
            }
            break;
            case SignConstants.SIGN_CHANNEL_ADD_SIGN_MESSAGE: {
                Sign sign = event.getData().get("sign", Sign.TYPE);

                if (sign != null) {
                    AbstractSignManagement.getInstance().onSignAdd(sign);
                }
            }
            break;
            case SignConstants.SIGN_CHANNEL_REMOVE_SIGN_MESSAGE: {
                Sign sign = event.getData().get("sign", Sign.TYPE);

                if (sign != null) {
                    AbstractSignManagement.getInstance().onSignRemove(sign);
                }
            }
            break;
        }
    }

}