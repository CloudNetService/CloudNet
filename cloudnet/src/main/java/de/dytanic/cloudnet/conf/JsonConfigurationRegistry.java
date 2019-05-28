package de.dytanic.cloudnet.conf;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class JsonConfigurationRegistry implements IConfigurationRegistry {

  private final int registryVersion = 1, lowestSupportedVersion = 1;

  private final Path path;

  private JsonDocument entries = new JsonDocument();

  public JsonConfigurationRegistry(Path path) {
    this.path = path;
    this.load();
  }

  @Override
  public IConfigurationRegistry put(String key, Object object) {
    Validate.checkNotNull(key);
    Validate.checkNotNull(object);

    entries.append(key, object);
    this.save();

    return this;
  }

  @Override
  public IConfigurationRegistry put(String key, String string) {
    Validate.checkNotNull(key);
    Validate.checkNotNull(string);

    entries.append(key, string);
    this.save();

    return this;
  }

  @Override
  public IConfigurationRegistry put(String key, Number number) {
    Validate.checkNotNull(key);
    Validate.checkNotNull(number);

    entries.append(key, number);
    this.save();

    return this;
  }

  @Override
  public IConfigurationRegistry put(String key, Boolean bool) {
    Validate.checkNotNull(key);
    Validate.checkNotNull(bool);

    entries.append(key, bool);
    this.save();

    return this;
  }

  @Override
  public IConfigurationRegistry put(String key, byte[] bytes) {
    Validate.checkNotNull(key);
    Validate.checkNotNull(bytes);

    entries.append(key, bytes);
    this.save();

    return this;
  }

  @Override
  public IConfigurationRegistry remove(String key) {
    Validate.checkNotNull(key);

    entries.remove(key);
    this.save();

    return this;
  }

  @Override
  public boolean contains(String key) {
    Validate.checkNotNull(key);

    return entries.contains(key);
  }

  @Override
  public <T> T getObject(String key, Class<T> clazz) {
    Validate.checkNotNull(key);
    Validate.checkNotNull(clazz);

    return entries.get(key, clazz);
  }

  @Override
  public <T> T getObject(String key, Type type) {
    Validate.checkNotNull(key);
    Validate.checkNotNull(type);

    return entries.get(key, type);
  }

  @Override
  public String getString(String key) {
    Validate.checkNotNull(key);

    return entries.getString(key);
  }

  @Override
  public String getString(String key, String def) {
    Validate.checkNotNull(key);
    Validate.checkNotNull(def);

    return entries.getString(key, def);
  }

  @Override
  public Integer getInt(String key) {
    Validate.checkNotNull(key);

    return entries.getInt(key);
  }

  @Override
  public Integer getInt(String key, Integer def) {
    Validate.checkNotNull(key);
    Validate.checkNotNull(def);

    return entries.getInt(key, def);
  }

  @Override
  public Double getDouble(String key) {
    Validate.checkNotNull(key);

    return entries.getDouble(key);
  }

  @Override
  public Double getDouble(String key, Double def) {
    Validate.checkNotNull(key);
    Validate.checkNotNull(def);

    return entries.getDouble(key, def);
  }

  @Override
  public Short getShort(String key) {
    Validate.checkNotNull(key);

    return entries.getShort(key);
  }

  @Override
  public Short getShort(String key, Short def) {
    Validate.checkNotNull(key);
    Validate.checkNotNull(def);

    return entries.getShort(key, def);
  }

  @Override
  public Long getLong(String key) {
    Validate.checkNotNull(key);

    return entries.getLong(key);
  }

  @Override
  public Long getLong(String key, Long def) {
    Validate.checkNotNull(key);
    Validate.checkNotNull(def);

    return entries.getLong(key, def);
  }

  @Override
  public Boolean getBoolean(String key) {
    Validate.checkNotNull(key);

    return entries.getBoolean(key);
  }

  @Override
  public Boolean getBoolean(String key, Boolean def) {
    Validate.checkNotNull(key);
    Validate.checkNotNull(def);

    return entries.getBoolean(key, def);
  }

  @Override
  public byte[] getBytes(String key) {
    Validate.checkNotNull(key);

    return entries.getBinary(key);
  }

  @Override
  public byte[] getBytes(String key, byte[] bytes) {
    Validate.checkNotNull(key);
    Validate.checkNotNull(bytes);

    return entries.getBinary(key, bytes);
  }

  @Override
  public IConfigurationRegistry save() {
    new JsonDocument()
        .append("registryVersion", registryVersion)
        .append("entries", entries)
        .write(path);

    return this;
  }

  @Override
  public IConfigurationRegistry load() {
    if (!Files.exists(path)) {
      path.toFile().getParentFile().mkdirs();
      this.save();
    }

    JsonDocument loaded = JsonDocument.newDocument(this.path);
    if (loaded.contains("registryVersion") && loaded.contains("entries")) {
      if (lowestSupportedVersion <= loaded.getInt("registryVersion")) {
        this.entries = loaded.getDocument("entries");
      }
    }

    return this;
  }
}