/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.driver.network.protocol;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.protocol.chunk.ChunkedPacketBuilder;
import de.dytanic.cloudnet.driver.network.protocol.chunk.listener.ChunkedPacketListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

public class ChunkedPacketTest {

  @Test
  public void testChunkedPacket() throws IOException {

    Path input = Paths.get("build/chunked_packet");
    Path output = Paths.get("build/chunked_packet_result");

    Files.deleteIfExists(input);
    Files.deleteIfExists(output);

    try (OutputStream outputStream = Files.newOutputStream(input)) {
      Random random = new Random();
      for (int i = 0; i < 256; i++) {
        byte[] data = new byte[1024 * 1024];
        random.nextBytes(data);

        outputStream.write(data);
      }
    }

    TestChunkedPacketListener listener = new TestChunkedPacketListener(output);
    JsonDocument header = JsonDocument.newDocument("test", "test");

    Assert.assertEquals(0, listener.getSessions().size());

    try (InputStream inputStream = Files.newInputStream(input)) {
      ChunkedPacketBuilder.newBuilder(1, inputStream).header(header).target(packet -> {
        try {
          listener.handle(null, packet.fillBuffer());
        } catch (Exception exception) {
          exception.printStackTrace();
        }
      }).complete();
    }

    Assert.assertEquals(0, listener.getSessions().size());

    Assert.assertTrue(Files.exists(input));
    Assert.assertTrue(Files.exists(output));

    try (InputStream expected = Files.newInputStream(input);
      InputStream actual = Files.newInputStream(output)) {
      long index = 0;
      int in;
      while ((in = expected.read()) > 0) {
        Assert.assertEquals("Wrong byte at index " + (index++), in, actual.read());
      }
    }

    Assert.assertEquals(-1, this.getUnequalIndex(input, output));

    Files.delete(input);
    Files.delete(output);

    Assert.assertFalse(Files.exists(input));
    Assert.assertFalse(Files.exists(output));
  }

  private long getUnequalIndex(Path expected, Path actual) throws IOException {
    try (InputStream expectedStream = Files.newInputStream(expected);
      InputStream actualStream = Files.newInputStream(actual)) {
      long index = 0;
      int expectedByte;
      while ((expectedByte = expectedStream.read()) > 0) {
        if (expectedByte != actualStream.read()) {
          return index;
        }
        ++index;
      }
    }
    return -1;
  }

  private static class TestChunkedPacketListener extends ChunkedPacketListener {

    private final Path path;

    public TestChunkedPacketListener(Path path) {
      this.path = path;
    }

    @Override
    protected @NotNull OutputStream createOutputStream(@NotNull UUID sessionUniqueId,
      @NotNull Map<String, Object> properties) throws IOException {
      return Files.newOutputStream(this.path);
    }
  }

}
