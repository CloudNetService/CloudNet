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
import java.io.IOError;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import lombok.NonNull;

/**
 * Class loader used for resolving library classes requested by modules. Each module has a unique library loader in the
 * class loader hierarchy. Libraries can be added dynamically to this class loader using the
 * {@link #registerLibrary(Path)} method.
 *
 * @since 4.0
 */
public final class ModuleLibraryClassLoader extends URLClassLoader {

  static {
    ClassLoader.registerAsParallelCapable();
  }

  /**
   * Constructs a new module library class loader instance.
   *
   * @param moduleMetadata the metadata of the module this loader is constructed for.
   * @param parent         the parent class loader of this library loader.
   * @throws NullPointerException if the given module metadata or parent class loader is null.
   */
  public ModuleLibraryClassLoader(@NonNull ModuleMetadata moduleMetadata, @NonNull ClassLoader parent) {
    super("module-library-cl:" + moduleMetadata.id(), new URL[0], parent);
  }

  /**
   * Adds the given library file or directory to the lookup of this class loader. This method does nothing if the given
   * path was already added to this loader.
   *
   * @param libraryPath the path to library file or directory to add.
   * @throws NullPointerException     if the given library path is null.
   * @throws IllegalArgumentException if an i/o error occurs while converting the given library path.
   */
  public void registerLibrary(@NonNull Path libraryPath) {
    try {
      super.addURL(libraryPath.toUri().toURL());
    } catch (IOError | IOException exception) {
      throw new IllegalArgumentException("Failed to convert library path " + libraryPath + " to url", exception);
    }
  }
}
