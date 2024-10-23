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

package eu.cloudnetservice.driver.base;

import java.util.Arrays;
import java.util.Optional;
import lombok.NonNull;

/**
 * An enumeration of all java versions starting from 8. This list is updated each time an early access build for a new
 * java version gets released.
 *
 * @since 4.0
 */
public enum JavaVersion {

  /**
   * Represents all java version before 8.
   */
  JAVA_UNSUPPORTED(-1, -1D, "Unsupported Java"),
  /**
   * Represents all java versions after the latest known release in this list.
   */
  JAVA_NEXT(Integer.MAX_VALUE, Double.MAX_VALUE, "Next Java"),

  /**
   * Java version 8 (LTS).
   */
  JAVA_8(8, 52D, "Java 8"),
  /**
   * Java version 9.
   */
  JAVA_9(9, 53D, "Java 9"),
  /**
   * Java version 10.
   */
  JAVA_10(10, 54D, "Java 10"),
  /**
   * Java version 11 (LTS).
   */
  JAVA_11(11, 55D, "Java 11"),
  /**
   * Java version 12.
   */
  JAVA_12(12, 56D, "Java 12"),
  /**
   * Java version 13.
   */
  JAVA_13(13, 57D, "Java 13"),
  /**
   * Java version 14.
   */
  JAVA_14(14, 58D, "Java 14"),
  /**
   * Java version 15.
   */
  JAVA_15(15, 59D, "Java 15"),
  /**
   * Java version 16.
   */
  JAVA_16(16, 60D, "Java 16"),
  /**
   * Java version 17 (LTS).
   */
  JAVA_17(17, 61D, "Java 17"),
  /**
   * Java version 18.
   */
  JAVA_18(18, 62D, "Java 18"),
  /**
   * Java version 19.
   */
  JAVA_19(19, 63D, "Java 19"),
  /**
   * Java version 20.
   */
  JAVA_20(20, 64D, "Java 20"),
  /**
   * Java version 21 (LTS).
   */
  JAVA_21(21, 65D, "Java 21"),
  /**
   * Java version 22.
   */
  JAVA_22(22, 66D, "Java 22"),
  /**
   * Java version 23.
   */
  JAVA_23(23, 67D, "Java 23"),
  /**
   * Java version 24.
   */
  JAVA_24(24, 68D, "Java 24");

  private static final JavaVersion[] JAVA_VERSIONS = resolveActualJavaVersions();
  private static final JavaVersion LATEST_VERSION = JAVA_VERSIONS[JAVA_VERSIONS.length - 1];

  // internal note: the resolveRuntimeVersion() call depends on the LATEST_VERSION field, do not move this up
  private static final JavaVersion RUNTIME_VERSION = resolveRuntimeVersion();

  private final int majorVersion;
  private final double classFileVersion;
  private final String displayName;

  /**
   * Constructs a new java version constant.
   *
   * @param majorVersion     the major version number of the release.
   * @param classFileVersion the class file version used by the release.
   * @param displayName      the display name of the release.
   * @throws NullPointerException if the given display name is null.
   */
  JavaVersion(int majorVersion, double classFileVersion, @NonNull String displayName) {
    this.majorVersion = majorVersion;
    this.classFileVersion = classFileVersion;
    this.displayName = displayName;
  }

  /**
   * Constructs an array of the java versions that are actually usable in the runtime (this excludes the unsupported and
   * next constants).
   *
   * @return an array of the java versions that are actually executable.
   */
  private static @NonNull JavaVersion[] resolveActualJavaVersions() {
    // remove index 0 and 1 (UNSUPPORTED and NEXT) as they shouldn't be resolvable
    var values = JavaVersion.values();
    return Arrays.copyOfRange(values, JAVA_8.ordinal(), values.length);
  }

  /**
   * Resolves the runtime version of the current jvm. This method returns {@link #JAVA_NEXT} if the current jvm version
   * is newer than the latest registered version in this enum. On the other hand, this method returns
   * {@link #JAVA_UNSUPPORTED} if the runtime version is older than 8.
   *
   * @return the java version enum constant representing the current jvm version.
   */
  private static @NonNull JavaVersion resolveRuntimeVersion() {
    var jcv = Double.parseDouble(System.getProperty("java.class.version"));
    return fromClassFileVersion(jcv).orElse(jcv > LATEST_VERSION.classFileVersion() ? JAVA_NEXT : JAVA_UNSUPPORTED);
  }

