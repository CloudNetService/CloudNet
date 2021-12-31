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

package de.dytanic.cloudnet.driver.module;

import com.google.common.collect.ObjectArrays;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.NonNull;

public class FinalizeURLClassLoader extends URLClassLoader {

  /**
   * All loaders which were created and of which the associated module is still loaded.
   */
  protected static final Lock CLASS_LOADING_LOCK = new ReentrantLock();
  protected static final Set<FinalizeURLClassLoader> LOADERS = new HashSet<>();

  static {
    ClassLoader.registerAsParallelCapable();
  }

  /**
   * Creates an instance of this class loader.
   *
   * @param moduleFileUrl        the module file to which this loader is associated.
   * @param moduleDependencyUrls all dependencies which were loaded for the module.
   */
  public FinalizeURLClassLoader(@NonNull URL moduleFileUrl, @NonNull Set<URL> moduleDependencyUrls) {
    super(
      ObjectArrays.concat(moduleFileUrl, moduleDependencyUrls.toArray(new URL[0])),
      FinalizeURLClassLoader.class.getClassLoader());
  }

  /**
   * Registers this loader to all loaders. This causes all other instances of this class to search for classes in this
   * loader too.
   */
  public void registerGlobally() {
    try {
      CLASS_LOADING_LOCK.lock();
      LOADERS.add(this);
    } finally {
      CLASS_LOADING_LOCK.unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws IOException {
    try {
      CLASS_LOADING_LOCK.lock();
      // remove first to prevent other trying to load classes while closed
      LOADERS.remove(this);
      super.close();
    } finally {
      CLASS_LOADING_LOCK.unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected @NonNull Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    return this.loadClass(name, resolve, true);
  }

  /**
   * Tries to load a class by the provided name.
   *
   * @param name    The name of the class to load.
   * @param resolve If the class should be resolved.
   * @param global  If all loaders registered in {@link FinalizeURLClassLoader#LOADERS} should be checked.
   * @return The resulting {@code Class} object
   * @throws ClassNotFoundException If the class could not be found
   */
  protected @NonNull Class<?> loadClass(String name, boolean resolve, boolean global) throws ClassNotFoundException {
    try {
      return super.loadClass(name, resolve);
    } catch (ClassNotFoundException ignored) {
      // ignore for now, we'll try the other loaders first
    }

    if (global) {
      CLASS_LOADING_LOCK.lock();
      try {
        for (var loader : LOADERS) {
          if (loader != this) {
            try {
              return loader.loadClass(name, resolve, false);
            } catch (ClassNotFoundException exception) {
              // there may be still other to come
            }
          }
        }
      } finally {
        CLASS_LOADING_LOCK.unlock();
      }
    }

    // nothing found
    throw new ClassNotFoundException(name);
  }
}
