/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.impl.network.netty;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import eu.cloudnetservice.driver.DriverEnvironment;
import eu.cloudnetservice.driver.impl.network.netty.buffer.NettyNioBufferReleasingAllocator;
import eu.cloudnetservice.driver.impl.network.scheduler.NetworkTaskScheduler;
import eu.cloudnetservice.driver.impl.network.scheduler.ScalingNetworkTaskScheduler;
import io.netty5.buffer.Buffer;
import io.netty5.buffer.BufferAllocator;
import io.netty5.buffer.BufferUtil;
import io.netty5.buffer.DefaultBufferAllocators;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelFactory;
import io.netty5.channel.EventLoopGroup;
import io.netty5.channel.ServerChannel;
import io.netty5.channel.ServerChannelFactory;
import io.netty5.handler.codec.DecoderException;
import io.netty5.handler.ssl.OpenSsl;
import io.netty5.handler.ssl.SslProvider;
import io.netty5.util.ResourceLeakDetector;
import java.util.concurrent.Executors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Internal util for the default netty based communication between server and clients, http and websocket.
 *
 * @since 4.0
 */
public final class NettyUtil {

  private static final int PACKET_DISPATCH_THREADS;
  private static final int NETTY_EVENT_LOOP_THREADS;

  private static final SslProvider SELECTED_SSL_PROVIDER;
  private static final NettyTransport SELECTED_NETTY_TRANSPORT;
  private static final BufferAllocator SELECTED_BUFFER_ALLOCATOR;

  static {
    // check if resource leak detection should be enabled for debugging purposes
    // if that is not the case leak detection will be disabled completely
    var enableLeakDetection = Boolean.getBoolean("cloudnet.net.leak-detection-enabled");
    if (enableLeakDetection) {
      ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
      System.setProperty("io.netty5.buffer.leakDetectionEnabled", "true");
    } else {
      ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
      System.setProperty("io.netty5.buffer.leakDetectionEnabled", "false");
    }

    // select the ssl provider to use for netty. this uses the jdk provider in case it was explicitly selected
    // or openssl is not available. the default choice is openssl if available.
    var preferredSslProvider = System.getProperty("cloudnet.net.preferred-ssl-provider");
    if ("jdk".equals(preferredSslProvider) || !OpenSsl.isAvailable()) {
      SELECTED_SSL_PROVIDER = SslProvider.JDK;
    } else {
      SELECTED_SSL_PROVIDER = SslProvider.OPENSSL;
    }

    // select the buffer allocator to use. our internal allocator will free all buffers provided to it directly
    // which significantly reduces the native memory usage. however, this might not be the designated behaviour for
    // some users, therefore we leave it to their choice which allocator should be used.
    var preferredBufferAllocator = System.getProperty("cloudnet.net.preferred-buffer-allocator");
    if ("netty-default".equals(preferredBufferAllocator) || NettyNioBufferReleasingAllocator.notAbleToFreeBuffers()) {
      SELECTED_BUFFER_ALLOCATOR = DefaultBufferAllocators.offHeapAllocator();
    } else {
      SELECTED_BUFFER_ALLOCATOR = new NettyNioBufferReleasingAllocator();
    }

    // select the transport type to use for netty
    var disableNativeTransport = Boolean.getBoolean("cloudnet.net.no-native");
    SELECTED_NETTY_TRANSPORT = NettyTransport.availableTransport(disableNativeTransport);

    // get the values defined by the user or fall back to using -1, these will later be remapped to
    // actual values when the whole context for the allocation is known.
    PACKET_DISPATCH_THREADS = Integer.getInteger("cloudnet.net.packet-dispatch-threads", -1);
    NETTY_EVENT_LOOP_THREADS = Integer.getInteger("cloudnet.net.netty-event-loop-threads", -1);
  }

  private NettyUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the given overridden count if given (not zero or negative) or returns the given default value.
   *
   * @param overriddenSetting the overridden setting, for example from a user input.
   * @param defaultValue      the default value to use as a fallback, if the overridden value is not valid.
   * @return the given overridden setting if valid, the given default value in all other cases.
   */
  private static int overriddenCountOrDefault(int overriddenSetting, int defaultValue) {
    return overriddenSetting >= 1 ? overriddenSetting : defaultValue;
  }

