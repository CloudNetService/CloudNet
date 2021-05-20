package eu.cloudnetservice.cloudnet.ext.signs.node;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.signs.AbstractSignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import eu.cloudnetservice.cloudnet.ext.signs.SignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.service.AbstractServiceSignManagement;

public class NodeChannelMessageListener {

    protected final SignManagement signManagement;

    public NodeChannelMessageListener(SignManagement signManagement) {
        this.signManagement = signManagement;
    }

    @EventListener
    public void handleChannelMessage(ChannelMessageReceiveEvent event) {
        if (event.getChannel().equals(AbstractSignManagement.SIGN_CHANNEL_NAME) && event.getMessage() != null) {
            switch (event.getMessage()) {
                case AbstractServiceSignManagement.REQUEST_CONFIG:
                    event.setBinaryResponse(ProtocolBuffer.create().writeObject(this.signManagement.getSignsConfiguration()));
                    break;
                case AbstractServiceSignManagement.SIGN_ALL_DELETE:
                    for (WorldPosition position : event.getBuffer().readObjectCollection(WorldPosition.class)) {
                        this.signManagement.deleteSign(position);
                    }
                    break;
                case AbstractServiceSignManagement.SIGN_CREATE:
                    this.signManagement.createSign(event.getBuffer().readObject(Sign.class));
                    break;
                case AbstractServiceSignManagement.SIGN_DELETE:
                    this.signManagement.deleteSign(event.getBuffer().readObject(WorldPosition.class));
                    break;
                case AbstractServiceSignManagement.SIGN_BULK_DELETE:
                    int deleted = this.signManagement.deleteAllSigns(event.getBuffer().readString(), event.getBuffer().readOptionalString());
                    event.setBinaryResponse(ProtocolBuffer.create().writeVarInt(deleted));
                    break;
                default:
                    break;
            }
        }
    }
}
