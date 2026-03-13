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

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.primitives.Ints;
import eu.cloudnetservice.node.module.metadata.ModuleVersion;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
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

  private static final Pattern NON_SEPERATOR_NUMBER_PATTERN = Pattern.compile("[^0-9_,-]");
  private static final CharMatcher SEPARATOR_CHAR_MATCHER = CharMatcher.anyOf("_,-");
  private static final Splitter SEPERATOR_SPLITTER = Splitter.on(SEPARATOR_CHAR_MATCHER).omitEmptyStrings();

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
   * Tries to extract a SemVer like version from the given version string by removing all chars, splitting at specific
   * separators and parsing the remaining numbers. If no numbers are included in the given version, the version will be
   * {@code 0.0.0}. The original version input is preserved as the display string.
   *
   * @param version the version to parse.
   * @return a parsed, unknown version based on the given input version string.
   * @throws NullPointerException if the given version string is null.
   */
  public static @NonNull UnknownModuleVersion parse(@NonNull String version) {
    // remove all non-number, non-separator chars from the version string
    var matcher = NON_SEPERATOR_NUMBER_PATTERN.matcher(version);
    var cleanedVersion = matcher.replaceAll("");
    if (cleanedVersion.isEmpty()) {
      return new UnknownModuleVersion(0, 0, 0, version);
    }

    var parts = SEPERATOR_SPLITTER.splitToList(cleanedVersion);
    if (parts.isEmpty()) {
      return new UnknownModuleVersion(0, 0, 0, version);
    }

    // compact down everything after the patch version into a single path version number
    if (parts.size() > 3) {
      var trailingParts = parts.subList(2, parts.size());
      var trailingPart = String.join("", trailingParts);
      parts = List.of(parts.getFirst(), parts.get(1), trailingPart);
    }

    var major = Ints.tryParse(parts.getFirst());
    var minor = parts.size() >= 2 ? Ints.tryParse(parts.get(1)) : null;
    var patch = parts.size() >= 3 ? Ints.tryParse(parts.get(2)) : null;
    return new UnknownModuleVersion(
      Objects.requireNonNullElse(major, 0),
      Objects.requireNonNullElse(minor, 0),
      Objects.requireNonNullElse(patch, 0),
      version);
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
  public int compareTo(@NotNull ModuleVersion otherVersion) {
    return MAJOR_MINOR_PATCH_COMPARATOR.compare(this, otherVersion);
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
