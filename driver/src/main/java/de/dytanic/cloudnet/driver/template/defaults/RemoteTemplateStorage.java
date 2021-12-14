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

package de.dytanic.cloudnet.driver.template.defaults;

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.stream.ListeningOutputStream;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.chunk.ChunkedPacketSender;
import de.dytanic.cloudnet.driver.network.chunk.TransferStatus;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.network.rpc.RPC;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.FileInfo;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RemoteTemplateStorage implements TemplateStorage {

  private final String name;
  private final RPC baseRPC;
  private final RPCSender sender;

  public RemoteTemplateStorage(@NotNull String name, @NotNull RPC baseRPC) {
    this.name = name;
    this.baseRPC = baseRPC;
    this.sender = baseRPC.getSender().getFactory().providerForClass(
      baseRPC.getSender().getAssociatedComponent(),
      TemplateStorage.class);
  }

  @Override
  public @NotNull String name() {
    return this.name;
  }

  @Override
  public boolean deployDirectory(
    @NotNull Path directory,
    @NotNull ServiceTemplate target,
    @Nullable Predicate<Path> fileFilter
  ) {
    try (var inputStream = FileUtils.zipToStream(directory, fileFilter)) {
      return this.deploy(inputStream, target);
    } catch (IOException exception) {
      return false;
    }
  }

  @Override
  public boolean deploy(@NotNull InputStream inputStream, @NotNull ServiceTemplate target) {
    return ChunkedPacketSender.forFileTransfer()
      .source(inputStream)
      .transferChannel("deploy_service_template")
      .withExtraData(DataBuf.empty().writeString(this.name).writeObject(target).writeBoolean(true))
      .toChannels(CloudNetDriver.getInstance().getNetworkClient().getFirstChannel())
      .build()
      .transferChunkedData()
      .get(5, TimeUnit.MINUTES, TransferStatus.FAILURE) == TransferStatus.SUCCESS;
  }

  @Override
  public boolean copy(@NotNull ServiceTemplate template, @NotNull Path directory) {
    return this.baseRPC.join(this.sender.invokeMethod("copy", template, directory)).fireSync();
  }

  @Override
  public @Nullable InputStream zipTemplate(@NotNull ServiceTemplate template) throws IOException {
    // send a request for the template to the node
    var responseId = UUID.randomUUID();
    var response = ChannelMessage.builder()
      .message("remote_templates_zip_template")
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .targetNode(CloudNetDriver.getInstance().getNodeUniqueId())
      .buffer(DataBuf.empty().writeString(this.name).writeObject(template).writeUniqueId(responseId))
      .build()
      .sendSingleQuery();
    // check if we got a response
    if (response == null || !response.content().readBoolean()) {
      return null;
    }
    // the file is transferred and should be readable
    return Files.newInputStream(FileUtils.TEMP_DIR.resolve(responseId.toString()), StandardOpenOption.DELETE_ON_CLOSE);
  }

  @Override
  public boolean delete(@NotNull ServiceTemplate template) {
    return this.baseRPC.join(this.sender.invokeMethod("delete", template)).fireSync();
  }

  @Override
  public boolean create(@NotNull ServiceTemplate template) {
    return this.baseRPC.join(this.sender.invokeMethod("create", template)).fireSync();
  }

  @Override
  public boolean has(@NotNull ServiceTemplate template) {
    return this.baseRPC.join(this.sender.invokeMethod("has", template)).fireSync();
  }

  @Override
  public @Nullable OutputStream appendOutputStream(
    @NotNull ServiceTemplate template,
    @NotNull String path
  ) throws IOException {
    return this.openLocalOutputStream(template, path, FileUtils.createTempFile(), true);
  }

  @Override
  public @Nullable OutputStream newOutputStream(
    @NotNull ServiceTemplate template,
    @NotNull String path
  ) throws IOException {
    return this.openLocalOutputStream(template, path, FileUtils.createTempFile(), false);
  }

  protected @NotNull OutputStream openLocalOutputStream(
    @NotNull ServiceTemplate template,
    @NotNull String path,
    @NotNull Path localPath,
    boolean append
  ) throws IOException {
    return new ListeningOutputStream<>(
      Files.newOutputStream(localPath),
      $ -> ChunkedPacketSender.forFileTransfer()
        .forFile(localPath)
        .transferChannel("deploy_single_file")
        .toChannels(CloudNetDriver.getInstance().getNetworkClient().getFirstChannel())
        .withExtraData(
          DataBuf.empty().writeString(this.name).writeObject(template).writeString(path).writeBoolean(append))
        .build()
        .transferChunkedData()
        .get(5, TimeUnit.MINUTES, null));
  }

  @Override
  public boolean createFile(@NotNull ServiceTemplate template, @NotNull String path) {
    return this.baseRPC.join(this.sender.invokeMethod("createFile", template, path)).fireSync();
  }

  @Override
  public boolean createDirectory(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
    return this.baseRPC.join(this.sender.invokeMethod("createDirectory", template, path)).fireSync();
  }

  @Override
  public boolean hasFile(@NotNull ServiceTemplate template, @NotNull String path) {
    return this.baseRPC.join(this.sender.invokeMethod("hasFile", template, path)).fireSync();
  }

  @Override
  public boolean deleteFile(@NotNull ServiceTemplate template, @NotNull String path) {
    return this.baseRPC.join(this.sender.invokeMethod("deleteFile", template, path)).fireSync();
  }

  @Override
  public @Nullable InputStream newInputStream(
    @NotNull ServiceTemplate template,
    @NotNull String path
  ) throws IOException {
    // send a request for the file to the node
    var responseId = UUID.randomUUID();
    var response = ChannelMessage.builder()
      .message("remote_templates_template_file")
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .targetNode(CloudNetDriver.getInstance().getNodeUniqueId())
      .buffer(DataBuf.empty().writeString(path).writeString(this.name).writeObject(template).writeUniqueId(responseId))
      .build()
      .sendSingleQuery();
    // check if we got a response
    if (response == null || !response.content().readBoolean()) {
      return null;
    }
    // the file is transferred and should be readable
    return Files.newInputStream(FileUtils.TEMP_DIR.resolve(responseId.toString()), StandardOpenOption.DELETE_ON_CLOSE);
  }

  @Override
  public @Nullable FileInfo getFileInfo(@NotNull ServiceTemplate template, @NotNull String path) {
    return this.baseRPC.join(this.sender.invokeMethod("getFileInfo", template, path)).fireSync();
  }

  @Override
  public @Nullable FileInfo[] listFiles(@NotNull ServiceTemplate template, @NotNull String dir, boolean deep) {
    return this.baseRPC.join(this.sender.invokeMethod("listFiles", template, dir, deep)).fireSync();
  }

  @Override
  public @NotNull Collection<ServiceTemplate> getTemplates() {
    return this.baseRPC.join(this.sender.invokeMethod("getTemplates")).fireSync();
  }

  @Override
  public void close() throws IOException {
    this.baseRPC.join(this.sender.invokeMethod("close")).fireSync();
  }
}
