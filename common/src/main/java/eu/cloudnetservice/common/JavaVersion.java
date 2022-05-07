/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

  UNKNOWN(-1, -1D, "Unknown Java"),
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
  JAVA_19(19, 63D, "Java 19");

  private static final JavaVersion[] JAVA_VERSIONS = JavaVersion.values();

  private final int version;
  private final double classFileVersion;
  private final String name;

  JavaVersion(int version, double classFileVersion, @NonNull String name) {
    this.version = version;
    this.classFileVersion = classFileVersion;
    this.name = name;
  }

  public static @NonNull JavaVersion runtimeVersion() {
    var classVersion = Double.parseDouble(System.getProperty("java.class.version"));
    return fromClassFileVersion(classVersion).orElse(UNKNOWN);
  }

  public static @NonNull Optional<JavaVersion> fromClassFileVersion(double versionId) {
    return Arrays.stream(JAVA_VERSIONS).filter(javaVersion -> javaVersion.classFileVersion == versionId).findFirst();
  }

  public static @NonNull Optional<JavaVersion> fromVersion(int version) {
    return Arrays.stream(JAVA_VERSIONS).filter(javaVersion -> javaVersion.version == version).findFirst();
  }

  public int version() {
    return this.version;
  }

  public double classFileVersion() {
    return this.classFileVersion;
  }

  public @NonNull String displayName() {
    return this.name;
  }

  public boolean unknown() {
    return this == UNKNOWN;
  }

  public boolean isSupported(@NonNull JavaVersion minJavaVersion, @NonNull JavaVersion maxJavaVersion) {
    return this.unknown() || this.classFileVersion >= minJavaVersion.classFileVersion
      && this.classFileVersion <= maxJavaVersion.classFileVersion;
  }

  public boolean isSupportedByMin(@NonNull JavaVersion minRequiredJavaVersion) {
    return this.unknown() || this.classFileVersion >= minRequiredJavaVersion.classFileVersion;
  }

  public boolean isSupportedByMax(@NonNull JavaVersion maxRequiredJavaVersion) {
    return this.unknown() || this.classFileVersion <= maxRequiredJavaVersion.classFileVersion;
  }
}
