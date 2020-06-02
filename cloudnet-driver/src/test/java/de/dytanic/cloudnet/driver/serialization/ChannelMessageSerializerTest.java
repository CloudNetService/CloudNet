package de.dytanic.cloudnet.driver.serialization;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.DriverEnvironment;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.channel.ChannelMessageSender;
import de.dytanic.cloudnet.driver.channel.ChannelMessageTarget;
import org.junit.Assert;
import org.junit.Test;

public class ChannelMessageSerializerTest {

    @Test
    public void serializeChannelMessage() {
        ChannelMessage original = ChannelMessage.builder(new ChannelMessageSender("Test", DriverEnvironment.CLOUDNET))
                .channel("test-channel")
                .message("test-message")
                .byteContent(new byte[]{1, 2, 3, 4, 5})
                .jsonContent(JsonDocument.newDocument("1", "2"))
                .targetAll(ChannelMessageTarget.Type.NODE)
                .build();

        ProtocolBuffer buffer = ProtocolBuffer.create();
        buffer.writeObject(original);

        ChannelMessage deserialized = buffer.readObject(ChannelMessage.class);

        Assert.assertEquals(original, deserialized);
    }

}
