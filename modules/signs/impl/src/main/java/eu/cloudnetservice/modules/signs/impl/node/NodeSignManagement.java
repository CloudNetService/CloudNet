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

package eu.cloudnetservice.modules.signs.impl.node;

import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.database.Database;
import eu.cloudnetservice.driver.database.DatabaseProvider;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.SignManagement;
import eu.cloudnetservice.modules.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.modules.signs.impl.AbstractSignManagement;
import eu.cloudnetservice.modules.signs.impl.InternalSignManagement;
import eu.cloudnetservice.modules.signs.impl.node.configuration.NodeSignsConfigurationHelper;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@Provides({AbstractSignManagement.class, InternalSignManagement.class, SignManagement.class})
public class NodeSignManagement extends AbstractSignManagement implements SignManagement {

  protected static final String NODE_TO_NODE_SET_SIGN_CONFIGURATION = "signs_node_node_set_signs_config";

  protected final Path configPath;
  protected final Database database;

  @Inject
  public NodeSignManagement(
    @NonNull SignsConfiguration configuration,
    @NonNull @Named("dataDirectory") Path dataDirectory,
    @NonNull DatabaseProvider databaseProvider
  ) {
    super(configuration);

    this.configPath = dataDirectory.resolve("config.json");
    this.database = databaseProvider.database(CloudNetSignsModule.DATABASE_NAME);

    this.database.documentsAsync().thenAccept(jsonDocuments -> {
      for (var document : jsonDocuments) {
        var sign = document.toInstanceOf(Sign.class);
        this.signs.put(sign.location(), sign);
      }
    });
  }

  @Override
  public void createSign(@NonNull Sign sign) {
    this.database.insert(this.documentKey(sign.location()), Document.newJsonDocument().appendTree(sign));
    this.signs.put(sign.location(), sign);

    this.channelMessage(SIGN_CREATED)
      .buffer(DataBuf.empty().writeObject(sign))
      .targetAll()
      .build().send();
  }

  @Override
  public void deleteSign(@NonNull WorldPosition position) {
    this.database.delete(this.documentKey(position));
    this.signs.remove(position);

    this.channelMessage(SIGN_DELETED)
      .buffer(DataBuf.empty().writeObject(position))
      .targetAll()
      .build().send();
  }

  @Override
  public int deleteAllSigns(@NonNull String group, @Nullable String templatePath) {
    var positions = this.signs.entrySet().stream()
      .filter(entry -> entry.getValue().targetGroup().equals(group)
        && (templatePath == null || templatePath.equals(entry.getValue().templatePath())))
      .map(Map.Entry::getKey)
      .collect(Collectors.toSet());

    for (var position : positions) {
      this.database.delete(this.documentKey(position));
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
    for (var position : positions) {
      this.database.delete(this.documentKey(position));
      this.signs.remove(position);
    }

    this.channelMessage(SIGN_BULK_DELETE)
      .buffer(DataBuf.empty().writeObject(positions))
      .targetAll()
      .build().send();
    return positions.size();
  }

  @Override
  public @NonNull Collection<Sign> signs(@NonNull Collection<String> groups) {
    return this.signs.values().stream()
      .filter(sign -> groups.contains(sign.location().group()))
      .collect(Collectors.toList());
  }

  @Override
  public void signsConfiguration(@NonNull SignsConfiguration signsConfiguration) {
    super.signsConfiguration(signsConfiguration);

    this.channelMessage(SIGN_CONFIGURATION_UPDATE)
      .buffer(DataBuf.empty().writeObject(signsConfiguration))
      .targetAll()
      .build().send();
    NodeSignsConfigurationHelper.write(signsConfiguration, this.configPath);
  }

  @Override
  public void handleInternalSignConfigUpdate(@NonNull SignsConfiguration configuration) {
    super.handleInternalSignConfigUpdate(configuration);
    NodeSignsConfigurationHelper.write(configuration, this.configPath);
  }

  protected String documentKey(@NonNull WorldPosition position) {
    return position.world()
      + '.' + position.group()
      + '.' + position.x()
      + '.' + position.y()
      + '.' + position.z();
  }
}
