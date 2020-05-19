package de.dytanic.cloudnet.conf;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

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
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(object);

        this.entries.append(key, object);
        this.save();

        return this;
    }

    @Override
    public IConfigurationRegistry put(String key, String string) {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(string);

        this.entries.append(key, string);
        this.save();

        return this;
    }

    @Override
    public IConfigurationRegistry put(String key, Number number) {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(number);

        this.entries.append(key, number);
        this.save();

        return this;
    }

    @Override
    public IConfigurationRegistry put(String key, Boolean bool) {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(bool);

        this.entries.append(key, bool);
        this.save();

        return this;
    }

    @Override
    public IConfigurationRegistry put(String key, byte[] bytes) {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(bytes);

        this.entries.append(key, bytes);
        this.save();

        return this;
    }

    @Override
    public IConfigurationRegistry remove(String key) {
        Preconditions.checkNotNull(key);

        this.entries.remove(key);
        this.save();

        return this;
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
        if (!Files.exists(this.path)) {
            this.path.toFile().getParentFile().mkdirs();
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