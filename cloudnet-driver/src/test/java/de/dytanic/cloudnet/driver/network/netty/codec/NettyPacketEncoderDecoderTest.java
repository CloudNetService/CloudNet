package de.dytanic.cloudnet.driver.network.netty.codec;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class NettyPacketEncoderDecoderTest {

    @Test
    public void testEncoderDecoder() {
        NettyPacketEncoder nettyPacketEncoder = new NettyPacketEncoder();
        NettyPacketDecoder nettyPacketDecoder = new NettyPacketDecoder();

        ByteBuf byteBuf = Unpooled.buffer();
        Packet packet = new Packet(4, new JsonDocument().append("val", true), Packet.EMPTY_PACKET_BYTE_ARRAY);
        Packet packet2 = new Packet(2, new JsonDocument(), "Test_Nachricht".getBytes());
        Packet packet3 = new Packet(3, new JsonDocument(), new byte[0]);

        nettyPacketEncoder.encode(null, packet, byteBuf);
        nettyPacketEncoder.encode(null, packet2, byteBuf);
        nettyPacketEncoder.encode(null, packet3, byteBuf);

        List<Object> packets = new ArrayList<>();
        nettyPacketDecoder.decode(null, byteBuf, packets);
        nettyPacketDecoder.decode(null, byteBuf, packets);
        nettyPacketDecoder.decode(null, byteBuf, packets);

        Assert.assertEquals(3, packets.size());
        Assert.assertTrue(packets.get(0) instanceof Packet);
        Assert.assertTrue(packets.get(1) instanceof Packet);
        Assert.assertTrue(packets.get(2) instanceof Packet);

        packet = (Packet) packets.get(0);
        Assert.assertTrue(packet.getHeader().getBoolean("val"));

        Assert.assertEquals("Test_Nachricht", new String(((Packet) packets.get(1)).getBodyAsArray(), StandardCharsets.UTF_8));
    }
}