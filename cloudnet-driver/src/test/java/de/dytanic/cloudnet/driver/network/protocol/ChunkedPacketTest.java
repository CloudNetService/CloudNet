package de.dytanic.cloudnet.driver.network.protocol;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.protocol.chunk.ChunkedPacket;
import de.dytanic.cloudnet.driver.network.protocol.chunk.ChunkedPacketListener;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class ChunkedPacketTest {

    @Test
    public void testChunkedPacket() throws IOException {

        Path input = Paths.get("build/chunked_packet");
        Path output = Paths.get("build/chunked_packet_result");

        Files.deleteIfExists(input);
        Files.deleteIfExists(output);

        try (OutputStream outputStream = Files.newOutputStream(input)) {
            for (int i = 0; i < 256; i++) {
                Random random = new Random();
                byte[] data = new byte[1024 * 1024];
                random.nextBytes(data);

                outputStream.write(data);
            }
        }

        TestChunkedPacketListener listener = new TestChunkedPacketListener(output);
        JsonDocument header = JsonDocument.newDocument("test", "test");

        Assert.assertEquals(0, listener.getSessions().size());

        try (InputStream inputStream = Files.newInputStream(input)) {
            ChunkedPacket.createChunkedPackets(inputStream, header, 1, packet -> {
                try {
                    listener.handle(null, packet);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            });
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
        protected @NotNull OutputStream createOutputStream(ChunkedPacket startPacket) throws IOException {
            return Files.newOutputStream(path);
        }

    }

}
