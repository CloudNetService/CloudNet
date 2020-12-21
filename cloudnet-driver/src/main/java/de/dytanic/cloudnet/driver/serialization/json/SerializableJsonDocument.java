package de.dytanic.cloudnet.driver.serialization.json;

import com.google.gson.JsonParser;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Properties;

public class SerializableJsonDocument extends JsonDocument implements SerializableObject {

    private SerializableJsonDocument(JsonDocument document) {
        super(document.toJsonObject());
    }

    public SerializableJsonDocument() {
        super();
    }

    public SerializableJsonDocument(Object toObjectMirror) {
        super(toObjectMirror);
    }

    public SerializableJsonDocument(Properties properties) {
        super(properties);
    }

    public SerializableJsonDocument(String key, String value) {
        super(key, value);
    }

    public SerializableJsonDocument(String key, Object value) {
        super(key, value);
    }

    public SerializableJsonDocument(String key, Boolean value) {
        super(key, value);
    }

    public SerializableJsonDocument(String key, Number value) {
        super(key, value);
    }

    public SerializableJsonDocument(String key, Character value) {
        super(key, value);
    }

    public SerializableJsonDocument(String key, JsonDocument value) {
        super(key, value);
    }

    public SerializableJsonDocument(String key, Properties value) {
        super(key, value);
    }

    public static SerializableJsonDocument newDocument() {
        return new SerializableJsonDocument();
    }

    public static SerializableJsonDocument newDocument(String key, String value) {
        return new SerializableJsonDocument(key, value);
    }

    public static SerializableJsonDocument newDocument(String key, Number value) {
        return new SerializableJsonDocument(key, value);
    }

    public static SerializableJsonDocument newDocument(String key, Character value) {
        return new SerializableJsonDocument(key, value);
    }

    public static SerializableJsonDocument newDocument(String key, Boolean value) {
        return new SerializableJsonDocument(key, value);
    }

    public static SerializableJsonDocument newDocument(String key, Object value) {
        return new SerializableJsonDocument(key, value);
    }


    public static SerializableJsonDocument newDocument(byte[] bytes) {
        return newDocument(new String(bytes, StandardCharsets.UTF_8));
    }

    public static SerializableJsonDocument newDocument(Object object) {
        return new SerializableJsonDocument(object);
    }

    public static SerializableJsonDocument newDocument(File file) {
        if (file == null) {
            return null;
        }

        return newDocument(file.toPath());
    }

    public static SerializableJsonDocument newDocument(Path path) {
        SerializableJsonDocument document = new SerializableJsonDocument();
        document.read(path);
        return document;
    }

    public static SerializableJsonDocument newDocument(String input) {
        SerializableJsonDocument document = new SerializableJsonDocument();
        document.read(input);
        return document;
    }

    public static SerializableJsonDocument asSerializable(JsonDocument document) {
        return document instanceof SerializableJsonDocument ? (SerializableJsonDocument) document : new SerializableJsonDocument(document);
    }

    @Override
    public JsonDocument clone() {
        return new SerializableJsonDocument(super.clone());
    }

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeString(super.toJson());
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        super.clear();
        super.append(JsonParser.parseString(buffer.readString()).getAsJsonObject());
    }
}
