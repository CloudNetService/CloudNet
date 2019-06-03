package de.dytanic.cloudnet.common.gson;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import org.junit.Assert;
import org.junit.Test;

public final class GsonUtilTest {

  @Test
  public void testGsonConstants() {
    Assert.assertEquals("{\"value\":false}",
        GsonUtil.GSON.toJson(new JsonDocument("value", false)));
  }
}