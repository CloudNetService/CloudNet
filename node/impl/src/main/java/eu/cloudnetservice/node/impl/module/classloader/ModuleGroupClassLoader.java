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

import eu.cloudnetservice.node.module.metadata.ModuleMetadata;
import java.security.SecureClassLoader;
import lombok.NonNull;

/**
 * Classloader that can load classes / libraries of other modules using a class loader group. If a module depends on
 * another module (even transitively), the classes and libraries of that module are visible to the calling module.
 * Classes of other modules not depended on will not be exposed by this class loader.
 *
 * @since 4.0
 */
public final class ModuleGroupClassLoader extends SecureClassLoader {

  /**
   * Scope that class load calls are being executed in. This is done to prevent calling the class loader group again
   * while already loading a class, as it's unnecessary because all transitive dependencies will be checked anyway.
   */
  private static final ScopedValue<Void> LOAD_CLASS_SCOPE = ScopedValue.newInstance();

  static {
    ClassLoader.registerAsParallelCapable();
  }

  private final ModuleMetadata moduleMetadata;
  private final ModuleClassLoaderGroup classLoaderGroup;

  /**
   * Constructs a new module group class loader instance.
   *
   * @param parent           the parent class loader to use.
   * @param moduleMetadata   the metadata of the module this class loader is for.
   * @param classLoaderGroup the module group that is associated with this class loader.
   * @throws NullPointerException if one of the given parameters is null.
   */
  public ModuleGroupClassLoader(
    @NonNull ClassLoader parent,
    @NonNull ModuleMetadata moduleMetadata,
    @NonNull ModuleClassLoaderGroup classLoaderGroup
  ) {
    super("module-group-cl:" + moduleMetadata.id(), parent);
    this.moduleMetadata = moduleMetadata;
    this.classLoaderGroup = classLoaderGroup;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected @NonNull Class<?> loadClass(@NonNull String name, boolean resolve) throws ClassNotFoundException {
    if (LOAD_CLASS_SCOPE.isBound()) {
      throw new ClassNotFoundException(name);
    }

    var loadClassCarrier = ScopedValue.where(LOAD_CLASS_SCOPE, null);
    return loadClassCarrier.call(() -> super.loadClass(name, resolve));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected @NonNull Class<?> findClass(@NonNull String name) throws ClassNotFoundException {
    return this.classLoaderGroup.loadClass(name, this.moduleMetadata);
  }
}
