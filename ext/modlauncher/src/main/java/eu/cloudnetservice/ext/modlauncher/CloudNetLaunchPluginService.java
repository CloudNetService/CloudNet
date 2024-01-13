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

package eu.cloudnetservice.ext.modlauncher;

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
    "eu.cloudnetservice.wrapper.",
    "eu.cloudnetservice.common.",
    "eu.cloudnetservice.driver.");

  @Override
  public @NonNull String name() {
    return "wrapper-tweaker";
  }

  @Override
  public @NonNull EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
    return EnumSet.noneOf(Phase.class);
  }

  @Override
  public boolean processClass(final Phase phase, ClassNode classNode, final Type classType) {
    return false;
  }

  @Override
  public void initializeLaunch(ITransformerLoader transformerLoader, Path[] specialPaths) {
    // at this point the transforming class loader should be the context class loader
    // we fail here if it's not there as this will cause problems later on when classes were loaded twice
    if (Thread.currentThread().getContextClassLoader() instanceof TransformingClassLoader loader) {
      loader.addTargetPackageFilter(pkg -> EXCLUDED_PACKAGE_STARTS.stream().noneMatch(pkg::startsWith));
    } else {
      throw new IllegalStateException("Thread context class loader should be an instance of TransformingClassLoader");
    }
  }
}
