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

        IPacketListener listener = new PacketListenerImpl();

        IPacketListenerRegistry registry = new DefaultPacketListenerRegistry();
        registry.addListener(channelId, listener);

        Assert.assertEquals(1, registry.getListeners().size());

        registry.handlePacket(null, new Packet(channelId, new JsonDocument("testProperty", 65), "TestValue".getBytes()));

        Assert.assertEquals(65, this.property);
        Assert.assertEquals("TestValue", this.testValue);

        registry.removeListeners(channelId);

        Assert.assertEquals(0, registry.getListeners().size());

        registry.addListener(channelId, listener);
        registry.removeListener(channelId, listener);

        Assert.assertEquals(0, registry.getListeners().size());
    }

    private final class PacketListenerImpl implements IPacketListener {

        @Override
        public void handle(INetworkChannel channel, IPacket packet) {
            DefaultPacketRegistryTest.this.testValue = new String(packet.getBodyAsArray());
            DefaultPacketRegistryTest.this.property = packet.getHeader().getInt("testProperty");
        }
    }
}