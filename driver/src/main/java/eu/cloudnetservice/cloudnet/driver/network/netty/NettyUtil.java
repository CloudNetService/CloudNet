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
import io.netty5.buffer.api.Buffer;
import io.netty5.buffer.api.BufferAllocator;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelFactory;
import io.netty5.channel.EventLoopGroup;
import io.netty5.channel.ServerChannel;
import io.netty5.channel.ServerChannelFactory;
import io.netty5.util.ResourceLeakDetector;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Range;

/**
 * Internal util for the default netty based communication between server and clients, http and websocket.
 *
 * @since 4.0
 */
@Internal
public final class NettyUtil {

  // buffer
  private static final BufferAllocator ALLOCATOR = BufferAllocator.offHeapPooled();
  // transport
  private static final boolean NO_NATIVE_TRANSPORT = Boolean.getBoolean("cloudnet.no-native");
  private static final NettyTransport CURR_NETTY_TRANSPORT = NettyTransport.availableTransport(NO_NATIVE_TRANSPORT);
  // var int codec
  private static final int[] VAR_INT_LENGTHS = new int[33];
  private static final SilentDecoderException INVALID_VAR_INT = new SilentDecoderException("Invalid var int");
  // packet thread handling
  private static final RejectedExecutionHandler DEFAULT_REJECT_HANDLER = new ThreadPoolExecutor.CallerRunsPolicy();

  static {
    // check if the leak detection level is set before overriding it
    // may be useful for debugging of the network
    if (System.getProperty("io.netty5.leakDetection.level") == null) {
      ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
    }

    // initializes the length of each var int which removes the need for that later
    for (int i = 0; i <= 32; ++i) {
      VAR_INT_LENGTHS[i] = (int) Math.ceil(31D - (i - 1) / 7D);
    }
    // 0 is always one byte long
    VAR_INT_LENGTHS[32] = 1;
  }

