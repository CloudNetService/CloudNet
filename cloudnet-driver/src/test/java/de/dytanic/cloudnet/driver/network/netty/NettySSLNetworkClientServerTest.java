package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.*;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public final class NettySSLNetworkClientServerTest implements INetworkChannelHandler {

    @Test
    public void testSslNetworking() throws Exception
    {
        SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();

        INetworkServer server = new NettyNetworkServer(() -> this, new SSLConfiguration(
            true,
            null,
            selfSignedCertificate.certificate(),
            selfSignedCertificate.privateKey()
        ), null);

        INetworkClient client = new NettyNetworkClient(() -> this, new SSLConfiguration(
            false,
            null,
            selfSignedCertificate.certificate(),
            selfSignedCertificate.privateKey()
        ), null);

        Assert.assertTrue(server.isSslEnabled());
        Assert.assertTrue(client.isSslEnabled());

        ITask<String> task = new ListenableTask<>(new Callable<String>() {
            @Override
            public String call() throws Exception
            {
                return "Hello, world!";
            }
        });

        server.getPacketRegistry().addListener(1, new IPacketListener() {
            @Override
            public void handle(INetworkChannel channel, IPacket packet) throws Exception
            {
                if (packet.getHeader().contains("hello") && packet.getHeader().getString("hello").equalsIgnoreCase("Unit test") &&
                    new String(packet.getBody()).equalsIgnoreCase("Test Test Test 1 2 4"))
                    task.call();
            }
        });

        HostAndPort hostAndPort = new HostAndPort("127.0.0.1", 34052);

        Assert.assertTrue(server.addListener(hostAndPort));
        Assert.assertTrue(client.connect(hostAndPort));

        Assert.assertEquals("Hello, world!", task.get(5, TimeUnit.SECONDS));

        server.close();
        client.close();
    }

    @Override
    public void handleChannelInitialize(INetworkChannel channel) throws Exception
    {
        channel.sendPacket(new Packet(1, new JsonDocument("hello", "Unit test"), "Test Test Test 1 2 4".getBytes()));
    }

    @Override
    public boolean handlePacketReceive(INetworkChannel channel, Packet packet) throws Exception
    {
        return true;
    }

    @Override
    public void handleChannelClose(INetworkChannel channel) throws Exception
    {

    }
}