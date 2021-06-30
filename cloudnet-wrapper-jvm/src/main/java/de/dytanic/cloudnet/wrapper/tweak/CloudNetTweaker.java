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

package de.dytanic.cloudnet.wrapper.tweak;

import java.io.File;
import java.util.List;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * Tweaker for the Minecraft LaunchWrapper which prevents doubled loading of CloudNet classes
 */
public class CloudNetTweaker implements ITweaker {

  @Override
  public void acceptOptions(List<String> args, File gameDir, final File assetsDir, String profile) {
  }

  @Override
  public void injectIntoClassLoader(LaunchClassLoader launchClassLoader) {
    launchClassLoader.addClassLoaderExclusion("de.dytanic.cloudnet.wrapper.");
    launchClassLoader.addClassLoaderExclusion("de.dytanic.cloudnet.common.");
    launchClassLoader.addClassLoaderExclusion("de.dytanic.cloudnet.driver.");
  }

  @Override
  public String getLaunchTarget() {
    return null;
  }

  @Override
  public String[] getLaunchArguments() {
    return new String[0];
  }

}
