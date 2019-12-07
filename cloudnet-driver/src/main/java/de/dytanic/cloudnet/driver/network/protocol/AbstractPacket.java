package de.dytanic.cloudnet.driver.network.protocol;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;

import java.util.UUID;

/**
 * The default simple implementation of the IPacket interface. You can create
 * with the constructor all new packets or use the class as superclass from an
 * another for specific constructor usage.
 * <p>
 * All packets require a channel, header and a body.
 * <p>
 * The channel id is the id from that the listeners should be filter The header
 * has the specify information or the data that is important The body has binary
 * packet information like for files, or zip compressed data
 */
public class AbstractPacket implements Packet {

    /**
     * An one length size byte[] for empty packet bodies
     */
    public static final byte[] EMPTY_PACKET_BYTE_ARRAY = new byte[]{0};

    protected int channel;

    protected UUID uniqueId;

    protected JsonDocument header;

    protected byte[] body;

    public AbstractPacket(int channel, JsonDocument header) {
        this(channel, header, null);
    }

    public AbstractPacket(int channel, JsonDocument header, byte[] body) {
        this.channel = channel;
        this.header = header;
        this.body = body;
        this.uniqueId = UUID.randomUUID();
    }

    public AbstractPacket(int channel, UUID uniqueId, JsonDocument header, byte[] body) {
        this.channel = channel;
        this.uniqueId = uniqueId;
        this.header = header;
        this.body = body;
    }

    public AbstractPacket() {
    }

    public int getChannel() {
        return this.channel;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public JsonDocument getHeader() {
        return this.header;
    }

    public byte[] getBody() {
        return this.body;
    }
}