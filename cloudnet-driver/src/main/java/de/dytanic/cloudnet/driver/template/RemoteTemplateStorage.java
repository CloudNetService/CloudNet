/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.driver.template;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.CompletableTaskListener;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.stream.WrappedOutputStream;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.api.DriverAPIUser;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.chunk.ChunkedPacketBuilder;
import de.dytanic.cloudnet.driver.network.protocol.chunk.ChunkedQueryResponse;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.defaults.DefaultAsyncTemplateStorage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RemoteTemplateStorage extends DefaultAsyncTemplateStorage implements DriverAPIUser {

  private static final ExecutorService SERVICE = Executors.newFixedThreadPool(3);

  static {
    Runtime.getRuntime().addShutdownHook(new Thread(SERVICE::shutdownNow));
  }

  private final String name;
  private final Supplier<INetworkChannel> channelSupplier;

  public RemoteTemplateStorage(String name, Supplier<INetworkChannel> channelSupplier) {
    this.name = name;
    this.channelSupplier = channelSupplier;
  }

  @Override
  public INetworkChannel getNetworkChannel() {
    return this.channelSupplier.get();
  }

  @Override
  public @NotNull ITask<Boolean> deployAsync(@NotNull Path directory, @NotNull ServiceTemplate target,
    @Nullable Predicate<Path> fileFilter) {
    CompletableTask<Boolean> task = new CompletableTask<>();

    SERVICE.execute(() -> {
      try (InputStream inputStream = FileUtils
        .zipToStream(directory, fileFilter != null ? path -> fileFilter.test(path) : null)) {
        this.deployAsync(inputStream, target).addListener(new CompletableTaskListener<>(task));
      } catch (IOException exception) {
        task.fail(exception);
      }
    });

    return task;
  }

  @Override
  public @NotNull ITask<Boolean> deployAsync(@NotNull InputStream inputStream, @NotNull ServiceTemplate target) {
    try {
      return this
        .sendChunks(DriverAPIRequestType.DEPLOY_TEMPLATE_STREAM, inputStream, target, JsonDocument.newDocument(), true)
        .map(packet -> packet.getBuffer().readBoolean());
    } catch (IOException exception) {
      return CompletedTask.createFailed(exception);
    }
  }

  @Override
  public @NotNull ITask<Boolean> copyAsync(@NotNull ServiceTemplate template, @NotNull Path directory) {
    return this.zipTemplateAsync(template).mapThrowable(inputStream -> {
      if (inputStream == null) {
        return false;
      }

      return FileUtils.extract(inputStream, directory) != null;
    });
  }

  @Override
  public @NotNull ITask<InputStream> zipTemplateAsync(@NotNull ServiceTemplate template) {
    return this.executeChunkedWithTemplate(
      DriverAPIRequestType.LOAD_TEMPLATE_STREAM,
      template
    ).mapThrowable(chunkedResponse -> {
      TemplateStorageResponse response = chunkedResponse.getSession().getHeader()
        .get("response", TemplateStorageResponse.class);
      this.throwException(response, () -> ProtocolBuffer.readAll(chunkedResponse.getInputStream()).readThrowable());
      return chunkedResponse.getInputStream();
    });
  }

  @Override
  public @NotNull ITask<Boolean> deleteAsync(@NotNull ServiceTemplate template) {
    return this.executeWithTemplate(DriverAPIRequestType.DELETE_TEMPLATE, template);
  }

  @Override
  public @NotNull ITask<Boolean> createAsync(@NotNull ServiceTemplate template) {
    return this.executeWithTemplate(DriverAPIRequestType.CREATE_TEMPLATE, template);
  }

  @Override
  public @NotNull ITask<Boolean> hasAsync(@NotNull ServiceTemplate template) {
    return this.executeWithTemplate(DriverAPIRequestType.CONTAINS_TEMPLATE, template);
  }

  @Override
  public @NotNull ITask<OutputStream> appendOutputStreamAsync(@NotNull ServiceTemplate template, @NotNull String path) {
    return this.createOutputStreamTask(DriverAPIRequestType.APPEND_FILE_CONTENT, template, path);
  }

  @Override
  public @NotNull ITask<OutputStream> newOutputStreamAsync(@NotNull ServiceTemplate template, @NotNull String path) {
    return this.createOutputStreamTask(DriverAPIRequestType.SET_FILE_CONTENT, template, path);
  }

  private ITask<OutputStream> createOutputStreamTask(@NotNull DriverAPIRequestType requestType,
    @NotNull ServiceTemplate template, @NotNull String path) {
    Path tempFile = FileUtils.createTempFile();

    CompletableTask<OutputStream> task = new CompletableTask<>();

    SERVICE.execute(() -> {
      try {
        task.complete(this.wrapOutputStream(requestType, template, path, tempFile));
      } catch (IOException exception) {
        task.fail(exception);
      }
    });

    return task;
  }

  private OutputStream wrapOutputStream(@NotNull DriverAPIRequestType requestType, @NotNull ServiceTemplate template,
    @NotNull String path, @NotNull Path tempFile) throws IOException {
    return new WrappedOutputStream(Files.newOutputStream(tempFile)) {
      @Override
      public void close() throws IOException {
        super.close();
        try (InputStream inputStream = Files.newInputStream(tempFile, StandardOpenOption.DELETE_ON_CLOSE)) {
          RemoteTemplateStorage.this
            .sendChunks(requestType, inputStream, template, JsonDocument.newDocument("path", path), false);
        }
      }
    };
  }

  @Override
  public @NotNull ITask<Boolean> createFileAsync(@NotNull ServiceTemplate template, @NotNull String path) {
    return this.executeWithTemplate(DriverAPIRequestType.CREATE_FILE, template, path);
  }

  @Override
  public @NotNull ITask<Boolean> createDirectoryAsync(@NotNull ServiceTemplate template, @NotNull String path) {
    return this.executeWithTemplate(DriverAPIRequestType.CREATE_DIRECTORY, template, path);
  }

  @Override
  public @NotNull ITask<Boolean> hasFileAsync(@NotNull ServiceTemplate template, @NotNull String path) {
    return this.executeDriverAPIMethod(
      DriverAPIRequestType.CONTAINS_FILE,
      buffer -> this.writeDefaults(buffer, template).writeString(path),
      this::readDefaultBooleanResponse
    );
  }

  @Override
  public @NotNull ITask<Boolean> deleteFileAsync(@NotNull ServiceTemplate template, @NotNull String path) {
    return this.executeWithTemplate(DriverAPIRequestType.DELETE_FILE, template, path);
  }

  @Override
  public @NotNull ITask<InputStream> newInputStreamAsync(@NotNull ServiceTemplate template, @NotNull String path) {
    return this.executeChunkedWithTemplate(
      DriverAPIRequestType.GET_FILE_CONTENT,
      template,
      path
    ).mapThrowable(chunkedResponse -> {
      TemplateStorageResponse response = chunkedResponse.getSession().getHeader()
        .get("response", TemplateStorageResponse.class);
      this.throwException(response, () -> ProtocolBuffer.readAll(chunkedResponse.getInputStream()).readThrowable());
      return chunkedResponse.getInputStream();
    });
  }

  @Override
  public @NotNull ITask<FileInfo> getFileInfoAsync(@NotNull ServiceTemplate template, @NotNull String path) {
    return this.executeDriverAPIMethod(
      DriverAPIRequestType.GET_FILE_INFO,
      buffer -> this.writeDefaults(buffer, template).writeString(path),
      packet -> this.readDefaults(packet).getBuffer().readOptionalObject(FileInfo.class)
    );
  }

  @Override
  public @NotNull ITask<FileInfo[]> listFilesAsync(@NotNull ServiceTemplate template, @NotNull String dir,
    boolean deep) {
    return this.executeDriverAPIMethod(
      DriverAPIRequestType.LIST_FILES,
      buffer -> this.writeDefaults(buffer, template).writeString(dir).writeBoolean(deep),
      packet -> this.readDefaults(packet).getBuffer().readBoolean() ? packet.getBuffer().readObjectArray(FileInfo.class)
        : null
    );
  }

  @Override
  public @NotNull ITask<Collection<ServiceTemplate>> getTemplatesAsync() {
    return this.executeDriverAPIMethod(
      DriverAPIRequestType.GET_TEMPLATES,
      this::writeDefaults,
      packet -> packet.getBuffer().readObjectCollection(ServiceTemplate.class)
    );
  }

  @Override
  public @NotNull ITask<Void> closeAsync() {
    return this.executeVoidDriverAPIMethod(
      DriverAPIRequestType.CLOSE_STORAGE,
      this::writeDefaults
    );
  }

  @CanIgnoreReturnValue
  private ProtocolBuffer writeDefaults(ProtocolBuffer buffer, ServiceTemplate template) {
    return buffer.writeObject(template);
  }

  @CanIgnoreReturnValue
  private ProtocolBuffer writeDefaults(ProtocolBuffer buffer) {
    return buffer.writeString(this.name);
  }

  @CanIgnoreReturnValue
  private IPacket readDefaults(IPacket packet) throws IOException {
    TemplateStorageResponse response = packet.getBuffer().readEnumConstant(TemplateStorageResponse.class);
    this.throwException(response, packet.getBuffer());
    return packet;
  }

  private boolean readDefaultBooleanResponse(IPacket packet) throws IOException {
    TemplateStorageResponse response = packet.getBuffer().readEnumConstant(TemplateStorageResponse.class);
    this.throwException(response, packet.getBuffer());
    return response == TemplateStorageResponse.SUCCESS;
  }

  private void throwException(TemplateStorageResponse response, ProtocolBuffer buffer) throws IOException {
    this.throwException(response, buffer::readThrowable);
  }

  private void throwException(TemplateStorageResponse response, Supplier<Throwable> throwableSupplier)
    throws IOException {
    switch (response) {
      case EXCEPTION:
        Throwable throwable = throwableSupplier.get();
        if (throwable instanceof IOException) {
          throw (IOException) throwable;
        } else if (throwable instanceof RuntimeException) {
          throw (RuntimeException) throwable;
        }
        break;

      case TEMPLATE_STORAGE_NOT_FOUND:
        throw new IllegalArgumentException(String.format("TemplateStorage '%s' not found", this.name));

      default:
        break;
    }
  }

  private ITask<Boolean> executeWithTemplate(DriverAPIRequestType requestType, ServiceTemplate template) {
    return this.executeDriverAPIMethod(requestType, buffer -> this.writeDefaults(buffer, template),
      this::readDefaultBooleanResponse);
  }

  private ITask<Boolean> executeWithTemplate(DriverAPIRequestType requestType, ServiceTemplate template, String path) {
    return this.executeDriverAPIMethod(requestType, buffer -> this.writeDefaults(buffer, template).writeString(path),
      this::readDefaultBooleanResponse);
  }

  private ITask<ChunkedQueryResponse> executeChunkedWithTemplate(DriverAPIRequestType requestType,
    ServiceTemplate template) {
    return this.executeChunkedDriverAPIMethod(requestType, buffer -> this.writeDefaults(buffer, template));
  }

  private ITask<ChunkedQueryResponse> executeChunkedWithTemplate(DriverAPIRequestType requestType,
    ServiceTemplate template, String path) {
    return this
      .executeChunkedDriverAPIMethod(requestType, buffer -> this.writeDefaults(buffer, template).writeString(path));
  }

  private ITask<IPacket> sendChunks(DriverAPIRequestType requestType, InputStream inputStream, ServiceTemplate template,
    JsonDocument header, boolean query) throws IOException {
    ChunkedPacketBuilder builder = ChunkedPacketBuilder
      .newBuilder(PacketConstants.CLUSTER_TEMPLATE_STORAGE_CHUNK_SYNC_CHANNEL, inputStream)
      .header(header.append("type", requestType).append("template", template))
      .target(CloudNetDriver.getInstance().getNetworkClient().getChannels());

    ITask<IPacket> task = null;
    if (query) {
      task = CloudNetDriver.getInstance().getNetworkClient().getFirstChannel()
        .registerQueryResponseHandler(builder.uniqueId());
    }

    builder.complete();

    return task;
  }

  @Override
  public String getName() {
    return this.name;
  }
}
