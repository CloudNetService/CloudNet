/*
 * Copyright 2019-2022 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cloudnetservice.cloudnet.driver.network.netty;

import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.driver.DriverEnvironment;
import eu.cloudnetservice.cloudnet.driver.network.exception.SilentDecoderException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCounted;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.FastThreadLocalThread;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Range;

/**
 * Internal util for Netty and its ByteBuf
 */
@Internal
public final class NettyUtils {

  public static final boolean NATIVE_TRANSPORT = Epoll.isAvailable();
  public static final ThreadFactory THREAD_FACTORY = FastThreadLocalThread::new;
  public static final SilentDecoderException INVALID_VAR_INT = new SilentDecoderException("Invalid var int");
  public static final RejectedExecutionHandler DEFAULT_REJECT_HANDLER = new ThreadPoolExecutor.CallerRunsPolicy();

  static {
    // use jdk logger to prevent issues with older slf4j versions
    // like them bundled in spigot 1.8
    InternalLoggerFactory.setDefaultFactory(JdkLoggerFactory.INSTANCE);
    // check if the leak detection level is set before overriding it
    // may be useful for debugging of the network
    if (System.getProperty("io.netty.leakDetection.level") == null) {
      ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
    }
  }

  private NettyUtils() {
    throw new UnsupportedOperationException();
  }

  /**
   * Get a so-called "packet dispatcher" which represents an executor handling the received packets sent to a network
   * component. The executor is optimized for the currently running-in environment - this represents the default
   * settings applied to the dispatcher. If running in a node env the packet dispatcher uses the amount of processors
   * multiplied by 2 for the maximum thread amount, in a wrapper env this is fixed to {@code 8}. All threads in the
   * dispatcher can idle for 30 seconds before they are terminated forcefully. One thread will always idle in the
   * handler to speed up just-in-time handling of packets. Given tasks are queued in the order they are given into the
   * dispatcher and if the dispatcher has no capacity to run the task, the caller will automatically call the task
   * instead.
   *
   * @return a new packet dispatcher instance.
   * @see #threadAmount()
   */
  public static @NonNull Executor newPacketDispatcher() {
    // a cached pool with a thread idle-lifetime of 30 seconds
    // rejected tasks will be executed on the calling thread (See ThreadPoolExecutor.CallerRunsPolicy)
    // at least one thread is always idling in this executor
    return new ThreadPoolExecutor(
      1,
      threadAmount(),
      30L,
      TimeUnit.SECONDS,
      new SynchronousQueue<>(true),
      DEFAULT_REJECT_HANDLER);
  }

  /**
   * Creates a new nio or epoll event loop group based on their availability.
   *
   * @return a new nio or epoll event loop group based on their availability.
   */
  public static @NonNull EventLoopGroup newEventLoopGroup() {
    return Epoll.isAvailable()
      ? new EpollEventLoopGroup(4, THREAD_FACTORY)
      : new NioEventLoopGroup(4, THREAD_FACTORY);
  }

  /**
   * Creates a new channel factory for network clients based on the epoll availability.
   *
   * @return a new channel factory for network clients based on the epoll availability.
   */
  public static @NonNull ChannelFactory<? extends Channel> clientChannelFactory() {
    return Epoll.isAvailable() ? EpollSocketChannel::new : NioSocketChannel::new;
  }

  /**
   * Creates a new channel factory for network servers based on the epoll availability.
   *
   * @return a new channel factory for network servers based on the epoll availability.
   */
  public static @NonNull ChannelFactory<? extends ServerChannel> serverChannelFactory() {
    return Epoll.isAvailable() ? EpollServerSocketChannel::new : NioServerSocketChannel::new;
  }

  /**
   * Writes the given integer value as a var int into the buffer.
   *
   * @param byteBuf the buffer to write to.
   * @param value   the value to write into the buffer.
   * @return the buffer used to call the method, for chaining.
   */
  public static @NonNull ByteBuf writeVarInt(@NonNull ByteBuf byteBuf, int value) {
    if ((value & -128) == 0) {
      byteBuf.writeByte(value);
    } else if ((value & -16384) == 0) {
      var shortValue = (value & 0x7F | 0x80) << 8 | (value >>> 7);
      byteBuf.writeShort(shortValue);
    } else {
      while (true) {
        if ((value & -128) == 0) {
          byteBuf.writeByte(value);
          return byteBuf;
        }

        byteBuf.writeByte(value & 0x7F | 0x80);
        value >>>= 7;
      }
    }

    return byteBuf;
  }

  /**
   * Reads a var int from the given buffer.
   *
   * @param byteBuf the buffer to read from.
   * @return the var int read from the buffer.
   * @throws SilentDecoderException if the buf current position has no var int.
   */
  public static int readVarInt(@NonNull ByteBuf byteBuf) {
    var i = 0;
    var maxRead = Math.min(5, byteBuf.readableBytes());
    for (var j = 0; j < maxRead; j++) {
      int nextByte = byteBuf.readByte();
      i |= (nextByte & 0x7F) << j * 7;
      if ((nextByte & 0x80) != 128) {
        return i;
      }
    }
    throw INVALID_VAR_INT;
  }

  /**
   * Releases the given {@link ReferenceCounted} object with a pre-check if the reference count is {@code > 0} before
   * releasing the message.
   *
   * @param counted the object to safe release.
   */
  public static void safeRelease(@NonNull ReferenceCounted counted) {
    if (counted.refCnt() > 0) {
      counted.release(counted.refCnt());
    }
  }

  /**
   * Get the thread amount used by the packet dispatcher to dispatch incoming packets. This method returns always {@code
   * 4} when running in {@link DriverEnvironment#WRAPPER} and the amount of processors cores multiplied by 2 when
   * running on a node.
   *
   * @return the thread amount used by the packet dispatcher to dispatch incoming packets.
   */
  public static @Range(from = 2, to = Integer.MAX_VALUE) int threadAmount() {
    var environment = CloudNetDriver.instance().environment();
    return environment == DriverEnvironment.CLOUDNET ? Runtime.getRuntime().availableProcessors() * 2 : 4;
  }
}
