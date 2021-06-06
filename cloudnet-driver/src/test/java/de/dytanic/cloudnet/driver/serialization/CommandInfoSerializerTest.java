package de.dytanic.cloudnet.driver.serialization;

import de.dytanic.cloudnet.driver.command.CommandInfo;
import org.junit.Assert;
import org.junit.Test;

public class CommandInfoSerializerTest {

  @Test
  public void serializeCommandInfo() {
    CommandInfo original = new CommandInfo(new String[]{"help", "?"}, null, "desc", "any usage");

    ProtocolBuffer buffer = ProtocolBuffer.create();
    buffer.writeObject(original);

    CommandInfo deserialized = buffer.readObject(CommandInfo.class);

    Assert.assertEquals(original, deserialized);
  }

}
