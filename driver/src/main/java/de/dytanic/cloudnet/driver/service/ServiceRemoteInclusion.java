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
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode(callSuper = false)
public class ServiceRemoteInclusion extends JsonDocPropertyHolder implements Cloneable {

  private final String url;
  private final String destination;

  protected ServiceRemoteInclusion(String url, String destination, JsonDocument properties) {
    this.url = url;
    this.destination = destination;
    this.properties = properties;
  }

  public static @NotNull Builder builder() {
    return new Builder();
  }

  public static @NotNull Builder builder(@NotNull ServiceRemoteInclusion inclusion) {
    return builder()
      .url(inclusion.getUrl())
      .destination(inclusion.getDestination())
      .properties(inclusion.getProperties().clone());
  }

  public @NotNull String getUrl() {
    return this.url;
  }

  public @NotNull String getDestination() {
    return this.destination;
  }

  @Override
  public @NotNull ServiceRemoteInclusion clone() {
    try {
      return (ServiceRemoteInclusion) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen - just explode
    }
  }

  public static class Builder {

    protected String url;
    protected String destination;
    protected JsonDocument properties = JsonDocument.newDocument();

    public @NotNull Builder url(@NotNull String url) {
      this.url = url;
      return this;
    }

    public @NotNull Builder destination(@NotNull String destination) {
      this.destination = destination;
      return this;
    }

    public @NotNull Builder properties(@NotNull JsonDocument properties) {
      this.properties = properties;
      return this;
    }

    public @NotNull ServiceRemoteInclusion build() {
      Verify.verifyNotNull(this.url, "no url given");
      Verify.verifyNotNull(this.destination, "no destination given");

      return new ServiceRemoteInclusion(this.url, this.destination, this.properties);
    }
  }
}
