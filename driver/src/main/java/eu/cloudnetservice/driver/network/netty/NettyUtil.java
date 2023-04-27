/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.network.netty;

import eu.cloudnetservice.driver.DriverEnvironment;
import eu.cloudnetservice.driver.util.ExecutorServiceUtil;
import io.netty5.buffer.Buffer;
import io.netty5.buffer.BufferUtil;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelFactory;
import io.netty5.channel.EventLoopGroup;
import io.netty5.channel.ServerChannel;
import io.netty5.channel.ServerChannelFactory;
import io.netty5.handler.codec.DecoderException;
import io.netty5.util.ResourceLeakDetector;
import io.netty5.util.concurrent.Future;
import java.net.ProtocolFamily;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

/**
 * Internal util for the default netty based communication between server and clients, http and websocket.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class NettyUtil {

  // pre-computed var int byte lengths
  private static final int[] VAR_INT_BYTE_LENGTHS = new int[33];
  // transport
  private static final boolean NO_NATIVE_TRANSPORT = Boolean.getBoolean("cloudnet.no-native");
  private static final NettyTransport CURR_NETTY_TRANSPORT = NettyTransport.availableTransport(NO_NATIVE_TRANSPORT);
  // packet thread handling
  private static final RejectedExecutionHandler DEFAULT_REJECT_HANDLER = new ThreadPoolExecutor.CallerRunsPolicy();

  static {
    // check if the leak detection level is set before overriding it
    // may be useful for debugging of the network
    if (System.getProperty("io.netty5.leakDetection.level") == null) {
      ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
    }

    // pre-compute all var int byte lengths
    for (var i = 0; i <= 32; ++i) {
      VAR_INT_BYTE_LENGTHS[i] = (int) Math.ceil((31d - (i - 1)) / 7d);
    }
    // 0 is always one byte long
    VAR_INT_BYTE_LENGTHS[32] = 1;
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
   * @param driverEnvironment the driver environment to get the executor for.
   * @return a new packet dispatcher instance.
   * @throws NullPointerException if the given environment is null.
   * @see #threadAmount(DriverEnvironment)
   */
  public static @NonNull Executor newPacketDispatcher(@NonNull DriverEnvironment driverEnvironment) {
    // a cached pool with a thread idle-lifetime of 30 seconds
    // rejected tasks will be executed on the calling thread (See ThreadPoolExecutor.CallerRunsPolicy)
    // at least one thread is always idling in this executor
    var maximumPoolSize = threadAmount(driverEnvironment);
    return ExecutorServiceUtil.newVirtualThreadExecutor("Packet-Dispatcher-", threadFactory -> new ThreadPoolExecutor(
      maximumPoolSize,
      maximumPoolSize,
      30L,
      TimeUnit.SECONDS,
      new LinkedBlockingQueue<>(),
      threadFactory,
      DEFAULT_REJECT_HANDLER));
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
   * @param protocolFamily the protocol Family the channel should created for.
   * @return a new channel factory for network clients based on the epoll availability.
   */
  public static @NonNull ChannelFactory<? extends Channel> clientChannelFactory(@NonNull ProtocolFamily protocolFamily) {
    return CURR_NETTY_TRANSPORT.clientChannelFactory(protocolFamily);
  }

  /**
   * Creates a new channel factory for network servers based on the epoll availability.
   *
   * @param protocolFamily the protocol Family the channel should created for.
   * @return a new channel factory for network servers based on the epoll availability.
   */
  public static @NonNull ServerChannelFactory<? extends ServerChannel> serverChannelFactory(@NonNull ProtocolFamily protocolFamily) {
    return CURR_NETTY_TRANSPORT.serverChannelFactory(protocolFamily);
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
    while (true) {
      if ((value & ~0x7F) == 0) {
        buffer.writeByte((byte) value);
        return buffer;
      } else {
        buffer.writeByte((byte) ((value & 0x7F) | 0x80));
        value >>>= 7;
      }
    }
  }

  /**
   * Reads a var int from the given buffer.
   *
   * @param buffer the buffer to read from.
   * @return the var int read from the buffer.
   * @throws DecoderException     if the buf current position has no var int.
   * @throws NullPointerException if the given buffer to read from is null.
   */
  public static int readVarInt(@NonNull Buffer buffer) {
    var varInt = readVarIntOrNull(buffer);
    if (varInt == null) {
      // unable to decode a var int at the current position
      var bufferDump = BufferUtil.hexDump(buffer, 0, buffer.readableBytes());
      throw new DecoderException(String.format(
        "Unable to decode VarInt at current buffer position (%d): %s",
        buffer.readerOffset(),
        bufferDump));
    }

    return varInt;
  }

  /**
   * Reads a var int from the given buffer, returns null if there is no Var Int at the current buffer position.
   *
   * @param buffer the buffer to read from.
   * @return the var int read from the buffer, or null if no var int is at the given position.
   * @throws NullPointerException if the given buffer to read from is null.
   */
  public static @Nullable Integer readVarIntOrNull(@NonNull Buffer buffer) {
    var i = 0;
    var maxRead = Math.min(5, buffer.readableBytes());
    for (var j = 0; j < maxRead; j++) {
      var nextByte = buffer.readByte();
      i |= (nextByte & 0x7F) << j * 7;
      if ((nextByte & 0x80) != 128) {
        return i;
      }
    }
    return null;
  }

  /**
   * Gets the number of bytes that writing the given content length as a var int will take in the underlying buffer.
   *
   * @param contentLength the number to get the amount of bytes for.
   * @return the number of bytes writing the given number as a var int will take.
   */
  public static int varIntBytes(int contentLength) {
    return VAR_INT_BYTE_LENGTHS[Integer.numberOfLeadingZeros(contentLength)];
  }

  /**
   * Waits for the given future to complete, either returning the same future instance as given (but completed) or
   * rethrowing all exceptions that occurred during completion. This method throws an IllegalThreadStateException if the
   * current thread was interrupted during the future computation.
   *
   * @param future the future to wait for.
   * @param <T>    the type of data returned by the future.
   * @return the same future as given to the method, but completed.
   * @throws NullPointerException        if the given future is null.
   * @throws CancellationException       if the computation was cancelled
   * @throws CompletionException         if the computation threw an exception.
   * @throws IllegalThreadStateException if the current thread was interrupted during computation.
   */
  public static @NonNull <T> Future<T> awaitFuture(@NonNull Future<T> future) {
    try {
      // await the future and rethrow exceptions if any occur
      return future.asStage().sync().future();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt(); // reset the interrupted state of the thread
      throw new IllegalThreadStateException();
    }
  }

  /**
   * Get the thread amount used by the packet dispatcher to dispatch incoming packets. This method returns always 4 when
   * running in as a wrapper and the amount of processors cores multiplied by 2 when running either embedded or as a
   * node.
   *
   * @param environment the environment to get the thread count for.
   * @return the thread amount used by the packet dispatcher to dispatch incoming packets.
   * @throws NullPointerException if the given environment is null.
   */
  public static @Range(from = 2, to = Integer.MAX_VALUE) int threadAmount(@NonNull DriverEnvironment environment) {
    return environment.equals(DriverEnvironment.NODE) ? Math.max(8, Runtime.getRuntime().availableProcessors() * 2) : 4;
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
