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

package eu.cloudnetservice.ext.platforminject.loader;

import eu.cloudnetservice.utils.base.io.FileUtil;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import lombok.NonNull;

final class JarInJarClassLoader extends URLClassLoader {

  static {
    ClassLoader.registerAsParallelCapable();
  }

  public JarInJarClassLoader(@NonNull String name, @NonNull URL internalJarUrl, @NonNull ClassLoader parent) {
    super(name, extractJar(internalJarUrl), parent);
  }

  private static @NonNull URL[] extractJar(@NonNull URL internalJarUrl) {
    // create the target file path & copy the resource to the target path
    var target = Path.of(".inject", "inject-support.jar");
    try (var stream = internalJarUrl.openStream()) {
      FileUtil.copy(stream, target);
      return new URL[]{target.toUri().toURL()};
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to extract jar in jar for inject support", exception);
    }
  }
}
