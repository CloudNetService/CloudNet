/*
 * Copyright 2019-2025 CloudNetService team & contributors
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

package eu.cloudnetservice.node.module.metadata;

import java.nio.file.Path;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A parser for a module metadata. Different parsers can, for example, read different metadata file formats.
 *
 * @since 4.0
 */
@FunctionalInterface
public interface ModuleMetadataParser {

  /**
   * Parses the module metadata from the given path. If the resource does not contain a metadata or is rejected, the
   * method can return null. The null return type is used to not interrupt the module loading while throwing an
   * exception will cause the module loading process to be interrupted.
   *
   * @param modulePath the path to the module file of which the module metadata should be parsed.
   * @return the parsed module metadata, can be null if the given path does not contain a valid metadata file.
   * @throws NullPointerException if the given module path is null.
   * @throws Exception            if any exception occurs during the metadata parsing.
   */
  @Nullable
  ModuleMetadata parseModuleMetadata(@NonNull Path modulePath) throws Exception;
}
