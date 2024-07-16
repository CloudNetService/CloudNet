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

package eu.cloudnetservice.driver.module.metadata;

import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Represents an artifact of the module that can either be compiled into the module file or located on the filesystem.
 * These artifacts will be copied from the source to the target path on the matching environments.
 *
 * @since 4.0
 */
public interface ModuleArtifact {

  /**
   * Get the loading source of the artifact.
   *
   * @return the loading source of the artifact.
   */
  @NonNull
  Source source();

  /**
   * Get the environments on which this artifact copy action should be enabled. Note that there is a special environment
   * called {@code node-wrapper} which indicates that the node should provide this artifact to the wrapper.
   *
   * @return the environments on which this artifact copy action should be enabled.
   */
  @NonNull
  @Unmodifiable
  Collection<String> environments();

  /**
   * Get the source path from where this artifact should be loaded. It can be an absolute path or relative to the
   * current running environment. An empty source string indicates that the file which contains the module is the source
   * path. Note that this setting only supports file copies, directories cannot be copied with this.
   *
   * @return the source path from where this artifact should be loaded.
   */
  @NonNull
  String sourcePath();

  /**
   * Get the target path to which this artifact should be copied. If the file in the destination already exists it will
   * be replaced. This can either be an absolute path or relative to the current running environment.
   *
   * @return the target path to which this artifact should be copied.
   */
  @NonNull
  String targetPath();

  /**
   * The available artifact sources.
   *
   * @since 4.0
   */
  enum Source {
    /**
     * The artifact should be loaded from the module classpath.
     */
    CLASSPATH,
    /**
     * The artifact should be loaded from the current default filesystem.
     */
    FILESYSTEM,
  }
}