  /**
   * Get the java version enum constant representing the current runtime version.
   *
   * @return the java version enum constant representing the current runtime version.
   */
  public static @NonNull JavaVersion runtimeVersion() {
    return RUNTIME_VERSION;
  }

  /**
   * Finds the java version enum constant that is representing the given class file version. This method returns an
   * empty optional if no known java version represents that version.
   *
   * @param classFileVersion the class file version to find the java version constant of.
   * @return an optional containing the java version that is associated with the given class file version, if any.
   */
  public static @NonNull Optional<JavaVersion> fromClassFileVersion(double classFileVersion) {
    for (var javaVersion : JAVA_VERSIONS) {
      if (javaVersion.classFileVersion() == classFileVersion) {
        return Optional.of(javaVersion);
      }
    }

    return Optional.empty();
  }

  /**
   * Guesses the java version from the given major version, returning either {@link #JAVA_NEXT} or
   * {@link #JAVA_UNSUPPORTED} in case no known version uses the given major version.
   *
   * @param major the major version number of the java version to get.
   * @return the exact matching java version from the major version, or either next or unsupported.
   */
  public static @NonNull JavaVersion guessFromMajor(int major) {
    return fromMajor(major).orElse(major > LATEST_VERSION.majorVersion() ? JAVA_NEXT : JAVA_UNSUPPORTED);
  }

  /**
   * Finds the exact known java version from the given major version. This method returns an empty optional in case no
   * known java version with the given major version number matches.
   *
   * @param version the major version number of the java version to find.
   * @return an optional containing the java version associated with the given major version, if any.
   */
  public static @NonNull Optional<JavaVersion> fromMajor(int version) {
    for (var javaVersion : JAVA_VERSIONS) {
      if (javaVersion.majorVersion() == version) {
        return Optional.of(javaVersion);
      }
    }

    return Optional.empty();
  }

  /**
   * Returns the major version number of this java version.
   *
   * @return the major version number of this java version.
   */
  public int majorVersion() {
    return this.majorVersion;
  }

  /**
   * Get the class file version number of this java version.
   *
   * @return the class file version number of this java version.
   */
  public double classFileVersion() {
    return this.classFileVersion;
  }

  /**
   * Get the display name of this java version.
   *
   * @return the display name of this java version.
   */
  public @NonNull String displayName() {
    return this.displayName;
  }

  /**
   * Checks if this java version constant is actually supported. In other words, this method checks if this java version
   * is not {@link #JAVA_UNSUPPORTED}.
   *
   * @return true if this java version constant is actually supported, false otherwise.
   */
  public boolean supported() {
    return this != JAVA_UNSUPPORTED;
  }

  /**
   * Checks if the version of the current java runtime is at least this java version.
   *
   * @return true if the runtime version is at least this java version, false otherwise.
   */
  public boolean atOrAbove() {
    return RUNTIME_VERSION.isNewerOrAt(this);
  }

  /**
   * Checks if this java version is newer or at the given lower bound and older or at the given upper bound. There is no
   * sanity check made if the lower bound is smaller than the upper bound.
   *
   * @param lowerBound the lower bound of the version range to check.
   * @param upperBound the upper bound of the version range to check.
   * @return true if this version is within the lower and upper java version bound, false otherwise.
   * @throws NullPointerException if the given lower or upper bound is null.
   */
  public boolean isInRange(@NonNull JavaVersion lowerBound, @NonNull JavaVersion upperBound) {
    return this.isNewerOrAt(lowerBound) && this.isOlderOrAt(upperBound);
  }

  /**
   * Checks if this java version is newer or at the given other version.
   *
   * @param version the version to check against.
   * @return true if this version is newer or at the given version, false otherwise.
   * @throws NullPointerException if the given version is null.
   */
  public boolean isNewerOrAt(@NonNull JavaVersion version) {
    return version.supported() && this.classFileVersion >= version.classFileVersion;
  }

  /**
   * Checks if this java version is older or at the given other version.
   *
   * @param version the version to check against.
   * @return true if this version is older or at the given version, false otherwise.
   * @throws NullPointerException if the given version is null.
   */
  public boolean isOlderOrAt(@NonNull JavaVersion version) {
    return this.supported() && this.classFileVersion <= version.classFileVersion;
  }
}
