package de.dytanic.cloudnet.driver.network.protocol;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import org.junit.Assert;
import org.junit.Test;

public class DefaultPacketRegistryTest {

    private String testValue = null;

    private int property = 0;

    @Test
    public void testPacketRegistry() throws Throwable {
        final int channelId = 4;

        PacketListener listener = new PacketListenerImpl();

        PacketListenerRegistry registry = new DefaultPacketListenerRegistry();
        registry.addListener(channelId, listener);

        Assert.assertEquals(1, registry.getListeners().size());

        registry.handlePacket(null, new AbstractPacket(channelId, new JsonDocument("testProperty", 65), "TestValue".getBytes()));

        Assert.assertEquals(65, property);
        Assert.assertEquals("TestValue", testValue);

        registry.removeListeners(channelId);

        Assert.assertEquals(0, registry.getListeners().size());

        registry.addListener(channelId, listener);
        registry.removeListener(channelId, listener);

        Assert.assertEquals(0, registry.getListeners().size());
    }

    private final class PacketListenerImpl implements PacketListener {

        @Override
        public void handle(INetworkChannel channel, Packet packet) {
            testValue = new String(packet.getBody());
            property = packet.getHeader().getInt("testProperty");
        }
    }
}