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

import lombok.NonNull;

/**
 * Represents a parsed module version which must be in SemVer format.
 *
 * @since 4.0
 */
public interface ModuleVersion extends Comparable<ModuleVersion> {

  /**
   * Get the major version.
   *
   * @return the major version.
   */
  int major();

  /**
   * Get the minor version.
   *
   * @return the minor version.
   */
  int minor();

  /**
   * Get the patch version.
   *
   * @return the patch version.
   */
  int patch();

  /**
   * Get the build version, can be empty if no build information is provided.
   *
   * @return the build version, can be empty if no build information is provided.
   */
  @NonNull
  String build();

  /**
   * Get the pre-release version, can be empty if no pre-release version information is provided.
   *
   * @return the pre-release version, can be empty if no pre-release version information is provided.
   */
  @NonNull
  String preRelease();

  /**
   * Get the fully assembled version string, which is a valid SemVer version.
   *
   * @return the fully assembled version string, which is a valid SemVer version.
   */
  @NonNull
  String displayString();

  /**
   * Checks if this version matches the given version range.
   *
   * @param versionRange the version range to check.
   * @return true if this version matches the given version range, false otherwise.
   * @throws NullPointerException if the given version range is null.
   */
  boolean satisfies(@NonNull String versionRange);
}
