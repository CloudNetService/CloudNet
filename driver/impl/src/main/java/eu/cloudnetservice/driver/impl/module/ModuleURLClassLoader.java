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

package eu.cloudnetservice.driver.impl.module;

import com.google.common.collect.ObjectArrays;
import dev.derklaro.aerogel.SpecifiedInjector;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.inject.InjectionLayerHolder;
import eu.cloudnetservice.driver.module.Module;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.NonNull;

/**
 * This module url class loader is a unique url class loader for each module. Loading a class using this ClassLoader
 * ensures that all other modules have access to the loaded class too.
 *
 * @see Module
 * @since 4.0
 */
public class ModuleURLClassLoader extends URLClassLoader implements InjectionLayerHolder<SpecifiedInjector> {

  /**
   * All loaders which were created and of which the associated module is still loaded.
   */
  protected static final Lock CLASS_LOADING_LOCK = new ReentrantLock();
  protected static final Set<ModuleURLClassLoader> LOADERS = new HashSet<>();

  static {
    ClassLoader.registerAsParallelCapable();
  }

  protected final InjectionLayer<SpecifiedInjector> injectionLayer;

  /**
   * Creates an instance of this FinalizeURLClassLoader.
   *
   * @param moduleFileUrl        the module file to which this loader is associated.
   * @param moduleDependencyUrls all dependencies which were loaded for the module.
   * @param injectionLayer       the injection layer for the module associated with this loader.
   * @throws NullPointerException if moduleFileUrl or moduleDependencyUrls is null.
   */
  public ModuleURLClassLoader(
    @NonNull URL moduleFileUrl,
    @NonNull Set<URL> moduleDependencyUrls,
    @NonNull InjectionLayer<SpecifiedInjector> injectionLayer
  ) {
    super(
      ObjectArrays.concat(moduleFileUrl, moduleDependencyUrls.toArray(new URL[0])),
      ModuleURLClassLoader.class.getClassLoader());
    this.injectionLayer = injectionLayer;
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

      // remove all injection stuff created by the associated module layer, then close the loader itself
      this.injectionLayer.close();
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
   * {@inheritDoc}
   */
  @Override
  public @NonNull InjectionLayer<SpecifiedInjector> injectionLayer() {
    return this.injectionLayer;
  }

  /**
   * Tries to load a class by the provided name.
   *
   * @param name    the name of the class to load.
   * @param resolve if the class should be resolved.
   * @param global  if all loaders registered in {@link ModuleURLClassLoader#LOADERS} should be checked.
   * @return The resulting Class object
   * @throws ClassNotFoundException if the class could not be found
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
