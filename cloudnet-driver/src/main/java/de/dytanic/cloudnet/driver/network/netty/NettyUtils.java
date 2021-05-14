package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.DriverEnvironment;
import de.dytanic.cloudnet.driver.network.exception.SilentDecoderException;
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
import org.jetbrains.annotations.ApiStatus;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@ApiStatus.Internal
public final class NettyUtils {

    private static final ThreadFactory THREAD_FACTORY = FastThreadLocalThread::new;
    private static final SilentDecoderException INVALID_VAR_INT = new SilentDecoderException("Invalid var int");

    static {
        if (System.getProperty("io.netty.leakDetection.level") == null) {
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
        }
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
        // a cached pool with a maximum of 100 threads with an idle-lifetime of 30 seconds
        return new ThreadPoolExecutor(0, 100,
                30L, TimeUnit.SECONDS, new SynchronousQueue<>(true));
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

    @Deprecated
    @ApiStatus.ScheduledForRemoval
    public static byte[] toByteArray(ByteBuf byteBuf, int size) {
        return readByteArray(byteBuf, size);
    }

    public static byte[] readByteArray(ByteBuf byteBuf, int size) {
        byte[] data = new byte[size];
        byteBuf.readBytes(data);
        return data;
    }

    public static int readVarInt(ByteBuf byteBuf) {
        return (int) readVarVariant(byteBuf, 5);
    }

    public static ByteBuf writeVarInt(ByteBuf byteBuf, int value) {
        while (true) {
            if ((value & -128) == 0) {
                byteBuf.writeByte(value);
                return byteBuf;
            }

            byteBuf.writeByte(value & 0x7F | 0x80);
            value >>>= 7;
        }
    }

    public static long readVarLong(ByteBuf byteBuf) {
        return readVarVariant(byteBuf, 10);
    }

    public static ByteBuf writeVarLong(ByteBuf byteBuf, long value) {
        while (true) {
            if ((value & -128) == 0) {
                byteBuf.writeByte((int) value);
                return byteBuf;
            }

            byteBuf.writeByte((int) value & 0x7F | 0x80);
            value >>>= 7;
        }
    }

    private static long readVarVariant(ByteBuf byteBuf, int maxReadUpperBound) {
        long i = 0;
        int maxRead = Math.min(maxReadUpperBound, byteBuf.readableBytes());
        for (int j = 0; j < maxRead; j++) {
            int nextByte = byteBuf.readByte();
            i |= (long) (nextByte & 0x7F) << j * 7;
            if ((nextByte & 0x80) != 128) {
                return i;
            }
        }
        throw INVALID_VAR_INT;
    }

    public static ByteBuf writeString(ByteBuf byteBuf, String string) {
        byte[] content = string.getBytes(StandardCharsets.UTF_8);
        writeVarInt(byteBuf, content.length);
        byteBuf.writeBytes(content);
        return byteBuf;
    }

    public static String readString(ByteBuf byteBuf) {
        int size = readVarInt(byteBuf);
        return new String(readByteArray(byteBuf, size), StandardCharsets.UTF_8);
    }

    public static int getThreadAmount() {
        return CloudNetDriver.optionalInstance()
                .filter(cloudNetDriver -> cloudNetDriver.getDriverEnvironment() == DriverEnvironment.CLOUDNET)
                .map(cloudNetDriver -> Runtime.getRuntime().availableProcessors() * 2)
                .orElse(4);
    }
}