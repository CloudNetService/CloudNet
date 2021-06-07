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

package de.dytanic.cloudnet.ext.signs.node;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.database.Database;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.ext.signs.Sign;
import de.dytanic.cloudnet.ext.signs.SignConstants;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfiguration;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfigurationReaderAndWriter;
import de.dytanic.cloudnet.ext.signs.node.command.CommandSigns;
import de.dytanic.cloudnet.ext.signs.node.http.V1SignConfigurationHttpHandler;
import de.dytanic.cloudnet.ext.signs.node.listener.CloudNetSignsModuleListener;
import de.dytanic.cloudnet.ext.signs.node.listener.IncludePluginListener;
import de.dytanic.cloudnet.ext.signs.node.listener.SignsTaskSetupListener;
import de.dytanic.cloudnet.module.NodeCloudNetModule;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

public final class CloudNetSignsModule extends NodeCloudNetModule {

  private static final String SIGN_STORE_DOCUMENT = "signs_store";

  private static CloudNetSignsModule instance;
  private SignConfiguration signConfiguration;
  private Path configurationFile;

  public CloudNetSignsModule() {
    instance = this;
  }

  public static CloudNetSignsModule getInstance() {
    return CloudNetSignsModule.instance;
  }

  @ModuleTask(order = 127, event = ModuleLifeCycle.STARTED)
  public void createConfigurationOrUpdate() {
    this.configurationFile = this.getModuleWrapper().getDataDirectory().resolve("config.json");
    this.signConfiguration = SignConfigurationReaderAndWriter.read(this.configurationFile);
  }

  @ModuleTask(order = 125, event = ModuleLifeCycle.STARTED)
  public void registerListeners() {
    this
      .registerListeners(new IncludePluginListener(), new CloudNetSignsModuleListener(), new SignsTaskSetupListener());
  }

  @ModuleTask(order = 124, event = ModuleLifeCycle.STARTED)
  public void registerCommands() {
    this.registerCommand(new CommandSigns());
  }

  @ModuleTask(order = 123, event = ModuleLifeCycle.STARTED)
  public void registerHttpHandlers() {
    this.getHttpServer().registerHandler("/api/v1/modules/signs/config",
      new V1SignConfigurationHttpHandler("cloudnet.http.v1.modules.signs.config"));
  }

  public void addSignToFile(Sign sign) {
    Preconditions.checkNotNull(sign);

    Collection<Sign> signs = this.loadSigns();
    signs.add(sign);

    this.write(signs);
  }

  public void removeSignToFile(Sign sign) {
    Preconditions.checkNotNull(sign);

    Collection<Sign> signs = this.loadSigns();

    signs.stream()
      .filter(s -> sign.getSignId() == s.getSignId())
      .findFirst().ifPresent(signs::remove);

    this.write(signs);
  }

  public Collection<Sign> loadSigns() {
    Database database = this.getDatabaseProvider().getDatabase(DefaultModuleHelper.DEFAULT_CONFIGURATION_DATABASE_NAME);
    JsonDocument document = database.get(SIGN_STORE_DOCUMENT);

    return document != null ? document.get("signs", SignConstants.COLLECTION_SIGNS, new ArrayList<>())
      : new ArrayList<>();
  }

  public void write(Collection<Sign> signs) {
    Preconditions.checkNotNull(signs);

    Database database = this.getDatabaseProvider().getDatabase(DefaultModuleHelper.DEFAULT_CONFIGURATION_DATABASE_NAME);
    JsonDocument document = database.get(SIGN_STORE_DOCUMENT);

    if (document == null) {
      document = new JsonDocument();
    }

    database.update(SIGN_STORE_DOCUMENT, document.append("signs", signs));
  }

  public SignConfiguration getSignConfiguration() {
    return this.signConfiguration;
  }

  public void setSignConfiguration(SignConfiguration signConfiguration) {
    this.signConfiguration = signConfiguration;
  }

  @Deprecated
  public File getConfigurationFile() {
    return this.configurationFile.toFile();
  }

  public Path getConfigurationFilePath() {
    return this.configurationFile;
  }
}
