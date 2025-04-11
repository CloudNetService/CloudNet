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

package eu.cloudnetservice.node.impl.module.metadata;

import eu.cloudnetservice.node.module.metadata.ModuleVersion;
import java.util.Comparator;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The fallback implementation of a module version.
 *
 * @since 4.0
 */
public final class UnknownModuleVersion implements ModuleVersion {

  private static final Comparator<ModuleVersion> MAJOR_MINOR_PATCH_COMPARATOR = Comparator
    .comparingInt(ModuleVersion::major)
    .thenComparing(ModuleVersion::minor)
    .thenComparing(ModuleVersion::patch);

  private final int major;
  private final int minor;
  private final int patch;

  private final String originalVersion;

  /**
   * Constructs a fallback module version.
   *
   * @param major           the major version to use.
   * @param minor           the minor version to use.
   * @param patch           the patch version to use.
   * @param originalVersion the original version string that was parsed.
   * @throws NullPointerException if the original version string is null.
   */
  private UnknownModuleVersion(int major, int minor, int patch, @NonNull String originalVersion) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
    this.originalVersion = originalVersion;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int major() {
    return this.major;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int minor() {
    return this.minor;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int patch() {
    return this.patch;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String build() {
    return "";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String preRelease() {
    return "";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String displayString() {
    return this.originalVersion;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean satisfies(@NonNull String versionRange) {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(@NotNull ModuleVersion o) {
    return MAJOR_MINOR_PATCH_COMPARATOR.compare(this, o);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String toString() {
    return "ModuleVersion[" + this.originalVersion + "]";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }

    return other instanceof UnknownModuleVersion moduleVersion
      && this.originalVersion.equals(moduleVersion.originalVersion);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return this.originalVersion.hashCode();
  }
}
