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

package eu.cloudnetservice.driver.network.chunk;

import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.protocol.Packet;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import lombok.NonNull;

/**
 * A sender of chunked data in any form. It is responsible to transfer all data chunks to the target network component
 * of the transfer.
 *
 * @since 4.0
 */
public interface ChunkedPacketSender extends ChunkedPacketProvider {

  /**
   * Get a new builder for a packet sender which allows to transfer a stream of data in chunks through the network.
   *
   * @return a builder for a chunked data transfer.
   */
  static @NonNull ChunkedPacketSender.Builder forStreamTransfer() {
    return ServiceRegistry.registry().defaultInstance(ChunkedPacketSender.Builder.class);
  }

  /**
   * Get a new builder for a packet sender which allows to transfer a single but huge file, e.g. a zip folder in chunks
   * through the network.
   *
   * @param filePath the path to the file that should be transferred.
   * @return a builder for file based chunked packet transfer.
   * @throws NullPointerException     if the given file path is null.
   * @throws IllegalArgumentException if the given file path cannot be opened for reading.
   */
  static @NonNull ChunkedPacketSender.Builder forFileTransfer(@NonNull Path filePath) {
    try {
      var fileStream = Files.newInputStream(filePath, StandardOpenOption.READ);
      return forStreamTransfer().source(fileStream);
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to open file for reading: " + filePath, exception);
    }
  }

  /**
   * Transfers the data from the data source to all targets of this sender. The returned future is completed when:
   * <ul>
   *   <li>The transfer completed successfully.
   *   <li>The transfer failed for any reason. The returned task will be completed holding the reason exception.
   * </ul>
   *
   * @return a future completed when the transfer finishes.
   */
  @NonNull
  CompletableFuture<TransferStatus> transferChunkedData();

  /**
   * A builder for a chunked packet sender, holding all general options.
   *
   * @since 4.0
   */
  interface Builder {

    /**
     * Sets the size each chunk has except for the last one must have. This defaults to 1 MB. The supplied value must be
     * greater than 0.
     *
     * @param chunkSize the size of each transferred chunk.
     * @return the same builder as used to call the method, for chaining.
     */
    @NonNull
    Builder chunkSize(int chunkSize);

    /**
     * Sets the unique id of the session. This defaults to a random id.
     *
     * @param uuid the session id to use.
     * @return the same builder as used to call the method, for chaining.
     * @throws NullPointerException if the given id is null.
     */
    @NonNull
    Builder sessionUniqueId(@NonNull UUID uuid);

    /**
     * Sets the name of the transfer channel. This option is required to be set by yourself. The channel role is just
     * identification of the incoming data. There should never be two channels named the same way.
     *
     * @param transferChannel the transfer channel name to use.
     * @return the same builder as used to call the method, for chaining.
     * @throws NullPointerException if the given channel name is null.
     */
    @NonNull
    Builder transferChannel(@NonNull String transferChannel);

    /**
     * Sets the source of this data transfer. This option is required to be set by yourself. The source stream should
     * not be closed by you, it will be closed when the transfer finished successfully.
     *
     * @param source the data source of the transfer.
     * @return the same builder as used to call the method, for chaining.
     * @throws NullPointerException if the given source is null.
     */
    @NonNull
    Builder source(@NonNull InputStream source);

    /**
     * Sends each chunk data packet to all the provided channels. You need to call one of these methods:
     * <ul>
     *   <li>{@code toChannels(NetworkChannel...)}
     *   <li>{@code toChannels(Collection)}
     *   <li>{@code packetSplitter(Consumer)}
     * </ul>
     * to set the packet splitter of this sender which is required.
     *
     * @param channels the channels to send the packet to.
     * @return the same builder as used to call the method, for chaining.
     * @throws NullPointerException if one of the given channels is null.
     */
    @NonNull
    Builder toChannels(NetworkChannel @NonNull ... channels);

    /**
     * Sends each chunk data packet to all the provided channels. You need to call one of these methods:
     * <ul>
     *   <li>{@code toChannels(NetworkChannel...)}
     *   <li>{@code toChannels(Collection)}
     *   <li>{@code packetSplitter(Consumer)}
     * </ul>
     * to set the packet splitter of this sender which is required.
     *
     * @param channels the channels to send the packet to.
     * @return the same builder as used to call the method, for chaining.
     * @throws NullPointerException if the given channel collection is null.
     */
    @NonNull
    Builder toChannels(@NonNull Collection<NetworkChannel> channels);

    /**
     * Sets the handler and processor of each packet which will be sent during the chunked data transfer. You need to
     * call one of these methods:
     * <ul>
     *   <li>{@code toChannels(NetworkChannel...)}
     *   <li>{@code toChannels(Collection)}
     *   <li>{@code packetSplitter(Consumer)}
     * </ul>
     * to set the packet splitter of this sender which is required.
     *
     * @param splitter the custom packet splitter to use.
     * @return the same builder as used to call the method, for chaining.
     * @throws NullPointerException if the given splitter is null.
     */
    @NonNull
    Builder packetSplitter(@NonNull Consumer<Packet> splitter);

    /**
     * Sets the extra information provided to each target component when opening a chunked session. The data is mainly
     * used for identifying specific parts of the transfer, for example the target file name might be a use case. This
     * defaults to an empty buffer.
     *
     * @param extraData the extra data to sent initially.
     * @return the same builder as used to call the method, for chaining.
     * @throws NullPointerException if the data buffer is null.
     */
    @NonNull
    Builder withExtraData(@NonNull DataBuf extraData);

    /**
     * Builds the chunked packet sender based on the supplied information in this builder.
     *
     * @return the instance build from the information.
     * @throws NullPointerException     if no source, splitter or channel were given.
     * @throws IllegalArgumentException if the chunk size is not greater than 0.
     */
    @NonNull
    ChunkedPacketSender build();
  }
}
