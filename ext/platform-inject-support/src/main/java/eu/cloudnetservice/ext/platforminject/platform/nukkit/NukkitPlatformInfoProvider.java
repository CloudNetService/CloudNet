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

package eu.cloudnetservice.ext.platforminject.platform.nukkit;

import cn.nukkit.plugin.PluginBase;
import eu.cloudnetservice.ext.platforminject.data.PluginDataParser;
import eu.cloudnetservice.ext.platforminject.provider.BasePlatformInfoProvider;

public class NukkitPlatformInfoProvider extends BasePlatformInfoProvider<String, PluginBase> {

  public NukkitPlatformInfoProvider() {
    super(
      "nukkit",
      NukkitPlatformPluginManager::new,
      NukkitPluginInfoGenerator::new,
      NukkitMainClassGenerator::new,
      () -> PluginDataParser.create()
        .enableSupport(PluginDataParser.PLUGIN_COMMANDS)
        .enableSupport(PluginDataParser.PLUGIN_DEPENDENCIES));
  }
}
