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

package eu.cloudnetservice.cloudnet.ext.signs.node;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.database.Database;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import eu.cloudnetservice.cloudnet.ext.signs.AbstractSignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import eu.cloudnetservice.cloudnet.ext.signs.SignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.cloudnet.ext.signs.node.configuration.NodeSignsConfigurationHelper;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NodeSignManagement extends AbstractSignManagement implements SignManagement {

  protected static final String NODE_TO_NODE_SET_SIGN_CONFIGURATION = "signs_node_node_set_signs_config";

  protected final Database database;
  protected final Path configurationFilePath;

  public NodeSignManagement(SignsConfiguration configuration, Path configurationFilePath, Database database) {
    super(configuration);

    this.configurationFilePath = configurationFilePath;
    this.database = database;

    this.database.documentsAsync().onComplete(jsonDocuments -> {
      for (JsonDocument document : jsonDocuments) {
        Sign sign = document.toInstanceOf(Sign.class);
        this.signs.put(sign.getWorldPosition(), sign);
      }
    });
  }

  @Override
  public void createSign(@NotNull Sign sign) {
    this.database.insert(this.getDocumentKey(sign.getWorldPosition()), JsonDocument.newDocument(sign));
    this.signs.put(sign.getWorldPosition(), sign);

    this.channelMessage(SIGN_CREATED)
      .buffer(DataBuf.empty().writeObject(sign))
      .targetAll()
      .build().send();
  }

  @Override
  public void deleteSign(@NotNull WorldPosition position) {
    this.database.delete(this.getDocumentKey(position));
    this.signs.remove(position);

    this.channelMessage(SIGN_DELETED)
      .buffer(DataBuf.empty().writeObject(position))
      .targetAll()
      .build().send();
  }

  @Override
  public int deleteAllSigns(@NotNull String group, @Nullable String templatePath) {
    Set<WorldPosition> positions = this.signs.entrySet().stream()
      .filter(entry -> entry.getValue().getTargetGroup().equals(group)
        && (templatePath == null || templatePath.equals(entry.getValue().getTemplatePath())))
      .map(Map.Entry::getKey)
      .collect(Collectors.toSet());

    for (WorldPosition position : positions) {
      this.database.delete(this.getDocumentKey(position));
      this.signs.remove(position);
    }

    this.channelMessage(SIGN_BULK_DELETE)
      .buffer(DataBuf.empty().writeObject(positions))
      .targetAll()
      .build().send();
    return positions.size();
  }

  @Override
  public int deleteAllSigns() {
    Set<WorldPosition> positions = new HashSet<>(this.signs.keySet());
    for (WorldPosition position : positions) {
      this.database.delete(this.getDocumentKey(position));
      this.signs.remove(position);
    }

    this.channelMessage(SIGN_BULK_DELETE)
      .buffer(DataBuf.empty().writeObject(positions))
      .targetAll()
      .build().send();
    return positions.size();
  }

  @Override
  public @NotNull Collection<Sign> getSigns(@NotNull String[] groups) {
    List<String> allGroups = Arrays.asList(groups);
    return this.signs.values().stream()
      .filter(sign -> allGroups.contains(sign.getCreatedGroup()))
      .collect(Collectors.toList());
  }

  @Override
  public void setSignsConfiguration(@NotNull SignsConfiguration signsConfiguration) {
    super.setSignsConfiguration(signsConfiguration);

    this.channelMessage(SIGN_CONFIGURATION_UPDATE)
      .buffer(DataBuf.empty().writeObject(signsConfiguration))
      .targetAll()
      .build().send();
    NodeSignsConfigurationHelper.write(signsConfiguration, this.configurationFilePath);
  }

  @Override
  public void handleInternalSignConfigUpdate(@NotNull SignsConfiguration configuration) {
    super.handleInternalSignConfigUpdate(configuration);
    NodeSignsConfigurationHelper.write(configuration, this.configurationFilePath);
  }

  protected String getDocumentKey(@NotNull WorldPosition position) {
    return position.getWorld() + '.' + position.getGroup() + '.' + position.getX()
      + '.' + position.getY() + '.' + position.getZ();
  }
}
