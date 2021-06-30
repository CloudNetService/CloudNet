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

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.driver.serialization.json.SerializableJsonDocPropertyable;
import java.util.Collection;
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
public final class ServiceDeployment extends SerializableJsonDocPropertyable implements SerializableObject {

  private ServiceTemplate template;
  private Collection<String> excludes;

  public ServiceDeployment(ServiceTemplate template, Collection<String> excludes) {
    this.template = template;
    this.excludes = excludes;
  }

  public ServiceDeployment() {
  }

  public ServiceTemplate getTemplate() {
    return this.template;
  }

  public Collection<String> getExcludes() {
    return this.excludes;
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeObject(this.template);
    buffer.writeStringCollection(this.excludes);

    super.write(buffer);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.template = buffer.readObject(ServiceTemplate.class);
    this.excludes = buffer.readStringCollection();

    super.read(buffer);
  }
}
