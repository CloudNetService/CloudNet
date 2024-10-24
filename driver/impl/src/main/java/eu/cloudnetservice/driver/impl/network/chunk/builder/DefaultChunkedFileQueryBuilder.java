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

package eu.cloudnetservice.driver.impl.network.chunk.builder;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.channel.ChannelMessageTarget;
import eu.cloudnetservice.driver.impl.network.NetworkConstants;
import eu.cloudnetservice.driver.impl.network.chunk.ChunkedSessionRegistry;
import eu.cloudnetservice.driver.impl.network.chunk.DefaultFileChunkedPacketHandler;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.chunk.ChunkSessionInformation;
import eu.cloudnetservice.driver.network.chunk.ChunkedFileQueryBuilder;
import eu.cloudnetservice.driver.registry.AutoService;
import eu.cloudnetservice.utils.base.io.FileUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import lombok.NonNull;

/**
 * Default implementation of a builder for a file query from a remote network component.
 *
 * @since 4.0
 */
@AutoService(services = ChunkedFileQueryBuilder.class, name = "default", singleton = false)
public class DefaultChunkedFileQueryBuilder implements ChunkedFileQueryBuilder {

  private static final DataBuf EMPTY_BUFFER;

  static {
    // buffer is never used, so we just allocate an empty buffer and release it immediately
    EMPTY_BUFFER = DataBufFactory.defaultFactory().createWithExpectedSize(0);
    EMPTY_BUFFER.release();
  }

  private String dataIdentifier;
  private ChannelMessageTarget dataSource;
  private Consumer<DataBuf.Mutable> messageBufferConfigurator;
  private int chunkSize = DefaultChunkedPacketSenderBuilder.DEFAULT_CHUNK_SIZE;

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ChunkedFileQueryBuilder chunkSize(int chunkSize) {
    Preconditions.checkArgument(chunkSize > 0, "chunk size must be greater than 0");
    this.chunkSize = chunkSize;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ChunkedFileQueryBuilder dataIdentifier(@NonNull String dataIdentifier) {
    Preconditions.checkArgument(!dataIdentifier.isBlank(), "data identifier cannot be empty");
    this.dataIdentifier = dataIdentifier;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ChunkedFileQueryBuilder requestFromNode(@NonNull String nodeId) {
    this.dataSource = ChannelMessageTarget.of(ChannelMessageTarget.Type.NODE, nodeId);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ChunkedFileQueryBuilder requestFromService(@NonNull String serviceName) {
    this.dataSource = ChannelMessageTarget.of(ChannelMessageTarget.Type.SERVICE, serviceName);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ChunkedFileQueryBuilder configureMessageBuffer(@NonNull Consumer<DataBuf.Mutable> bufferConfigurer) {
    this.messageBufferConfigurator = bufferConfigurer;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull CompletableFuture<InputStream> query() {
    Preconditions.checkNotNull(this.dataIdentifier, "no data id provided");
    Preconditions.checkNotNull(this.dataSource, "no data source provided");

    // configure the base message buffer
    var sessionId = UUID.randomUUID();
    var queryBuffer = DataBuf.empty()
      .writeInt(this.chunkSize)
      .writeUniqueId(sessionId)
      .writeString(this.dataIdentifier);
    if (this.messageBufferConfigurator != null) {
      this.messageBufferConfigurator.accept(queryBuffer);
    }

    // register the session that is responsible for handling the response
    var responseFuture = new CompletableFuture<InputStream>();
    var sessionRegistry = InjectionLayer.boot().instance(ChunkedSessionRegistry.class);
    var sessionInfo = new ChunkSessionInformation(this.chunkSize, sessionId, "query:dummy", EMPTY_BUFFER);
    var handler = new DefaultFileChunkedPacketHandler(sessionInfo, (_, stream) -> !responseFuture.complete(stream));
    sessionRegistry.registerSession(sessionId, handler);

    // send the request to transmit the data
    var channelMessage = ChannelMessage.builder()
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .message("chunked_query_file")
      .target(this.dataSource)
      .buffer(queryBuffer)
      .build();
    return channelMessage
      .sendSingleQueryAsync()
      .thenCompose(response -> {
        var responseData = response.content();
        if (responseData.readBoolean()) {
          // transfer started successfully
          return responseFuture;
        } else {
          // transfer couldn't be started for some reason
          throw new IllegalStateException("unable to start chunked data transfer");
        }
      });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull CompletableFuture<Path> queryToTempFile() {
    var tempFile = FileUtil.createTempFile();
    return this.queryToPath(tempFile);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull CompletableFuture<Path> queryToPath(@NonNull Path target) {
    return this.query().thenApply(stream -> {
      try (stream) {
        FileUtil.copy(stream, target);
        return target;
      } catch (IOException exception) {
        throw new UncheckedIOException(exception);
      }
    });
  }
}
