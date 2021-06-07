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

package de.dytanic.cloudnet.conf;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public final class JsonConfigurationRegistry implements IConfigurationRegistry {

  private final int registryVersion = 1;
  private final int lowestSupportedVersion = 1;

  private final Path path;

  private JsonDocument entries = new JsonDocument();

  public JsonConfigurationRegistry(Path path) {
    this.path = path;
    this.load();
  }

  private IConfigurationRegistry update(String key, Consumer<JsonDocument> modifier) {
    Preconditions.checkNotNull(key);

    modifier.accept(this.entries);
    this.save();

    return this;
  }

  @Override
  public IConfigurationRegistry put(String key, Object object) {
    return this.update(key, document -> document.append(key, object));
  }

  @Override
  public IConfigurationRegistry put(String key, String object) {
    Preconditions.checkNotNull(object);
    return this.update(key, document -> document.append(key, object));
  }

  @Override
  public IConfigurationRegistry put(String key, Number object) {
    Preconditions.checkNotNull(object);
    return this.update(key, document -> document.append(key, object));
  }

  @Override
  public IConfigurationRegistry put(String key, Boolean object) {
    Preconditions.checkNotNull(object);
    return this.update(key, document -> document.append(key, object));
  }

  @Override
  public IConfigurationRegistry put(String key, byte[] object) {
    Preconditions.checkNotNull(object);
    return this.update(key, document -> document.append(key, object));
  }

  @Override
  public IConfigurationRegistry remove(String key) {
    return this.update(key, document -> document.remove(key));
  }

  @Override
  public boolean contains(String key) {
    Preconditions.checkNotNull(key);

    return this.entries.contains(key);
  }

  @Override
  public <T> T getObject(String key, Class<T> clazz) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(clazz);

    return this.entries.get(key, clazz);
  }

  @Override
  public <T> T getObject(String key, Type type) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(type);

    return this.entries.get(key, type);
  }

  @Override
  public String getString(String key) {
    Preconditions.checkNotNull(key);

    return this.entries.getString(key);
  }

  @Override
  public String getString(String key, String def) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(def);

    return this.entries.getString(key, def);
  }

  @Override
  public Integer getInt(String key) {
    Preconditions.checkNotNull(key);

    return this.entries.getInt(key);
  }

  @Override
  public Integer getInt(String key, Integer def) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(def);

    return this.entries.getInt(key, def);
  }

  @Override
  public Double getDouble(String key) {
    Preconditions.checkNotNull(key);

    return this.entries.getDouble(key);
  }

  @Override
  public Double getDouble(String key, Double def) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(def);

    return this.entries.getDouble(key, def);
  }

  @Override
  public Short getShort(String key) {
    Preconditions.checkNotNull(key);

    return this.entries.getShort(key);
  }

  @Override
  public Short getShort(String key, Short def) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(def);

    return this.entries.getShort(key, def);
  }

  @Override
  public Long getLong(String key) {
    Preconditions.checkNotNull(key);

    return this.entries.getLong(key);
  }

  @Override
  public Long getLong(String key, Long def) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(def);

    return this.entries.getLong(key, def);
  }

  @Override
  public Boolean getBoolean(String key) {
    Preconditions.checkNotNull(key);

    return this.entries.getBoolean(key);
  }

  @Override
  public Boolean getBoolean(String key, Boolean def) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(def);

    return this.entries.getBoolean(key, def);
  }

  @Override
  public byte[] getBytes(String key) {
    Preconditions.checkNotNull(key);

    return this.entries.getBinary(key);
  }

  @Override
  public byte[] getBytes(String key, byte[] bytes) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(bytes);

    return this.entries.getBinary(key, bytes);
  }

  @Override
  public IConfigurationRegistry save() {
    new JsonDocument()
      .append("registryVersion", this.registryVersion)
      .append("entries", this.entries)
      .write(this.path);

    return this;
  }

  @Override
  public IConfigurationRegistry load() {
    if (Files.notExists(this.path)) {
      FileUtils.createDirectoryReported(this.path.getParent());
      this.save();
    }

    JsonDocument loaded = JsonDocument.newDocument(this.path);
    if (loaded.contains("registryVersion") && loaded.contains("entries")) {
      if (this.lowestSupportedVersion <= loaded.getInt("registryVersion")) {
        this.entries = loaded.getDocument("entries");
      }
    }

    return this;
  }

  public int getRegistryVersion() {
    return this.registryVersion;
  }

  public int getLowestSupportedVersion() {
    return this.lowestSupportedVersion;
  }

  public Path getPath() {
    return this.path;
  }

  public JsonDocument getEntries() {
    return this.entries;
  }

}
