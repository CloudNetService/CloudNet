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

package eu.cloudnetservice.cloudnet.driver.template.defaults;

import eu.cloudnetservice.cloudnet.common.io.FileUtil;
import eu.cloudnetservice.cloudnet.common.io.ZipUtil;
import eu.cloudnetservice.cloudnet.common.stream.ListeningOutputStream;
import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessage;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.network.chunk.ChunkedPacketSender;
import eu.cloudnetservice.cloudnet.driver.network.chunk.TransferStatus;
import eu.cloudnetservice.cloudnet.driver.network.def.NetworkConstants;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPC;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCSender;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTemplate;
import eu.cloudnetservice.cloudnet.driver.template.FileInfo;
import eu.cloudnetservice.cloudnet.driver.template.TemplateStorage;
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
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class RemoteTemplateStorage implements TemplateStorage {

  private final String name;
  private final RPC baseRPC;
  private final RPCSender sender;

  public RemoteTemplateStorage(@NonNull String name, @NonNull RPC baseRPC) {
    this.name = name;
    this.baseRPC = baseRPC;
    this.sender = baseRPC.sender().factory().providerForClass(
      baseRPC.sender().associatedComponent(),
      TemplateStorage.class);
  }

  @Override
  public @NonNull String name() {
    return this.name;
  }

  @Override
  public boolean deployDirectory(
    @NonNull Path directory,
    @NonNull ServiceTemplate target,
    @Nullable Predicate<Path> fileFilter
  ) {
    try (var inputStream = ZipUtil.zipToStream(directory, fileFilter)) {
      return this.deploy(inputStream, target);
    } catch (IOException exception) {
      return false;
    }
  }

  @Override
  public boolean deploy(@NonNull InputStream inputStream, @NonNull ServiceTemplate target) {
    return ChunkedPacketSender.forFileTransfer()
      .source(inputStream)
      .transferChannel("deploy_service_template")
      .withExtraData(DataBuf.empty().writeString(this.name).writeObject(target).writeBoolean(true))
      .toChannels(CloudNetDriver.instance().networkClient().firstChannel())
      .build()
      .transferChunkedData()
      .get(5, TimeUnit.MINUTES, TransferStatus.FAILURE) == TransferStatus.SUCCESS;
  }

  @Override
  public boolean copy(@NonNull ServiceTemplate template, @NonNull Path directory) {
    return this.baseRPC.join(this.sender.invokeMethod("copy", template, directory)).fireSync();
  }

  @Override
  public @Nullable InputStream zipTemplate(@NonNull ServiceTemplate template) throws IOException {
    // send a request for the template to the node
    var responseId = UUID.randomUUID();
    var response = ChannelMessage.builder()
      .message("remote_templates_zip_template")
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .targetNode(CloudNetDriver.instance().nodeUniqueId())
      .buffer(DataBuf.empty().writeString(this.name).writeObject(template).writeUniqueId(responseId))
      .build()
      .sendSingleQuery();
    // check if we got a response
    if (response == null || !response.content().readBoolean()) {
      return null;
    }
    // the file is transferred and should be readable
    return Files.newInputStream(FileUtil.TEMP_DIR.resolve(responseId.toString()), StandardOpenOption.DELETE_ON_CLOSE);
  }

  @Override
  public boolean delete(@NonNull ServiceTemplate template) {
    return this.baseRPC.join(this.sender.invokeMethod("delete", template)).fireSync();
  }

  @Override
  public boolean create(@NonNull ServiceTemplate template) {
    return this.baseRPC.join(this.sender.invokeMethod("create", template)).fireSync();
  }

  @Override
  public boolean has(@NonNull ServiceTemplate template) {
    return this.baseRPC.join(this.sender.invokeMethod("has", template)).fireSync();
  }

  @Override
  public @Nullable OutputStream appendOutputStream(
    @NonNull ServiceTemplate template,
    @NonNull String path
  ) throws IOException {
    return this.openLocalOutputStream(template, path, FileUtil.createTempFile(), true);
  }

  @Override
  public @Nullable OutputStream newOutputStream(
    @NonNull ServiceTemplate template,
    @NonNull String path
  ) throws IOException {
    return this.openLocalOutputStream(template, path, FileUtil.createTempFile(), false);
  }

  protected @NonNull OutputStream openLocalOutputStream(
    @NonNull ServiceTemplate template,
    @NonNull String path,
    @NonNull Path localPath,
    boolean append
  ) throws IOException {
    return new ListeningOutputStream<>(
      Files.newOutputStream(localPath),
      $ -> ChunkedPacketSender.forFileTransfer()
        .forFile(localPath)
        .transferChannel("deploy_single_file")
        .toChannels(CloudNetDriver.instance().networkClient().firstChannel())
        .withExtraData(
          DataBuf.empty().writeString(this.name).writeObject(template).writeString(path).writeBoolean(append))
        .build()
        .transferChunkedData()
        .get(5, TimeUnit.MINUTES, null));
  }

  @Override
  public boolean createFile(@NonNull ServiceTemplate template, @NonNull String path) {
    return this.baseRPC.join(this.sender.invokeMethod("createFile", template, path)).fireSync();
  }

  @Override
  public boolean createDirectory(@NonNull ServiceTemplate template, @NonNull String path) throws IOException {
    return this.baseRPC.join(this.sender.invokeMethod("createDirectory", template, path)).fireSync();
  }

  @Override
  public boolean hasFile(@NonNull ServiceTemplate template, @NonNull String path) {
    return this.baseRPC.join(this.sender.invokeMethod("hasFile", template, path)).fireSync();
  }

  @Override
  public boolean deleteFile(@NonNull ServiceTemplate template, @NonNull String path) {
    return this.baseRPC.join(this.sender.invokeMethod("deleteFile", template, path)).fireSync();
  }

  @Override
  public @Nullable InputStream newInputStream(
    @NonNull ServiceTemplate template,
    @NonNull String path
  ) throws IOException {
    // send a request for the file to the node
    var responseId = UUID.randomUUID();
    var response = ChannelMessage.builder()
      .message("remote_templates_template_file")
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .targetNode(CloudNetDriver.instance().nodeUniqueId())
      .buffer(DataBuf.empty().writeString(path).writeString(this.name).writeObject(template).writeUniqueId(responseId))
      .build()
      .sendSingleQuery();
    // check if we got a response
    if (response == null || !response.content().readBoolean()) {
      return null;
    }
    // the file is transferred and should be readable
    return Files.newInputStream(FileUtil.TEMP_DIR.resolve(responseId.toString()), StandardOpenOption.DELETE_ON_CLOSE);
  }

  @Override
  public @Nullable FileInfo fileInfo(@NonNull ServiceTemplate template, @NonNull String path) {
    return this.baseRPC.join(this.sender.invokeMethod("fileInfo", template, path)).fireSync();
  }

  @Override
  public @Nullable FileInfo[] listFiles(@NonNull ServiceTemplate template, @NonNull String dir, boolean deep) {
    return this.baseRPC.join(this.sender.invokeMethod("listFiles", template, dir, deep)).fireSync();
  }

  @Override
  public @NonNull Collection<ServiceTemplate> templates() {
    return this.baseRPC.join(this.sender.invokeMethod("templates")).fireSync();
  }

  @Override
  public void close() throws IOException {
    this.baseRPC.join(this.sender.invokeMethod("close")).fireSync();
  }
}
