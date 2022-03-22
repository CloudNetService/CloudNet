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

package eu.cloudnetservice.cloudnet.node.version;

import eu.cloudnetservice.cloudnet.common.JavaVersion;
import eu.cloudnetservice.cloudnet.common.Nameable;
import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;

public class ServiceVersion implements Nameable {

  private String name;
  private String url;

  private int minJavaVersion;
  private int maxJavaVersion;

  private boolean deprecated;
  private boolean cacheFiles = true;

  private JsonDocument properties = JsonDocument.emptyDocument();
  private Map<String, String> additionalDownloads = Collections.emptyMap();

  public ServiceVersion(
    @NonNull String name,
    @NonNull String url,
    int minJavaVersion,
    int maxJavaVersion,
    boolean deprecated,
    boolean cacheFiles,
    @NonNull JsonDocument properties,
    @NonNull Map<String, String> additionalDownloads
  ) {
    this.name = name;
    this.url = url;
    this.minJavaVersion = minJavaVersion;
    this.maxJavaVersion = maxJavaVersion;
    this.deprecated = deprecated;
    this.cacheFiles = cacheFiles;
    this.properties = properties;
    this.additionalDownloads = additionalDownloads;
  }

  public ServiceVersion() {
  }

  public boolean cacheFiles() {
    return this.cacheFiles;
  }

  public boolean deprecated() {
    return this.deprecated;
  }

  public boolean canRun() {
    return this.canRun(JavaVersion.runtimeVersion());
  }

  public boolean canRun(@NonNull JavaVersion javaVersion) {
    var minJavaVersion = JavaVersion.fromVersion(this.minJavaVersion);
    var maxJavaVersion = JavaVersion.fromVersion(this.maxJavaVersion);

    if (minJavaVersion.isPresent() && maxJavaVersion.isPresent()) {
      return javaVersion.isSupported(minJavaVersion.get(), maxJavaVersion.get());
    }

    return minJavaVersion.map(javaVersion::isSupportedByMin)
      .orElseGet(() -> maxJavaVersion.map(javaVersion::isSupportedByMax).orElse(true));
  }

  public @NonNull String name() {
    return this.name;
  }

  public String url() {
    return this.url;
  }

  public void url(@NonNull String url) {
    this.url = url;
  }

  public @NonNull Optional<JavaVersion> minJavaVersion() {
    return JavaVersion.fromVersion(this.minJavaVersion);
  }

  public @NonNull Optional<JavaVersion> maxJavaVersion() {
    return JavaVersion.fromVersion(this.maxJavaVersion);
  }

  public @NonNull JsonDocument properties() {
    return this.properties;
  }

  public @NonNull Map<String, String> additionalDownloads() {
    return this.additionalDownloads;
  }
}
