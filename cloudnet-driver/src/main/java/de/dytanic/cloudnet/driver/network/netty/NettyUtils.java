package de.dytanic.cloudnet.driver.network.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.EventLoopGroup;
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
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.MultithreadEventExecutorGroup;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadFactory;


public final class NettyUtils {

    static {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
    }

    private NettyUtils() {
        throw new UnsupportedOperationException();
    }

    public static EventLoopGroup newEventLoopGroup() {
        return Epoll.isAvailable() ?
                new EpollEventLoopGroup(Runtime.getRuntime().availableProcessors(), threadFactory()) :
                KQueue.isAvailable() ?
                        new KQueueEventLoopGroup(Runtime.getRuntime().availableProcessors(), threadFactory()) :
                        new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(), threadFactory());
    }

    public static Class<? extends SocketChannel> getSocketChannelClass() {
        return Epoll.isAvailable() ? EpollSocketChannel.class : KQueue.isAvailable() ? KQueueSocketChannel.class : NioSocketChannel.class;
    }

    public static Class<? extends ServerSocketChannel> getServerSocketChannelClass() {
        return Epoll.isAvailable() ? EpollServerSocketChannel.class : KQueue.isAvailable() ? KQueueServerSocketChannel.class : NioServerSocketChannel.class;
    }

    public static ThreadFactory threadFactory() {
        return new DefaultThreadFactory(MultithreadEventExecutorGroup.class, true, Thread.MIN_PRIORITY);
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
            numRead |= (read & 127) << result++ * 7;

            if (result > 5) {
                throw new RuntimeException("VarInt is too big");
            }
        } while ((read & 128) == 128);

        return numRead;
    }

    public static ByteBuf writeVarInt(ByteBuf byteBuf, int value) {
        while ((value & -128) != 0) {
            byteBuf.writeByte(value & 127 | 128);
            value >>>= 7;
        }

        byteBuf.writeByte(value);
        return byteBuf;
    }

    public static void writeString(ByteBuf byteBuf, String string) {
        byte[] values = string.getBytes(StandardCharsets.UTF_8);
        writeVarInt(byteBuf, values.length);
        byteBuf.writeBytes(values);
    }

    public static String readString(ByteBuf byteBuf) {
        int integer = readVarInt(byteBuf);
        byte[] buffer = new byte[integer];
        byteBuf.readBytes(buffer, 0, integer);

        return new String(buffer, StandardCharsets.UTF_8);
    }
}