  private NettyUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Get a so-called "packet dispatcher" which represents an executor handling the received packets sent to a network
   * component. The executor is optimized for the currently running-in environment - this represents the default
   * settings applied to the dispatcher. If running in a node env the packet dispatcher uses the amount of processors
   * multiplied by 2 for the maximum thread amount, in a wrapper env this is fixed to 8. All threads in the dispatcher
   * can idle for 30 seconds before they are terminated forcefully. One thread will always idle in the handler to speed
   * up just-in-time handling of packets. Given tasks are queued in the order they are given into the dispatcher and if
   * the dispatcher has no capacity to run the task, the caller will automatically call the task instead.
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
      new LinkedBlockingQueue<>(),
      DEFAULT_REJECT_HANDLER);
  }

  /**
   * Creates a new nio or epoll event loop group based on their availability.
   *
   * @param threads the number of threads to use for the event loop.
   * @return a new nio or epoll event loop group.
   */
  public static @NonNull EventLoopGroup newEventLoopGroup(int threads) {
    return CURR_NETTY_TRANSPORT.createEventLoopGroup(threads);
  }

  /**
   * Creates a new channel factory for network clients based on the epoll availability.
   *
   * @return a new channel factory for network clients based on the epoll availability.
   */
  public static @NonNull ChannelFactory<? extends Channel> clientChannelFactory() {
    return CURR_NETTY_TRANSPORT.clientChannelFactory();
  }

  /**
   * Creates a new channel factory for network servers based on the epoll availability.
   *
   * @return a new channel factory for network servers based on the epoll availability.
   */
  public static @NonNull ServerChannelFactory<? extends ServerChannel> serverChannelFactory() {
    return CURR_NETTY_TRANSPORT.serverChannelFactory();
  }

  /**
   * Writes the given integer value as a var int into the buffer.
   *
   * @param buffer the buffer to write to.
   * @param value  the value to write into the buffer.
   * @return the buffer used to call the method, for chaining.
   * @throws NullPointerException if the given byte buf is null.
   */
  public static @NonNull Buffer writeVarInt(@NonNull Buffer buffer, int value) {
    if ((value & -128) == 0) {
      buffer.ensureWritable(Byte.BYTES);
      buffer.writeByte((byte) value);
    } else if ((value & -16384) == 0) {
      var shortValue = (value & 0x7F | 0x80) << 8 | (value >>> 7);

      buffer.ensureWritable(Short.BYTES);
      buffer.writeShort((short) shortValue);
    } else {
      // pre-allocate a new size of +5 on the buffer to prevent multiple allocations during the iteration
      buffer.ensureWritable(Byte.BYTES * 5);

      while (true) {
        if ((value & -128) == 0) {
          buffer.writeByte((byte) value);
          return buffer;
        }

        buffer.writeByte((byte) (value & 0x7F | 0x80));
        value >>>= 7;
      }
    }

    return buffer;
  }

  /**
   * Reads a var int from the given buffer.
   *
   * @param buffer the buffer to read from.
   * @return the var int read from the buffer.
   * @throws SilentDecoderException if the buf current position has no var int.
   * @throws NullPointerException   if the given buffer to read from is null.
   */
  public static int readVarInt(@NonNull Buffer buffer) {
    var i = 0;
    var maxRead = Math.min(5, buffer.readableBytes());
    for (var j = 0; j < maxRead; j++) {
      int nextByte = buffer.readByte();
      i |= (nextByte & 0x7F) << j * 7;
      if ((nextByte & 0x80) != 128) {
        return i;
      }
    }
    throw INVALID_VAR_INT;
  }

  /**
   * Writes the given boolean into the given buffer, ensuring the buffer is large enough to fit the boolean.
   *
   * @param buffer the buffer to write the boolean to.
   * @param b      the boolean to write.
   * @throws NullPointerException if the given buffer is null.
   */
  public static void writeBoolean(@NonNull Buffer buffer, boolean b) {
    buffer.ensureWritable(Byte.BYTES);
    buffer.writeByte((byte) (b ? 1 : 0));
  }

  /**
   * Reads a boolean from the current reader offset of the given buffer.
   *
   * @param buffer the buffer to read the boolean from.
   * @return the boolean at current reader offset.
   * @throws NullPointerException if the given buffer is null.
   */
  public static boolean readBoolean(@NonNull Buffer buffer) {
    return buffer.readByte() > 0;
  }

  /**
   * Get the amount of bytes the given integer will consume when converted to a var int.
   *
   * @param varInt the var int to write.
   * @return the number of bytes the given var int takes when serializing.
   */
  public static int varIntByteAmount(int varInt) {
    return VAR_INT_LENGTHS[Integer.numberOfLeadingZeros(varInt)];
  }

  /**
   * Extracts the raw bytes from the given buffer.
   *
   * @param buffer the buffer to read the bytes of.
   * @return the raw bytes of the given buffer.
   * @throws NullPointerException if the given buffer is null.
   */
  public static byte[] extractBytes(@NonNull Buffer buffer) {
    var bytes = new byte[buffer.readableBytes()];
    buffer.copyInto(0, bytes, 0, bytes.length);
    return bytes;
  }

  /**
   * Get the buffer allocator instance used for all buffer operations made. This is (by default) a pooled heap buffer
   * allocator.
   *
   * @return the buffer allocator used for all buffer allocations.
   */
  public static @NonNull BufferAllocator allocator() {
    return ALLOCATOR;
  }

  /**
   * Get the thread amount used by the packet dispatcher to dispatch incoming packets. This method returns always 4 when
   * running in as a wrapper and the amount of processors cores multiplied by 2 when running either embedded or as a
   * node.
   *
   * @return the thread amount used by the packet dispatcher to dispatch incoming packets.
   */
  public static @Range(from = 2, to = Integer.MAX_VALUE) int threadAmount() {
    var environment = CloudNetDriver.instance().environment();
    return environment == DriverEnvironment.CLOUDNET ? Math.max(8, Runtime.getRuntime().availableProcessors() * 2) : 4;
  }

  /**
   * Get the selected netty transport which will be used for client/server channel and event loop group construction.
   *
   * @return the selected netty transport.
   */
  public static @NonNull NettyTransport selectedNettyTransport() {
    return CURR_NETTY_TRANSPORT;
  }
}