  /**
   * Creates a new executor for all incoming packets. The thread size of the returned dispatcher depends either on a
   * user-provided setting or on the given driver environment.
   *
   * @param driverEnvironment the driver environment currently running on.
   * @return a newly created executor for dispatching inbound packets.
   * @throws NullPointerException if the given driver environment is null.
   */
  public static @NonNull NetworkTaskScheduler createPacketDispatcher(@NonNull DriverEnvironment driverEnvironment) {
    // the maximum thread count that the pool will be allowed to use for packet processing
    // TODO: consider moving the default thread amount for an environment into the environment as a property
    var defaultEnvThreadCount = driverEnvironment.equals(DriverEnvironment.NODE) ? 12 : 4;
    var maximumPoolSize = overriddenCountOrDefault(PACKET_DISPATCH_THREADS, defaultEnvThreadCount);

    var threadFactory = new ThreadFactoryBuilder()
      .setNameFormat("Packet-Dispatcher-%d")
      .setThreadFactory(Executors.defaultThreadFactory())
      .build();
    return new ScalingNetworkTaskScheduler(threadFactory, maximumPoolSize);
  }

  /**
   * Creates a new boss event loop group based on the selected netty transport. Boss event loops are used to accept new
   * connections, which only requires a single thread.
   *
   * @return a newly created boss event loop group.
   */
  public static @NonNull EventLoopGroup createBossEventLoopGroup() {
    return SELECTED_NETTY_TRANSPORT.createEventLoopGroup(1);
  }

  /**
   * Creates a new worker event loop group based on the selected netty transport. The thread count that is used by the
   * event loop is either set by the user or depends on the given driver environment.
   *
   * @param driverEnvironment the driver environment currently running on.
   * @return a newly created worker event loop group.
   * @throws NullPointerException if the given driver environment is null.
   */
  public static @NonNull EventLoopGroup createWorkerEventLoopGroup(@NonNull DriverEnvironment driverEnvironment) {
    // use a relatively small thread count for netty, most of the work will be loaded into the processing
    // packet executor when handling anyway. this also means that packets, when coming in really fast, are throttled
    // by default when reading, which should reduce the risk of huge amounts of allocated memory due to that
    // TODO: consider moving the default thread amount for an environment into the environment as a property
    var defaultEnvThreadCount = driverEnvironment.equals(DriverEnvironment.NODE) ? 6 : 2;
    var threadCount = overriddenCountOrDefault(NETTY_EVENT_LOOP_THREADS, defaultEnvThreadCount);
    return SELECTED_NETTY_TRANSPORT.createEventLoopGroup(threadCount);
  }

  /**
   * Get the channel factory for client channels of the selected netty transport.
   *
   * @return the channel factory for client channels of the selected netty transport.
   */
  public static @NonNull ChannelFactory<? extends Channel> clientChannelFactory() {
    return SELECTED_NETTY_TRANSPORT.clientChannelFactory();
  }

  /**
   * Get the channel factory for server channels of the selected netty transport.
   *
   * @return the channel factory for server channels of the selected netty transport.
   */
  public static @NonNull ServerChannelFactory<? extends ServerChannel> serverChannelFactory() {
    return SELECTED_NETTY_TRANSPORT.serverChannelFactory();
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
   * @param value the number to get the amount of bytes for.
   * @return the number of bytes writing the given number as a var int will take.
   */
  public static int varIntBytes(int value) {
    if (value < 0 || value >= 268_435_456) {
      return 5;
    } else if (value < 128) {
      return 1;
    } else if (value < 16_384) {
      return 2;
    } else if (value < 2_097_152) {
      return 3;
    } else {
      return 4;
    }
  }

  /**
   * Get the selected netty transport which will be used for client/server channel and event loop group construction.
   *
   * @return the selected netty transport.
   */
  public static @NonNull NettyTransport selectedNettyTransport() {
    return SELECTED_NETTY_TRANSPORT;
  }

  /**
   * Get the selected ssl provider which will be used in case ssl configurations are enabled on the client or server.
   *
   * @return the selected ssl provider.
   */
  public static @NonNull SslProvider selectedSslProvider() {
    return SELECTED_SSL_PROVIDER;
  }

  /**
   * Get the selected allocator for buffers that should be used for all buffer allocations.
   *
   * @return the selected allocator for buffers that should be used for all buffer allocations.
   */
  public static @NonNull BufferAllocator selectedBufferAllocator() {
    return SELECTED_BUFFER_ALLOCATOR;
  }
}
