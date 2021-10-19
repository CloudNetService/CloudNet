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

package de.dytanic.cloudnet.template.install;

import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.JavaVersion;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public class ServiceVersion implements INameable {

  private String name;
  private String url;

  private int minJavaVersion;
  private int maxJavaVersion;

  private boolean deprecated;
  private boolean cacheFiles = true;

  private JsonDocument properties = JsonDocument.empty();
  private Map<String, String> additionalDownloads = Collections.emptyMap();

  public ServiceVersion(
    @NotNull String name,
    @NotNull String url,
    int minJavaVersion,
    int maxJavaVersion,
    boolean deprecated,
    boolean cacheFiles,
    @NotNull JsonDocument properties,
    @NotNull Map<String, String> additionalDownloads
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

  public boolean isCacheFiles() {
    return this.cacheFiles;
  }

  public boolean isDeprecated() {
    return this.deprecated;
  }

  public boolean canRun() {
    return this.canRun(JavaVersion.getRuntimeVersion());
  }

  public boolean canRun(@NotNull JavaVersion javaVersion) {
    Optional<JavaVersion> minJavaVersion = JavaVersion.fromVersion(this.minJavaVersion);
    Optional<JavaVersion> maxJavaVersion = JavaVersion.fromVersion(this.maxJavaVersion);

    if (minJavaVersion.isPresent() && maxJavaVersion.isPresent()) {
      return javaVersion.isSupported(minJavaVersion.get(), maxJavaVersion.get());
    }

    return minJavaVersion.map(javaVersion::isSupportedByMin)
      .orElseGet(() -> maxJavaVersion.map(javaVersion::isSupportedByMax).orElse(true));
  }

  public @NotNull String getName() {
    return this.name;
  }

  public String getUrl() {
    return this.url;
  }

  public void setUrl(@NotNull String url) {
    this.url = url;
  }

  public @NotNull JsonDocument getProperties() {
    return this.properties;
  }

  public @NotNull Map<String, String> getAdditionalDownloads() {
    return this.additionalDownloads;
  }
}
