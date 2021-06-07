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

package de.dytanic.cloudnet.driver.module;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode
public class ModuleDependency implements SerializableObject {

  private String repo;
  private String url;
  private String group;
  private String name;
  private String version;

  public ModuleDependency(String repo, String url, String group, String name, String version) {
    this.repo = repo;
    this.url = url;
    this.group = group;
    this.name = name;
    this.version = version;
  }

  public ModuleDependency(String repo, String group, String name, String version) {
    this.repo = repo;
    this.group = group;
    this.name = name;
    this.version = version;
  }

  public ModuleDependency(String url) {
    this.url = url;
  }

  public ModuleDependency() {
  }

  public String getRepo() {
    return this.repo;
  }

  public String getUrl() {
    return this.url;
  }

  public String getGroup() {
    return this.group;
  }

  public String getName() {
    return this.name;
  }

  public String getVersion() {
    return this.version;
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeOptionalString(this.repo);
    buffer.writeOptionalString(this.url);
    buffer.writeOptionalString(this.group);
    buffer.writeOptionalString(this.name);
    buffer.writeOptionalString(this.version);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.repo = buffer.readOptionalString();
    this.url = buffer.readOptionalString();
    this.group = buffer.readOptionalString();
    this.name = buffer.readOptionalString();
    this.version = buffer.readOptionalString();
  }
}
