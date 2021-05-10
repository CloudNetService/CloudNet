package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.*;
import de.dytanic.cloudnet.driver.network.netty.client.NettyNetworkClient;
import de.dytanic.cloudnet.driver.network.netty.server.NettyNetworkServer;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
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

        Thread.sleep(500);
        Assert.assertTrue(this.connectedClient);
        Assert.assertTrue(this.connectedServer);

        Assert.assertEquals(1, networkClient.getChannels().size());
        Assert.assertEquals(1, networkServer.getChannels().size());

        networkServer.sendPacket(new Packet(6, new JsonDocument(), "TestValue".getBytes()));
        networkClient.sendPacket(new Packet(6, new JsonDocument(), "TestValue".getBytes()));

        Thread.sleep(500);

        Assert.assertNotNull(this.cliPacketClientReceive);
        Assert.assertNotNull(this.cliPacketServerReceive);

        Assert.assertEquals("TestValue", this.cliPacketClientReceive);
        Assert.assertEquals("TestValue", this.cliPacketServerReceive);

        networkClient.close();
        networkServer.close();

        Assert.assertEquals(0, networkClient.getChannels().size());
        Assert.assertEquals(0, networkServer.getChannels().size());
    }

    private final class NetworkChannelClientHandler implements INetworkChannelHandler {

        @Override
        public void handleChannelInitialize(INetworkChannel channel) {
            NettyNetworkClientServerTest.this.connectedClient = true;
        }

        @Override
        public boolean handlePacketReceive(INetworkChannel channel, Packet packet) {
            NettyNetworkClientServerTest.this.cliPacketServerReceive = new String(packet.getBodyAsArray());

            return true;
        }

        @Override
        public void handleChannelClose(INetworkChannel channel) {

        }
    }

    private final class NetworkChannelServerHandler implements INetworkChannelHandler {

        @Override
        public void handleChannelInitialize(INetworkChannel channel) {
            NettyNetworkClientServerTest.this.connectedServer = true;
        }

        @Override
        public boolean handlePacketReceive(INetworkChannel channel, Packet packet) {
            return true;
        }

        @Override
        public void handleChannelClose(INetworkChannel channel) {

        }
    }

    private final class PacketListenerImpl implements IPacketListener {

        @Override
        public void handle(INetworkChannel channel, IPacket packet) {
            NettyNetworkClientServerTest.this.cliPacketClientReceive = new String(packet.getBodyAsArray());
        }
    }

}