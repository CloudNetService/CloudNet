/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.common;

import java.util.Arrays;
import java.util.Optional;
import lombok.NonNull;

public enum JavaVersion {

  JAVA_UNSUPPORTED(-1, -1D, "Unsupported Java"),
  JAVA_NEXT(Integer.MAX_VALUE, Double.MAX_VALUE, "Next Java"),

  JAVA_8(8, 52D, "Java 8"),
  JAVA_9(9, 53D, "Java 9"),
  JAVA_10(10, 54D, "Java 10"),
  JAVA_11(11, 55D, "Java 11"),
  JAVA_12(12, 56D, "Java 12"),
  JAVA_13(13, 57D, "Java 13"),
  JAVA_14(14, 58D, "Java 14"),
  JAVA_15(15, 59D, "Java 15"),
  JAVA_16(16, 60D, "Java 16"),
  JAVA_17(17, 61D, "Java 17"),
  JAVA_18(18, 62D, "Java 18"),
  JAVA_19(19, 63D, "Java 19"),
  JAVA_20(20, 64D, "Java 20"),
  JAVA_21(21, 65D, "Java 21");

  private static final JavaVersion[] JAVA_VERSIONS = resolveActualJavaVersions();
  private static final JavaVersion LATEST_VERSION = JAVA_VERSIONS[JAVA_VERSIONS.length - 1];

  private final int majorVersion;
  private final double classFileVersion;
  private final String displayName;

  JavaVersion(int majorVersion, double classFileVersion, @NonNull String displayName) {
    this.majorVersion = majorVersion;
    this.classFileVersion = classFileVersion;
    this.displayName = displayName;
  }

  private static @NonNull JavaVersion[] resolveActualJavaVersions() {
    // remove index 0 and 1 (UNSUPPORTED and NEXT) as they shouldn't be resolvable
    var values = JavaVersion.values();
    return Arrays.copyOfRange(values, JAVA_8.ordinal(), values.length);
  }

  public static @NonNull JavaVersion runtimeVersion() {
    var jcv = Double.parseDouble(System.getProperty("java.class.version"));
    return fromClassFileVersion(jcv).orElse(jcv > LATEST_VERSION.classFileVersion() ? JAVA_NEXT : JAVA_UNSUPPORTED);
  }

  public static @NonNull Optional<JavaVersion> fromClassFileVersion(double classFileVersion) {
    return Arrays.stream(JAVA_VERSIONS).filter(version -> version.classFileVersion() == classFileVersion).findFirst();
  }

  public static @NonNull JavaVersion guessFromMajor(int major) {
    return fromMajor(major).orElse(major > LATEST_VERSION.majorVersion() ? JAVA_NEXT : JAVA_UNSUPPORTED);
  }

  public static @NonNull Optional<JavaVersion> fromMajor(int version) {
    return Arrays.stream(JAVA_VERSIONS).filter(javaVersion -> javaVersion.majorVersion == version).findFirst();
  }

  public int majorVersion() {
    return this.majorVersion;
  }

  public double classFileVersion() {
    return this.classFileVersion;
  }

  public @NonNull String displayName() {
    return this.displayName;
  }

  public boolean supported() {
    return this != JAVA_UNSUPPORTED;
  }

  public boolean isInRange(@NonNull JavaVersion lowerBound, @NonNull JavaVersion upperBound) {
    return this.isNewerOrAt(lowerBound) && this.isOlderOrAt(upperBound);
  }

  public boolean isNewerOrAt(@NonNull JavaVersion lowerBound) {
    return lowerBound.supported() && this.classFileVersion >= lowerBound.classFileVersion;
  }

  public boolean isOlderOrAt(@NonNull JavaVersion upperBound) {
    return this.supported() && this.classFileVersion <= upperBound.classFileVersion;
  }
}
