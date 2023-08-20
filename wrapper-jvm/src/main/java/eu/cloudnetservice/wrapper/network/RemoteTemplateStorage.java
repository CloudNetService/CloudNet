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

package eu.cloudnetservice.wrapper.network;

import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.common.io.ListenableOutputStream;
import eu.cloudnetservice.common.io.ZipUtil;
import eu.cloudnetservice.driver.ComponentInfo;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.chunk.ChunkedPacketSender;
import eu.cloudnetservice.driver.network.chunk.TransferStatus;
import eu.cloudnetservice.driver.network.def.NetworkConstants;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.wrapper.network.chunk.TemplateStorageCallbackListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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
  private final TemplateStorageCallbackListener templateStorageCallbackListener;
  private final NetworkClient networkClient;

  /**
   * Constructs a new remote template storage instance.
   *
   * @param name                            the name of the storage which was created.
   * @param componentInfo                   the information about the current component.
   * @param templateStorageCallbackListener the callback listener for file transfer
   * @param networkClient                   the network client of the current component.
   * @throws NullPointerException if the given name, component info or network client is null.
   */
  public RemoteTemplateStorage(
    @NonNull String name,
    @NonNull ComponentInfo componentInfo,
    TemplateStorageCallbackListener templateStorageCallbackListener, @NonNull NetworkClient networkClient
  ) {
    this.name = name;
    this.componentInfo = componentInfo;
    this.templateStorageCallbackListener = templateStorageCallbackListener;
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
    return ChunkedPacketSender.forFileTransfer()
      .source(inputStream)
      .transferChannel("deploy_service_template")
      .withExtraData(DataBuf.empty().writeString(this.name).writeObject(target).writeBoolean(true))
      .toChannels(this.networkClient.firstChannel())
      .build()
      .transferChunkedData()
      .get(5, TimeUnit.MINUTES, TransferStatus.FAILURE) == TransferStatus.SUCCESS;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable InputStream zipTemplate(@NonNull ServiceTemplate template) {
    // send a request for the template to the node
    var responseId = UUID.randomUUID();
    this.templateStorageCallbackListener.startSession(responseId);
    var response = ChannelMessage.builder()
      .message("remote_templates_zip_template")
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .targetNode(this.componentInfo.nodeUniqueId())
      .buffer(DataBuf.empty().writeString(this.name).writeObject(template).writeUniqueId(responseId))
      .build()
      .sendSingleQuery();
    // check if we got a response
    if (response == null || !response.content().readBoolean()) {
      this.templateStorageCallbackListener.stopSession(responseId);
      return null;
    }
    // the file is transferred, but may not be fully written yet
    return this.templateStorageCallbackListener.waitForFile(responseId);
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
      $ -> ChunkedPacketSender.forFileTransfer()
        .forFile(localPath)
        .transferChannel("deploy_single_file")
        .toChannels(this.networkClient.firstChannel())
        .withExtraData(
          DataBuf.empty().writeString(this.name).writeObject(template).writeString(path).writeBoolean(append))
        .build()
        .transferChunkedData()
        .get(5, TimeUnit.MINUTES, null));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable InputStream newInputStream(
    @NonNull ServiceTemplate template,
    @NonNull String path
  ) {
    // send a request for the file to the node
    var responseId = UUID.randomUUID();
    this.templateStorageCallbackListener.startSession(responseId);
    var response = ChannelMessage.builder()
      .message("remote_templates_template_file")
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .targetNode(this.componentInfo.nodeUniqueId())
      .buffer(DataBuf.empty().writeString(path).writeString(this.name).writeObject(template).writeUniqueId(responseId))
      .build()
      .sendSingleQuery();
    // check if we got a response
    if (response == null || !response.content().readBoolean()) {
      this.templateStorageCallbackListener.stopSession(responseId);
      return null;
    }
    // the file is transferred, but may not be fully written yet
    return this.templateStorageCallbackListener.waitForFile(responseId);
  }
}
