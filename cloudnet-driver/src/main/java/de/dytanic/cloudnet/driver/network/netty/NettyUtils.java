package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.DriverEnvironment;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.FastThreadLocalThread;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;
import org.jetbrains.annotations.ApiStatus;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@ApiStatus.Internal
public final class NettyUtils {

    private static final ThreadFactory THREAD_FACTORY = FastThreadLocalThread::new;

    static {
        // use jdk logger to prevent issues with older slf4j versions
        // like them bundled in spigot 1.8
        InternalLoggerFactory.setDefaultFactory(JdkLoggerFactory.INSTANCE);
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
    }

    private NettyUtils() {
        throw new UnsupportedOperationException();
    }

    public static EventLoopGroup newEventLoopGroup() {
        int threads = getThreadAmount();
        return Epoll.isAvailable() ?
                new EpollEventLoopGroup(threads, threadFactory()) :
                KQueue.isAvailable() ?
                        new KQueueEventLoopGroup(threads, threadFactory()) :
                        new NioEventLoopGroup(threads, threadFactory());
    }

    public static Executor newPacketDispatcher() {
        return Executors.newFixedThreadPool(getThreadAmount());
    }

    @Deprecated
    @ApiStatus.ScheduledForRemoval
    public static Class<? extends SocketChannel> getSocketChannelClass() {
        return Epoll.isAvailable() ? EpollSocketChannel.class : KQueue.isAvailable() ? KQueueSocketChannel.class : NioSocketChannel.class;
    }

    public static ChannelFactory<? extends Channel> getClientChannelFactory() {
        return Epoll.isAvailable() ? EpollSocketChannel::new : KQueue.isAvailable() ? KQueueSocketChannel::new : NioSocketChannel::new;
    }

    @Deprecated
    @ApiStatus.ScheduledForRemoval
    public static Class<? extends ServerSocketChannel> getServerSocketChannelClass() {
        return Epoll.isAvailable() ? EpollServerSocketChannel.class : KQueue.isAvailable() ? KQueueServerSocketChannel.class : NioServerSocketChannel.class;
    }

    public static ChannelFactory<? extends ServerChannel> getServerChannelFactory() {
        return Epoll.isAvailable() ? EpollServerSocketChannel::new : KQueue.isAvailable() ? KQueueServerSocketChannel::new : NioServerSocketChannel::new;
    }

    public static ThreadFactory threadFactory() {
        return THREAD_FACTORY;
    }

    public static byte[] toByteArray(ByteBuf byteBuf, int size) {
        byte[] data = new byte[size];
        byteBuf.readBytes(data);
        return data;
    }

    public static int readVarInt(ByteBuf byteBuf) {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = byteBuf.readByte();
            int value = (read & 0b01111111);
            result |= (value << (7 * numRead));

            numRead++;
            if (numRead > 5) {
                throw new RuntimeException("VarInt is too big");
            }
        } while ((read & 0b10000000) != 0);

        return result;
    }

    public static long readVarLong(ByteBuf byteBuf) {
        int numRead = 0;
        long result = 0;
        byte read;
        do {
            read = byteBuf.readByte();
            int value = (read & 0b01111111);
            result |= (value << (7 * numRead));

            numRead++;
            if (numRead > 10) {
                throw new RuntimeException("VarLong is too big");
            }
        } while ((read & 0b10000000) != 0);

        return result;
    }

    public static ByteBuf writeVarInt(ByteBuf byteBuf, int value) {
        do {
            byte temp = (byte) (value & 0b01111111);
            value >>>= 7;
            if (value != 0) {
                temp |= 0b10000000;
            }
            byteBuf.writeByte(temp);
        } while (value != 0);

        return byteBuf;
    }

    public static ByteBuf writeVarLong(ByteBuf byteBuf, long value) {
        do {
            byte temp = (byte) (value & 0b01111111);
            value >>>= 7;
            if (value != 0) {
                temp |= 0b10000000;
            }
            byteBuf.writeByte(temp);
        } while (value != 0);

        return byteBuf;
    }

    public static ByteBuf writeString(ByteBuf byteBuf, String string) {
        byte[] values = string.getBytes(StandardCharsets.UTF_8);
        writeVarInt(byteBuf, values.length);
        byteBuf.writeBytes(values);
        return byteBuf;
    }

    public static String readString(ByteBuf byteBuf) {
        int integer = readVarInt(byteBuf);
        byte[] buffer = new byte[integer];
        byteBuf.readBytes(buffer, 0, integer);

        return new String(buffer, StandardCharsets.UTF_8);
    }

    public static int getThreadAmount() {
        return CloudNetDriver.optionalInstance()
                .filter(cloudNetDriver -> cloudNetDriver.getDriverEnvironment() == DriverEnvironment.CLOUDNET)
                .map(cloudNetDriver -> Runtime.getRuntime().availableProcessors() * 2)
                .orElse(4);
    }
}