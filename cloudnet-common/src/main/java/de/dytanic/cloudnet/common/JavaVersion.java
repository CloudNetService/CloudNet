/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.common;

import java.util.Arrays;
import java.util.Optional;

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
  JAVA_17(17, 61D, "Java 17");

  private final int version;
  private final double versionId;
  private final String name;

  JavaVersion(int version, double versionId, String name) {
    this.version = version;
    this.versionId = versionId;
    this.name = name;
  }

  public static JavaVersion getRuntimeVersion() {
    double versionId = Double.parseDouble(System.getProperty("java.class.version"));
    return fromVersionId(versionId).orElse(UNKNOWN);
  }

  public static Optional<JavaVersion> fromVersionId(double versionId) {
    return Arrays.stream(values()).filter(javaVersion -> javaVersion.versionId == versionId).findFirst();
  }

  public static Optional<JavaVersion> fromVersion(int version) {
    return Arrays.stream(values()).filter(javaVersion -> javaVersion.version == version).findFirst();
  }

  public int getVersion() {
    return this.version;
  }

  public double getVersionId() {
    return this.versionId;
  }

  public String getName() {
    return this.name;
  }

  public boolean isUnknown() {
    return this == UNKNOWN;
  }

  public boolean isSupported(JavaVersion minJavaVersion, JavaVersion maxJavaVersion) {
    return this.isUnknown() || this.versionId >= minJavaVersion.versionId && this.versionId <= maxJavaVersion.versionId;
  }

  public boolean isSupportedByMin(JavaVersion minRequiredJavaVersion) {
    return this.isUnknown() || this.versionId >= minRequiredJavaVersion.versionId;
  }

  public boolean isSupportedByMax(JavaVersion maxRequiredJavaVersion) {
    return this.isUnknown() || this.versionId <= maxRequiredJavaVersion.versionId;
  }

}
