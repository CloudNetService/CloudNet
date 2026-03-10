/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.module.classloader;

import eu.cloudnetservice.node.module.ModuleDependencyTree;
import eu.cloudnetservice.node.module.metadata.ModuleMetadata;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.NonNull;

/**
 * A collection of module class loaders. This group can be used to load a class by name from any class loader a module
 * is transitively depending on. This allows access to classes provided by other modules (including their libraries),
 * while isolating from accidentally loading classes from unexpected module sources. All methods in this implementation
 * can be accessed concurrently.
 *
 * @since 4.0
 */
public final class ModuleClassLoaderGroup {

  private final ModuleDependencyTree dependencyTree;

  private final ReadWriteLock classLoadersLock;
  private final List<ModuleClassLoader> allModuleClassLoaders;

  /**
   * Constructs a new module class loader group.
   *
   * @param dependencyTree the tree to use for checking module dependency on each other.
   * @throws NullPointerException if the given dependency tree is null.
   */
  public ModuleClassLoaderGroup(@NonNull ModuleDependencyTree dependencyTree) {
    this.dependencyTree = dependencyTree;
    this.allModuleClassLoaders = new ArrayList<>();
    this.classLoadersLock = new ReentrantReadWriteLock(true);
  }

  /**
   * Registers the given class loader to this class loader group. This method does nothing if the given class loader is
   * already registered in this group.
   *
   * @param loader the class loader to register.
   * @throws NullPointerException if the given class loader is null.
   */
  public void registerClassLoader(@NonNull ModuleClassLoader loader) {
    var lock = this.classLoadersLock.writeLock();
    lock.lock();
    try {
      if (!this.allModuleClassLoaders.contains(loader)) {
        this.allModuleClassLoaders.add(loader);
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Unregisters the given class loader from this class loader group.
   *
   * @param loader the class loader to unregister.
   * @throws NullPointerException if the given class loader is null.
   */
  public void unregisterClassLoader(@NonNull ModuleClassLoader loader) {
    var lock = this.classLoadersLock.writeLock();
    lock.lock();
    try {
      this.allModuleClassLoaders.remove(loader);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Tries to load a class with the given name from any of the registered module class loaders. A module class loader is
   * only checked if the module (identified by the given module metadata) directly or transitively depends on the module
   * being checked. If the class does not exist or if the module does not depend on the module providing the class, this
   * method throws an {@link ClassNotFoundException}.
   *
   * @param name             the name of the class to find in any of the registered class loaders.
   * @param requestingModule the metadata of the module requesting the class, used for dependency checking.
   * @return the class loaded from a registered class loader of a module the requesting module depends on.
   * @throws NullPointerException   if the given name or requesting module metadata is null.
   * @throws ClassNotFoundException if the class does not exist or is not depended upon by the requesting module.
   */
  @NonNull
  Class<?> loadClass(@NonNull String name, @NonNull ModuleMetadata requestingModule) throws ClassNotFoundException {
    var lock = this.classLoadersLock.readLock();
    lock.lock();
    try {
      for (var moduleClassLoader : this.allModuleClassLoaders) {
        var moduleMeta = moduleClassLoader.moduleMetadata();
        if (moduleMeta != requestingModule && this.dependencyTree.transitiveDependingOn(requestingModule, moduleMeta)) {
          try {
            return moduleClassLoader.loadClass(name);
          } catch (ClassNotFoundException _) {
          }
        }
      }
    } finally {
      lock.unlock();
    }

    throw new ClassNotFoundException(name);
  }
}
