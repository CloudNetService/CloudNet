package de.dytanic.cloudnet.driver.network.protocol;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

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
@ToString
@EqualsAndHashCode
public class Packet implements IPacket {

    /**
     * An one length size byte[] for empty packet bodies
     */
    public static final byte[] EMPTY_PACKET_BYTE_ARRAY = new byte[]{0};

    public static final Packet EMPTY = new Packet(-1, JsonDocument.EMPTY, EMPTY_PACKET_BYTE_ARRAY);

    protected int channel;

    protected UUID uniqueId;

    protected JsonDocument header;

    protected ProtocolBuffer body;

    public Packet(int channel, @NotNull JsonDocument header) {
        this(channel, header, (ProtocolBuffer) null);
    }

    public Packet(int channel, @NotNull JsonDocument header, byte[] body) {
        this(channel, UUID.randomUUID(), header, body);
    }

    public Packet(int channel, @NotNull UUID uniqueId, @NotNull JsonDocument header, byte[] body) {
        this.channel = channel;
        this.uniqueId = uniqueId;
        this.header = header;
        this.body = body == null ? null : ProtocolBuffer.wrap(body);
    }


    public Packet(int channel, ProtocolBuffer body) {
        this(channel, JsonDocument.EMPTY, body);
    }

    public Packet(int channel, @NotNull JsonDocument header, ProtocolBuffer body) {
        this(channel, UUID.randomUUID(), header, body);
    }

    public Packet(int channel, @NotNull UUID uniqueId, @NotNull JsonDocument header) {
        this(channel, uniqueId, header, (ProtocolBuffer) null);
    }

    public Packet(int channel, @NotNull UUID uniqueId, @NotNull JsonDocument header, ProtocolBuffer body) {
        this.channel = channel;
        this.uniqueId = uniqueId;
        this.header = header;
        this.body = body;
    }

    public Packet() {
    }

    public int getChannel() {
        return this.channel;
    }

    public @NotNull UUID getUniqueId() {
        if (this.uniqueId == null) {
            this.uniqueId = UUID.randomUUID();
        }
        return this.uniqueId;
    }

    public JsonDocument getHeader() {
        return this.header;
    }

    public ProtocolBuffer getBuffer() {
        return this.body;
    }

    @Override
    public byte[] getBodyAsArray() {
        return this.body == null ? EMPTY_PACKET_BYTE_ARRAY : this.body.toArray();
    }

    public static Packet createResponseFor(IPacket packet, JsonDocument header, ProtocolBuffer body) {
        return new Packet(-1, packet.getUniqueId(), header, body);
    }

    public static Packet createResponseFor(IPacket packet, JsonDocument header) {
        return new Packet(-1, packet.getUniqueId(), header);
    }

    public static Packet createResponseFor(IPacket packet, ProtocolBuffer body) {
        return new Packet(-1, packet.getUniqueId(), JsonDocument.EMPTY, body);
    }

    public static Packet createResponseFor(IPacket packet) {
        return new Packet(-1, packet.getUniqueId(), JsonDocument.EMPTY);
    }

}