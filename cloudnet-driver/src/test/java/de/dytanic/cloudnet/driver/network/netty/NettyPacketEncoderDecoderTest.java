package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.protocol.AbstractPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class NettyPacketEncoderDecoderTest {

    @Test
    public void testEncoderDecoder() throws Exception {
        NettyPacketEncoder nettyPacketEncoder = new NettyPacketEncoder();
        NettyPacketDecoder nettyPacketDecoder = new NettyPacketDecoder();

        ByteBuf byteBuf = Unpooled.buffer();
        AbstractPacket packet = new AbstractPacket(4, new JsonDocument().append("val", true), AbstractPacket.EMPTY_PACKET_BYTE_ARRAY);
        AbstractPacket packet2 = new AbstractPacket(2, new JsonDocument(), "Test_Nachricht".getBytes());
        AbstractPacket packet3 = new AbstractPacket(3, new JsonDocument(), new byte[0]);

        nettyPacketEncoder.encode(null, packet, byteBuf);
        nettyPacketEncoder.encode(null, packet2, byteBuf);
        nettyPacketEncoder.encode(null, packet3, byteBuf);

        List<Object> packets = new ArrayList<>();
        nettyPacketDecoder.decode(null, byteBuf, packets);
        nettyPacketDecoder.decode(null, byteBuf, packets);
        nettyPacketDecoder.decode(null, byteBuf, packets);

        Assert.assertEquals(3, packets.size());
        Assert.assertTrue(packets.get(0) instanceof AbstractPacket);
        Assert.assertTrue(packets.get(1) instanceof AbstractPacket);
        Assert.assertTrue(packets.get(2) instanceof AbstractPacket);

        packet = (AbstractPacket) packets.get(0);
        Assert.assertTrue(packet.getHeader().getBoolean("val"));

        Assert.assertEquals("Test_Nachricht", new String(((AbstractPacket) packets.get(1)).getBody(), StandardCharsets.UTF_8));
    }
}