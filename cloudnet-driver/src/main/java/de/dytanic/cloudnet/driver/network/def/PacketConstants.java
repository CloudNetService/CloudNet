package de.dytanic.cloudnet.driver.network.def;

public final class PacketConstants {

    public static final int
            INTERNAL_AUTHORIZATION_CHANNEL = 1,
            INTERNAL_CLUSTER_CHANNEL = 2,
            INTERNAL_WRAPPER_TO_NODE_INFO_CHANNEL = 3,
            INTERNAL_EVENTBUS_CHANNEL = 4,
            INTERNAL_CALLABLE_CHANNEL = 5,
            INTERNAL_PACKET_CLUSTER_MESSAGE_CHANNEL = 6,
            INTERNAL_H2_DATABASE_UPDATE_MODULE = 7,
            INTERNAL_DEBUGGING_CHANNEL = 8;
    public static final String SYNC_PACKET_CHANNEL_PROPERTY = "synchronized_packet_channel_name", SYNC_PACKET_ID_PROPERTY = "synchronized_packet_id";
    public static final String CLUSTER_NODE_SYNC_PACKET_CHANNEL_NAME = "synchronized_cluster_node_sync_channel";

    private PacketConstants() {
        throw new UnsupportedOperationException();
    }

}