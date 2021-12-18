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

package eu.cloudnetservice.cloudnet.modlauncher;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.TransformingClassLoader;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import lombok.NonNull;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

/**
 * Hacky service class to hook into the ModLauncher sponge is using to prevent double loading of CloudNet internal
 * classes.
 */
public final class CloudNetLaunchPluginService implements ILaunchPluginService {

  private static final Collection<String> EXCLUDED_PACKAGE_STARTS = Arrays.asList(
    "de.dytanic.cloudnet.wrapper.",
    "de.dytanic.cloudnet.common.",
    "de.dytanic.cloudnet.driver.");

  @Override
  public String name() {
    return "wrapper-tweaker";
  }

  @Override
  public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
    return EnumSet.noneOf(Phase.class);
  }

  @Override
  public boolean processClass(final Phase phase, ClassNode classNode, final Type classType) {
    return false;
  }

  @Override
  public void initializeLaunch(ITransformerLoader transformerLoader, Path[] specialPaths) {
    // at this point the transforming class loader should be available - get it
    this.transformingLoader().addTargetPackageFilter(
      pkg -> EXCLUDED_PACKAGE_STARTS.stream().noneMatch(pkg::startsWith));
  }

  private @NonNull TransformingClassLoader transformingLoader() {
    try {
      var field = Launcher.class.getDeclaredField("classLoader");
      field.setAccessible(true);
      return (TransformingClassLoader) field.get(Launcher.INSTANCE);
    } catch (NoSuchFieldException | IllegalAccessException exception) {
      throw new IllegalStateException("Unable to retrieve transforming class loader", exception);
    }
  }
}
