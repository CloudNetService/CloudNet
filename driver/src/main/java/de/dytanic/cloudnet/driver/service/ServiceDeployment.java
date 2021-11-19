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

package de.dytanic.cloudnet.driver.service;

import com.google.common.base.Verify;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.document.property.JsonDocPropertyHolder;
import java.util.Collection;
import java.util.HashSet;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

/**
 * Defines the properties for a service that is copied to a specified template. It contains the {@link ServiceTemplate}
 * and {@code excludes} for excluded files/directories. For excluded files, just add the path to the file to the
 * excludes (e.g. "spigot.jar"). For excluded directories, the path has to end with a "/" (e.g. "plugins/").
 */
@ToString
@EqualsAndHashCode(callSuper = false)
public class ServiceDeployment extends JsonDocPropertyHolder implements Cloneable {

  protected final ServiceTemplate template;
  protected final Collection<String> excludes;

  protected ServiceDeployment(
    @NotNull ServiceTemplate template,
    @NotNull Collection<String> excludes,
    @NotNull JsonDocument properties
  ) {
    this.template = template;
    this.excludes = excludes;
    this.properties = properties;
  }

  public static @NotNull Builder builder() {
    return new Builder();
  }

  public static @NotNull Builder builder(@NotNull ServiceDeployment deployment) {
    return builder()
      .template(deployment.getTemplate())
      .excludes(deployment.getExcludes())
      .properties(deployment.getProperties().clone());
  }

  public @NotNull ServiceTemplate getTemplate() {
    return this.template;
  }

  public @NotNull Collection<String> getExcludes() {
    return this.excludes;
  }

  @Override
  public @NotNull ServiceDeployment clone() {
    try {
      return (ServiceDeployment) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen - just explode
    }
  }

  public static class Builder {

    protected ServiceTemplate template;
    protected Collection<String> excludes = new HashSet<>();
    protected JsonDocument properties = JsonDocument.newDocument();

    public @NotNull Builder template(@NotNull ServiceTemplate template) {
      this.template = template;
      return this;
    }

    public @NotNull Builder excludes(@NotNull Collection<String> excludes) {
      this.excludes = new HashSet<>(excludes);
      return this;
    }

    public @NotNull Builder addExclude(@NotNull String exclude) {
      this.excludes.add(exclude);
      return this;
    }

    public @NotNull Builder properties(@NotNull JsonDocument properties) {
      this.properties = properties;
      return this;
    }

    public @NotNull ServiceDeployment build() {
      Verify.verifyNotNull(this.template, "no target template given");
      return new ServiceDeployment(this.template, this.excludes, this.properties);
    }
  }
}
