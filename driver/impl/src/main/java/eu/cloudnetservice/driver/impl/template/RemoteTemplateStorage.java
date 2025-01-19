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

package eu.cloudnetservice.driver.impl.template;

import eu.cloudnetservice.driver.ComponentInfo;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.chunk.ChunkedFileQueryBuilder;
import eu.cloudnetservice.driver.network.chunk.ChunkedPacketSender;
import eu.cloudnetservice.driver.network.chunk.TransferStatus;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCInvocationTarget;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.utils.base.concurrent.TaskUtil;
import eu.cloudnetservice.utils.base.io.FileUtil;
import eu.cloudnetservice.utils.base.io.ListenableOutputStream;
import eu.cloudnetservice.utils.base.io.ZipUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default implementation of a template storage which pulls its information from a remote provider.
 *
 * @since 4.0
 */
public abstract class RemoteTemplateStorage implements TemplateStorage {

  private final String name;
  private final ComponentInfo componentInfo;
  private final NetworkClient networkClient;

  /**
   * Constructs a new remote template storage instance.
   *
   * @param name          the name of the storage which was created.
   * @param componentInfo the information about the current component.
   * @param networkClient the network client of the current component.
   * @throws NullPointerException if the given name, component info or network client is null.
   */
  @RPCInvocationTarget
  public RemoteTemplateStorage(
    @NonNull String name,
    @NonNull ComponentInfo componentInfo,
    @NonNull NetworkClient networkClient
  ) {
    this.name = name;
    this.componentInfo = componentInfo;
    this.networkClient = networkClient;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String name() {
    return this.name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean deployDirectory(
    @NonNull ServiceTemplate target,
    @NonNull Path directory,
    @Nullable Predicate<Path> filter
  ) {
    try (var inputStream = ZipUtil.zipToStream(directory, filter)) {
      return this.deploy(target, inputStream);
    } catch (IOException exception) {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean deploy(@NonNull ServiceTemplate target, @NonNull InputStream inputStream) {
    return TaskUtil.getOrDefault(this.deployAsync(target, inputStream), false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull CompletableFuture<Boolean> deployAsync(
    @NonNull ServiceTemplate target,
    @NonNull InputStream inputStream
  ) {
    return ChunkedPacketSender.forStreamTransfer()
      .source(inputStream)
      .transferChannel("deploy_service_template")
      .withExtraData(DataBuf.empty().writeString(this.name).writeObject(target).writeBoolean(true))
      .toChannels(this.networkClient.firstChannel())
      .build()
      .transferChunkedData()
      .thenApply(status -> status == TransferStatus.SUCCESS);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable InputStream zipTemplate(@NonNull ServiceTemplate template) {
    return TaskUtil.getOrDefault(this.zipTemplateAsync(template), null);
  }

  @Override
  public @NonNull CompletableFuture<InputStream> zipTemplateAsync(@NonNull ServiceTemplate template) {
    return ChunkedFileQueryBuilder.create()
      .dataIdentifier("remote_templates_zip_template")
      .requestFromNode(this.componentInfo.nodeUniqueId())
      .configureMessageBuffer(buffer -> buffer.writeString(this.name).writeObject(template))
      .query();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull OutputStream appendOutputStream(
    @NonNull ServiceTemplate template,
    @NonNull String path
  ) throws IOException {
    return this.openLocalOutputStream(template, path, FileUtil.createTempFile(), true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull OutputStream newOutputStream(
    @NonNull ServiceTemplate template,
    @NonNull String path
  ) throws IOException {
    return this.openLocalOutputStream(template, path, FileUtil.createTempFile(), false);
  }

  /**
   * Opens a new output stream to a temporary local file which will be deployed to the remote component once the stream
   * was closed.
   *
   * @param template  the template in which the file to open the stream for is located.
   * @param path      the path to the file in the given template for which the stream was opened.
   * @param localPath the local path to the file which should be used as a temporary location to write to.
   * @param append    if the stream should append to the content in the remote file.
   * @return a new output stream which writes to a local temp files and deploys its data once closed.
   * @throws IOException          if an I/O error occurs while opening the stream.
   * @throws NullPointerException if one of the given parameters is null.
   */
  private @NonNull OutputStream openLocalOutputStream(
    @NonNull ServiceTemplate template,
    @NonNull String path,
    @NonNull Path localPath,
    boolean append
  ) throws IOException {
    return new ListenableOutputStream<>(
      Files.newOutputStream(localPath),
      _ -> ChunkedPacketSender.forFileTransfer(localPath)
        .transferChannel("deploy_single_file")
        .toChannels(this.networkClient.firstChannel())
        .withExtraData(DataBuf.empty()
          .writeString(this.name)
          .writeObject(template)
          .writeString(path)
          .writeBoolean(append))
        .build()
        .transferChunkedData()
        .join());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable InputStream newInputStream(
    @NonNull ServiceTemplate template,
    @NonNull String path
  ) throws IOException {
    return TaskUtil.getOrDefault(this.newInputStreamAsync(template, path), null);
  }

  @Override
  public @NonNull CompletableFuture<InputStream> newInputStreamAsync(
    @NonNull ServiceTemplate template,
    @NonNull String path
  ) {
    return ChunkedFileQueryBuilder.create()
      .dataIdentifier("remote_templates_template_file")
      .requestFromNode(this.componentInfo.nodeUniqueId())
      .configureMessageBuffer(buffer -> buffer.writeString(this.name).writeObject(template).writeString(path))
      .query();
  }
}
