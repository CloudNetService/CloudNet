package de.dytanic.cloudnet.driver.network.def.packet;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.Packet;

public final class PacketClientAuthorization extends Packet {

    public PacketClientAuthorization(PacketAuthorizationType packetAuthorizationType, JsonDocument credentials) {
        super(PacketConstants.INTERNAL_AUTHORIZATION_CHANNEL, new JsonDocument(), null);

        Validate.checkNotNull(packetAuthorizationType);
        Validate.checkNotNull(credentials);

        this.header.append("authorization", packetAuthorizationType).append("credentials", credentials);
    }

    public enum PacketAuthorizationType {

        NODE_TO_NODE(0),
        WRAPPER_TO_NODE(1);

        private int value;

        private PacketAuthorizationType(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }
}