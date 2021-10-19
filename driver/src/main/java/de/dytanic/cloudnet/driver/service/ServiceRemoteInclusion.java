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

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.document.property.JsonDocPropertyHolder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = false)
public final class ServiceRemoteInclusion extends JsonDocPropertyHolder {

  private final String url;
  private final String destination;

  public ServiceRemoteInclusion(String url, String destination) {
    this(url, destination, JsonDocument.newDocument());
  }

  public ServiceRemoteInclusion(String url, String destination, JsonDocument properties) {
    this.url = url;
    this.destination = destination;
    this.properties = properties;
  }

  public String getUrl() {
    return this.url;
  }

  public String getDestination() {
    return this.destination;
  }
}
