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

package eu.cloudnetservice.driver.module.defaults;

import eu.cloudnetservice.driver.module.metadata.ModuleVersion;
import java.util.Comparator;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.semver4j.Semver;

/**
 * The default implementation of a module version.
 *
 * @since 4.0
 */
public final class DefaultModuleVersion implements ModuleVersion {

  private static final Comparator<ModuleVersion> MAJOR_MINOR_PATCH_COMPARATOR = Comparator
    .comparingInt(ModuleVersion::major)
    .thenComparing(ModuleVersion::minor)
    .thenComparing(ModuleVersion::patch);

  private final Semver semver;
  private final String build;
  private final String preRelease;

  /**
   * Constructs a new default module version instance.
   *
   * @param semver the parsed SemVer instance from the given input version.
   * @throws NullPointerException if the given SemVer instance is null.
   */
  private DefaultModuleVersion(@NonNull Semver semver) {
    this.semver = semver;
    this.build = String.join(".", semver.getBuild());
    this.preRelease = String.join(".", semver.getPreRelease());
  }

  /**
   * Parses a module version from the given SemVer input string. If the given input string is not a valid SemVer
   * version, this method returns null.
   *
   * @param input the input version that should be parsed.
   * @return the parsed module version from the given SemVer input.
   * @throws NullPointerException if the given input version is null.
   */
  public static @Nullable ModuleVersion parseSemVer(@NonNull String input) {
    // use coerce here to potentially fix invalid SemVer versions, for example
    // not providing a patch version would be counted as invalid on strict parsing
    var parsedSemver = Semver.coerce(input);
    return parsedSemver == null ? null : new DefaultModuleVersion(parsedSemver);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int major() {
    return this.semver.getMajor();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int minor() {
    return this.semver.getMinor();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int patch() {
    return this.semver.getPatch();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String build() {
    return this.build;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String preRelease() {
    return this.preRelease;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String displayString() {
    return this.semver.getVersion();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean satisfies(@NonNull String versionRange) {
    return this.semver.satisfies(versionRange);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(@NonNull ModuleVersion otherVersion) {
    // if the other version is also based on SemVer just compare those two
    if (otherVersion instanceof DefaultModuleVersion otherSemverVersion) {
      return this.semver.compareTo(otherSemverVersion.semver);
    }

    // in all other cases just fall back to comparing the major minor & patch information
    return MAJOR_MINOR_PATCH_COMPARATOR.compare(this, otherVersion);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String toString() {
    return "ModuleVersion[" + this.semver.getVersion() + "]";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }

    return other instanceof DefaultModuleVersion moduleVersion && this.semver.equals(moduleVersion.semver);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return this.semver.hashCode();
  }
}
