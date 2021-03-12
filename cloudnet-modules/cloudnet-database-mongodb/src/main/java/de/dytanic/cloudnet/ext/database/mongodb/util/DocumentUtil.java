package de.dytanic.cloudnet.ext.database.mongodb.util;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import org.bson.Document;

public class DocumentUtil {
    public static Document toBson(JsonDocument jsonDocument) {
        return Document.parse(jsonDocument.toJson());
    }

    public static JsonDocument toJsonDocument(Document document) {
        return JsonDocument.newDocument(document.toJson());
    }
}
