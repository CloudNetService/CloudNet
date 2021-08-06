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

package de.dytanic.cloudnet.driver.module;

import com.google.common.collect.ObjectArrays;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.jetbrains.annotations.NotNull;

public final class FinalizeURLClassLoader extends URLClassLoader {

  private static final Set<FinalizeURLClassLoader> LOADERS = new CopyOnWriteArraySet<>();

  static {
    ClassLoader.registerAsParallelCapable();
  }

  public FinalizeURLClassLoader(@NotNull URL moduleFileUrl, @NotNull Set<URL> moduleDependencyUrls) {
    super(ObjectArrays.concat(moduleFileUrl, moduleDependencyUrls.toArray(new URL[0])));
  }

  public void registerGlobally() {
    LOADERS.add(this);
  }

  @Override
  public void close() throws IOException {
    super.close();
    LOADERS.remove(this);
  }

  @Override
  protected @NotNull Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    try {
      return super.loadClass(name, resolve);
    } catch (ClassNotFoundException ignored) {
      // ignore for now, we'll try the other loaders first
    }

    for (FinalizeURLClassLoader loader : LOADERS) {
      if (loader != this) {
        try {
          return loader.loadClass(name, resolve);
        } catch (ClassNotFoundException exception) {
          // there may be still other to come
        }
      }
    }

    // nothing found
    throw new ClassNotFoundException(name);
  }
}
