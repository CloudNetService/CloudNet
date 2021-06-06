package de.dytanic.cloudnet.driver.serialization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import java.io.IOException;
import org.junit.Test;

public class ProtocolBufferTest {

  @Test
  public void testProtocolBuffer() {
    ProtocolBuffer out = ProtocolBuffer.create();

    out.writeString("test");
    out.writeOptionalString(null);
    out.writeOptionalString("asdf");

    out.writeInt(1234);

    out.writeBoolean(false);
    out.writeBoolean(true);

    out.writeJsonDocument(JsonDocument.newDocument("test", "test"));
    out.writeOptionalJsonDocument(null);

    out.writeLong(Long.MAX_VALUE);
    out.writeLong(Long.MIN_VALUE);

    out.writeByte(-1);
    out.writeByte(255);

    out.writeThrowable(new IOException("test message"));

    ProtocolBuffer in = ProtocolBuffer.wrap(out.toArray());

    assertEquals("test", in.readString());
    assertNull(in.readOptionalString());
    assertEquals("asdf", in.readOptionalString());

    assertEquals(1234, in.readInt());

    assertFalse(in.readBoolean());
    assertTrue(in.readBoolean());

    assertEquals("test", in.readJsonDocument().getString("test"));
    assertNull(in.readOptionalJsonDocument());

    assertEquals(Long.MAX_VALUE, in.readLong());
    assertEquals(Long.MIN_VALUE, in.readLong());

    assertEquals(-1, in.readByte());
    assertEquals(255, in.readUnsignedByte());

    Throwable throwable = in.readThrowable();
    assertTrue(throwable instanceof IOException);
    assertEquals("test message", throwable.getMessage());

  }

}
