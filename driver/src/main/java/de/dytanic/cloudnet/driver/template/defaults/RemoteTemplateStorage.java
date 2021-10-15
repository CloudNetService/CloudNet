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

import de.dytanic.cloudnet.driver.network.rpc.RPC;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.FileInfo;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// todo: re-implement later
public class RemoteTemplateStorage implements TemplateStorage {

  private final String name;
  private final RPC baseRPC;

  public RemoteTemplateStorage(@NotNull String name, @NotNull RPC baseRPC) {
    this.name = name;
    this.baseRPC = baseRPC;
  }

  @Override
  public @NotNull String getName() {
    return this.name;
  }

  @Override
  public boolean deployDirectory(@NotNull Path directory,
    @NotNull ServiceTemplate target,
    @Nullable Predicate<Path> fileFilter) {
    return false;
  }

  @Override
  public boolean deploy(@NotNull InputStream inputStream,
    @NotNull ServiceTemplate target) {
    return false;
  }

  @Override
  public boolean copy(@NotNull ServiceTemplate template, @NotNull Path directory) {
    return false;
  }

  @Override
  public @Nullable InputStream zipTemplate(
    @NotNull ServiceTemplate template) throws IOException {
    return null;
  }

  @Override
  public boolean delete(@NotNull ServiceTemplate template) {
    return false;
  }

  @Override
  public boolean create(@NotNull ServiceTemplate template) {
    return false;
  }

  @Override
  public boolean has(@NotNull ServiceTemplate template) {
    return false;
  }

  @Override
  public @Nullable OutputStream appendOutputStream(
    @NotNull ServiceTemplate template, @NotNull String path) throws IOException {
    return null;
  }

  @Override
  public @Nullable OutputStream newOutputStream(
    @NotNull ServiceTemplate template, @NotNull String path) throws IOException {
    return null;
  }

  @Override
  public boolean createFile(
    @NotNull ServiceTemplate template, @NotNull String path) throws IOException {
    return false;
  }

  @Override
  public boolean createDirectory(
    @NotNull ServiceTemplate template, @NotNull String path) throws IOException {
    return false;
  }

  @Override
  public boolean hasFile(@NotNull ServiceTemplate template, @NotNull String path) throws IOException {
    return false;
  }

  @Override
  public boolean deleteFile(
    @NotNull ServiceTemplate template, @NotNull String path) throws IOException {
    return false;
  }

  @Override
  public @Nullable InputStream newInputStream(
    @NotNull ServiceTemplate template, @NotNull String path) throws IOException {
    return null;
  }

  @Override
  public @Nullable FileInfo getFileInfo(
    @NotNull ServiceTemplate template, @NotNull String path) throws IOException {
    return null;
  }

  @Override
  public @Nullable FileInfo[] listFiles(
    @NotNull ServiceTemplate template, @NotNull String dir, boolean deep) throws IOException {
    return new FileInfo[0];
  }

  @Override
  public @NotNull Collection<ServiceTemplate> getTemplates() {
    return null;
  }

  @Override
  public void close() throws IOException {

  }
}
