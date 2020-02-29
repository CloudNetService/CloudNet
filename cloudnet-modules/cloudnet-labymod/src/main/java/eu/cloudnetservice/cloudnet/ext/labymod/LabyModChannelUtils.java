package eu.cloudnetservice.cloudnet.ext.labymod;

import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

public class LabyModChannelUtils {

    private LabyModChannelUtils() {
        throw new UnsupportedOperationException();
    }

    public static byte[] getLMCMessageContents(String messageKey, JsonDocument messageContents) {
        ByteBuf byteBuf = Unpooled.buffer();
        writeString(byteBuf, messageKey);
        writeString(byteBuf, messageContents.toJson());

        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);

        return bytes;
    }

    public static Pair<String, JsonDocument> readLMCMessageContents(byte[] data) {
        ByteBuf byteBuf = Unpooled.wrappedBuffer(data);

        String messageKey = readString(byteBuf);
        String messageContents = readString(byteBuf);
        JsonDocument document = JsonDocument.newDocument(messageContents);

        return new Pair<>(messageKey, document);
    }

    public static int readVarInt(ByteBuf byteBuf) {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = byteBuf.readByte();
            int value = (read & 0b01111111);
            result |= (value << (7 * numRead));

            numRead++;
            if (numRead > 5) {
                throw new RuntimeException("VarInt is too big");
            }
        } while ((read & 0b10000000) != 0);

        return result;
    }

    public static ByteBuf writeVarInt(ByteBuf byteBuf, int value) {
        do {
            byte temp = (byte) (value & 0b01111111);
            value >>>= 7;
            if (value != 0) {
                temp |= 0b10000000;
            }
            byteBuf.writeByte(temp);
        } while (value != 0);

        return byteBuf;
    }

    public static ByteBuf writeString(ByteBuf byteBuf, String string) {
        byte[] values = string.getBytes(StandardCharsets.UTF_8);
        writeVarInt(byteBuf, values.length);
        byteBuf.writeBytes(values);
        return byteBuf;
    }

    public static String readString(ByteBuf byteBuf) {
        int integer = readVarInt(byteBuf);
        byte[] buffer = new byte[integer];
        byteBuf.readBytes(buffer, 0, integer);

        return new String(buffer, StandardCharsets.UTF_8);
    }

}
