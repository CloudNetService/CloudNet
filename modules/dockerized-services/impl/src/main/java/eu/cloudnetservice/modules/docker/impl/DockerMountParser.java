/*
 * Copyright 2019-present CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License
 * You may obtain a copy of the License at
 *
 *     [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package eu.cloudnetservice.modules.docker.impl;

import java.util.Arrays;
import java.util.Objects;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class DockerMountParser {

  private DockerMountParser() {
    throw new UnsupportedOperationException();
  }

  /*
   * Parses a docker volume definition from short syntax
   */
  public static @NonNull ParsedVolume parseVolume(@NonNull String definition) {
    var raw = requireText(definition, "volume definition");
    var parts = splitDockerShortSyntax(raw, false);

    return switch (parts.length) {
      case 1 -> new ParsedVolume(null, normalizeContainerPath(parts[0]), false);
      case 2 -> new ParsedVolume(emptyToNull(parts[0]), normalizeContainerPath(parts[1]), false);
      case 3 -> new ParsedVolume(
        emptyToNull(parts[0]),
        normalizeContainerPath(parts[1]),
        isReadOnly(parts[2])
      );
      default -> throw new IllegalArgumentException("Invalid volume definition: " + definition);
    };
  }

  /*
   * Parses a docker bind mount definition from short syntax
   */
  public static @NonNull ParsedBindMount parseBindMount(@NonNull String definition) {
    var raw = requireText(definition, "bind definition");
    var parts = splitDockerShortSyntax(raw, true);

    return switch (parts.length) {
      case 1 -> new ParsedBindMount(normalizeHostPath(parts[0]), normalizeHostPath(parts[0]), false);
      case 2 -> new ParsedBindMount(normalizeHostPath(parts[0]), normalizeContainerPath(parts[1]), false);
      case 3 -> new ParsedBindMount(
        normalizeHostPath(parts[0]),
        normalizeContainerPath(parts[1]),
        isReadOnly(parts[2])
      );
      default -> throw new IllegalArgumentException("Invalid bind definition: " + definition);
    };
  }

  /*
   * Splits a docker short syntax string into source, target and options
   */
  private static @NonNull String[] splitDockerShortSyntax(@NonNull String input, boolean handleWindowsSource) {
    var firstSeparator = findSeparator(input, 0, handleWindowsSource);
    if (firstSeparator == -1) {
      return new String[]{input};
    }

    var secondSeparator = findSeparator(input, firstSeparator + 1, false);
    if (secondSeparator == -1) {
      return new String[]{
        input.substring(0, firstSeparator),
        input.substring(firstSeparator + 1)
      };
    }

    return new String[]{
      input.substring(0, firstSeparator),
      input.substring(firstSeparator + 1, secondSeparator),
      input.substring(secondSeparator + 1)
    };
  }

  /*
   * Finds the next valid colon separator in the definition
   */
  private static int findSeparator(@NonNull String input, int start, boolean handleWindowsSource) {
    for (var index = start; index < input.length(); index++) {
      if (input.charAt(index) == ':' && (!handleWindowsSource || !hasWindowsDriveSeparator(input, index))) {
        return index;
      }
    }

    return -1;
  }

  /*
   * Checks whether the colon belongs to a Windows drive path like C:\ or C:/
   */
  private static boolean hasWindowsDriveSeparator(@NonNull String input, int index) {
    if (index != 1 || input.length() < 3) {
      return false;
    }

    var driveLetter = input.charAt(0);
    var next = input.charAt(2);
    return Character.isLetter(driveLetter) && (next == '\\' || next == '/');
  }

  /*
   * Checks if the mount options mark the mount as read only
   */
  private static boolean isReadOnly(@NonNull String options) {
    return Arrays.stream(options.split(","))
      .map(String::trim)
      .anyMatch(opt -> opt.equalsIgnoreCase("ro"));
  }

  /*
   * Validates and normalizes the container target path
   */
  private static @NonNull String normalizeContainerPath(@NonNull String path) {
    var trimmed = requireText(path, "container target");
    if (isRelativePath(trimmed)) {
      throw new IllegalArgumentException("Container target path must be absolute: " + path);
    }

    return trimmed;
  }

  /*
   * Validates and normalizes the host source path
   */
  private static @NonNull String normalizeHostPath(@NonNull String path) {
    var trimmed = requireText(path, "bind source");
    if (isRelativePath(trimmed)) {
      throw new IllegalArgumentException("Bind source path must be absolute: " + path);
    }

    return trimmed;
  }

  /*
   * Checks if a path is relative for Unix or Windows
   */
  private static boolean isRelativePath(@NonNull String value) {
    if (value.startsWith("/")) {
      return false;
    }

    return value.length() < 3
      || !Character.isLetter(value.charAt(0))
      || value.charAt(1) != ':'
      || (value.charAt(2) != '\\' && value.charAt(2) != '/');
  }

  /*
   * Ensures a string is not null empty or blank
   */
  private static @NonNull String requireText(@Nullable String value, @NonNull String name) {
    Objects.requireNonNull(name, "name");
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be empty");
    }

    return value.trim();
  }

  /*
   * Returns null for blank strings, otherwise returns the trimmed value
   */
  private static @Nullable String emptyToNull(@NonNull String value) {
    return value.isBlank() ? null : value.trim();
  }

  /*
   * Parsed named or anonymous volume definition
   */
  public record ParsedVolume(
    @Nullable String source,
    @NonNull String target,
    boolean readOnly
  ) {

    /*
     * Returns true when the volume has a source path or name
     */
    public boolean hasSource() {
      return this.source != null && !this.source.isBlank();
    }
  }

  /*
   * Parsed bind mount definition
   */
  public record ParsedBindMount(
    @NonNull String source,
    @NonNull String target,
    boolean readOnly
  ) {
  }
}
