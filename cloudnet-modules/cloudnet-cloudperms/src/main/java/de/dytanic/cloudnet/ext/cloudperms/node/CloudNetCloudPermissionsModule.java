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

package de.dytanic.cloudnet.ext.cloudperms.node;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.ext.cloudperms.node.listener.ConfigurationUpdateListener;
import de.dytanic.cloudnet.ext.cloudperms.node.listener.IncludePluginListener;
import de.dytanic.cloudnet.module.NodeCloudNetModule;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class CloudNetCloudPermissionsModule extends NodeCloudNetModule {

  private static final Type LIST_STRING = new TypeToken<List<String>>() {
  }.getType();

  private static CloudNetCloudPermissionsModule instance;

  public static CloudNetCloudPermissionsModule getInstance() {
    return CloudNetCloudPermissionsModule.instance;
  }

  @ModuleTask(order = 127, event = ModuleLifeCycle.LOADED)
  public void init() {
    instance = this;
  }

  @ModuleTask(order = 126, event = ModuleLifeCycle.STARTED)
  public void initConfig() {
    this.getConfig().getBoolean("enabled", true);
    this.getConfig().get("excludedGroups", LIST_STRING, new ArrayList<>());
    this.saveConfig();
  }

  @ModuleTask(order = 124, event = ModuleLifeCycle.STARTED)
  public void registerListeners() {
    this.registerListeners(new IncludePluginListener(), new ConfigurationUpdateListener());
  }

  public List<String> getExcludedGroups() {
    return this.getConfig().get("excludedGroups", LIST_STRING);
  }
}
