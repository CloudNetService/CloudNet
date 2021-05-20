package eu.cloudnetservice.cloudnet.ext.signs;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignsConfiguration;

public class GlobalChannelMessageListener {

    protected final SignManagement signManagement;

    public GlobalChannelMessageListener(SignManagement signManagement) {
        this.signManagement = signManagement;
    }

    @EventListener
    public void handleChannelMessage(ChannelMessageReceiveEvent event) {
        if (event.getChannel().equals(AbstractSignManagement.SIGN_CHANNEL_NAME) && event.getMessage() != null) {
            switch (event.getMessage()) {
                case AbstractSignManagement.SIGN_CREATED:
                    this.signManagement.handleInternalSignCreate(event.getBuffer().readObject(Sign.class));
                    break;
                case AbstractSignManagement.SIGN_DELETED:
                    this.signManagement.handleInternalSignRemove(event.getBuffer().readObject(WorldPosition.class));
                    break;
                case AbstractSignManagement.SIGN_BULK_DELETE:
                    for (WorldPosition position : event.getBuffer().readObjectCollection(WorldPosition.class)) {
                        this.signManagement.handleInternalSignRemove(position);
                    }
                    break;
                case AbstractSignManagement.SIGN_CONFIGURATION_UPDATE:
                    this.signManagement.handleInternalSignConfigUpdate(event.getBuffer().readObject(SignsConfiguration.class));
                    break;
                default:
                    break;
            }
        }
    }
}
