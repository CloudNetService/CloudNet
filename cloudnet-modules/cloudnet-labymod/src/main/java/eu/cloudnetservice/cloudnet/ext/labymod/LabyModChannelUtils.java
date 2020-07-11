package eu.cloudnetservice.cloudnet.ext.labymod;

import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;

public class LabyModChannelUtils {

    private LabyModChannelUtils() {
        throw new UnsupportedOperationException();
    }

    public static byte[] getLMCMessageContents(String messageKey, JsonDocument messageContents) {
        return ProtocolBuffer.create()
                .writeString(messageKey)
                .writeString(messageContents.toJson())
                .toArray();
    }

    public static Pair<String, JsonDocument> readLMCMessageContents(byte[] data) {
        ProtocolBuffer buffer = ProtocolBuffer.wrap(data);

        String messageKey = buffer.readString();
        String messageContents = buffer.readString();
        JsonDocument document = JsonDocument.newDocument(messageContents);

        return new Pair<>(messageKey, document);
    }

}
