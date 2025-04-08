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

import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import lombok.NonNull;

/**
 * A builder that can be used to query a file from a remote network component.
 *
 * @since 4.0
 */
public interface ChunkedFileQueryBuilder {

  /**
   * Constructs a new default implementation of this builder.
   *
   * @return a new default implementation of this builder.
   */
  static @NonNull ChunkedFileQueryBuilder create() {
    return ServiceRegistry.registry().defaultInstance(ChunkedFileQueryBuilder.class);
  }

  /**
   * The size of the chunks in bytes that will be transferred from the remote to the caller.
   *
   * @param chunkSize the size of the chunks that should be transferred from the remote to the caller.
   * @return this builder, for chaining.
   * @throws IllegalArgumentException if the given chunk size is not positive.
   */
  @NonNull
  ChunkedFileQueryBuilder chunkSize(int chunkSize);

  /**
   * The identifier for the data should be transferred from the remote to the caller.
   *
   * @param dataIdentifier the identifier of the data that should be transferred.
   * @return this builder, for chaining.
   * @throws NullPointerException     if the given identifier is null.
   * @throws IllegalArgumentException if the given identifier is empty.
   */
  @NonNull
  ChunkedFileQueryBuilder dataIdentifier(@NonNull String dataIdentifier);

  /**
   * Sets that the file data should be requested from the node with the given id. Note: the source must be directly
   * available to the caller, else the data will never reach the caller.
   *
   * @param nodeId the id of the node to request the data from.
   * @return this builder, for chaining.
   * @throws NullPointerException if the given node id is null.
   */
  @NonNull
  ChunkedFileQueryBuilder requestFromNode(@NonNull String nodeId);

  /**
   * Sets that the file data should be requested from the service with the given name. Note: the source must be directly
   * available to the caller, else the data will never reach the caller.
   *
   * @param serviceName the name of the service to request the data from.
   * @return this builder, for chaining.
   * @throws NullPointerException if the given service name is null.
   */
  @NonNull
  ChunkedFileQueryBuilder requestFromService(@NonNull String serviceName);

  /**
   * A consumer that is called when the actual request is sent to the data source. The consumer can append additional
   * information to the buffer that is relevant for the remote side to identify the data to transmit.
   *
   * @param bufferConfigurer the configurator for additional transmitted information about the request.
   * @return this builder, for chaining.
   * @throws NullPointerException if the given buffer configurator is null.
   */
  @NonNull
  ChunkedFileQueryBuilder configureMessageBuffer(@NonNull Consumer<DataBuf.Mutable> bufferConfigurer);

  /**
   * Queries the data from the remote and completes with an input stream when the transmission was successful.
   *
   * @return a future completed with an input stream of the transmitted data.
   */
  @NonNull
  CompletableFuture<InputStream> query();

  /**
   * Queries the data from the remote and redirects it into a temporary file. The path to the file is wrapped in the
   * returned future.
   *
   * @return a future completed with the path to which the transmitted contents were written.
   */
  @NonNull
  CompletableFuture<Path> queryToTempFile();

  /**
   * Queries the data from the remote and redirects it into the given file path.
   *
   * @param target the path to the file into which the response data should be written.
   * @return a future completed with the path to which the transmitted contents were written.
   * @throws NullPointerException if the given target path is null.
   */
  @NonNull
  CompletableFuture<Path> queryToPath(@NonNull Path target);
}
