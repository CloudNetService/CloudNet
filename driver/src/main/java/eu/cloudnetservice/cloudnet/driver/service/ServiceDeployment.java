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

package eu.cloudnetservice.cloudnet.driver.service;

import com.google.common.base.Verify;
import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.common.document.property.JsonDocPropertyHolder;
import java.util.Collection;
import java.util.HashSet;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

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
    @NonNull ServiceTemplate template,
    @NonNull Collection<String> excludes,
    @NonNull JsonDocument properties
  ) {
    this.template = template;
    this.excludes = excludes;
    this.properties = properties;
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull ServiceDeployment deployment) {
    return builder()
      .template(deployment.template())
      .excludes(deployment.excludes())
      .properties(deployment.properties().clone());
  }

  public @NonNull ServiceTemplate template() {
    return this.template;
  }

  public @NonNull Collection<String> excludes() {
    return this.excludes;
  }

  @Override
  public @NonNull ServiceDeployment clone() {
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

    public @NonNull Builder template(@NonNull ServiceTemplate template) {
      this.template = template;
      return this;
    }

    public @NonNull Builder excludes(@NonNull Collection<String> excludes) {
      this.excludes = new HashSet<>(excludes);
      return this;
    }

    public @NonNull Builder addExclude(@NonNull String exclude) {
      this.excludes.add(exclude);
      return this;
    }

    public @NonNull Builder properties(@NonNull JsonDocument properties) {
      this.properties = properties;
      return this;
    }

    public @NonNull ServiceDeployment build() {
      Verify.verifyNotNull(this.template, "no target template given");
      return new ServiceDeployment(this.template, this.excludes, this.properties);
    }
  }
}
