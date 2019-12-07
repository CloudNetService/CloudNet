package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.*;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.protocol.PacketListener;
import de.dytanic.cloudnet.driver.network.protocol.AbstractPacket;
import org.junit.Assert;
import org.junit.Test;

public class NettyNetworkClientServerTest {

    private boolean
            connectedClient = false,
            connectedServer = false;

    private volatile String
            cliPacketServerReceive = null,
            cliPacketClientReceive = null;

    @Test
    public void testNettyConnectorServer() throws Throwable {
        INetworkServer networkServer = new NettyNetworkServer(NetworkChannelServerHandler::new);
        INetworkClient networkClient = new NettyNetworkClient(NetworkChannelClientHandler::new);

        networkClient.getPacketRegistry().addListener(6, new PacketListenerImpl());
        networkServer.getPacketRegistry().addListener(6, new PacketListenerImpl());

        HostAndPort address = new HostAndPort("127.0.0.1", NettyTestUtil.generateRandomPort());

        Assert.assertTrue(networkServer.addListener(address));
        Assert.assertTrue(networkClient.connect(address));

        Thread.sleep(50);
        Assert.assertTrue(connectedClient);
        Assert.assertTrue(connectedServer);

        Assert.assertEquals(1, networkClient.getChannels().size());
        Assert.assertEquals(1, networkServer.getChannels().size());

        networkServer.sendPacket(new AbstractPacket(6, new JsonDocument(), "TestValue".getBytes()));
        networkClient.sendPacket(new AbstractPacket(6, new JsonDocument(), "TestValue".getBytes()));

        Thread.sleep(500);

        Assert.assertNotNull(cliPacketClientReceive);
        Assert.assertNotNull(cliPacketServerReceive);

        Assert.assertEquals("TestValue", cliPacketClientReceive);
        Assert.assertEquals("TestValue", cliPacketServerReceive);

        networkClient.close();
        networkServer.close();

        Assert.assertEquals(0, networkClient.getChannels().size());
        Assert.assertEquals(0, networkServer.getChannels().size());
    }

    private final class NetworkChannelClientHandler implements NetworkChannelHandler {

        @Override
        public void handleChannelInitialize(INetworkChannel channel) {
            connectedClient = true;
        }

        @Override
        public boolean handlePacketReceive(INetworkChannel channel, AbstractPacket packet) {
            cliPacketServerReceive = new String(packet.getBody());

            return true;
        }

        @Override
        public void handleChannelClose(INetworkChannel channel) {

        }
    }

    private final class NetworkChannelServerHandler implements NetworkChannelHandler {

        @Override
        public void handleChannelInitialize(INetworkChannel channel) {
            connectedServer = true;
        }

        @Override
        public boolean handlePacketReceive(INetworkChannel channel, AbstractPacket packet) {
            return true;
        }

        @Override
        public void handleChannelClose(INetworkChannel channel) {

        }
    }

    private final class PacketListenerImpl implements PacketListener {

        @Override
        public void handle(INetworkChannel channel, Packet packet) {
            cliPacketClientReceive = new String(packet.getBody());
        }
    }

}