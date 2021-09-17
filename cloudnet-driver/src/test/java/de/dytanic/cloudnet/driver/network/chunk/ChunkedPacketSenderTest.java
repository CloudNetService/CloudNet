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

package de.dytanic.cloudnet.driver.network.chunk;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.chunk.defaults.splitter.NetworkChannelsPacketSplitter;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

@TestMethodOrder(OrderAnnotation.class)
public class ChunkedPacketSenderTest {

  @Test
  @Order(0)
  @Timeout(20)
  void testChunkPacketSender() throws Exception {
    AtomicInteger packetSplits = new AtomicInteger();
    byte[] chunkData = this.generateRandomChunkData();

    UUID sessionId = UUID.randomUUID();
    JsonDocument jsonData = JsonDocument.newDocument("test", "1234");

    Assertions.assertEquals(TransferStatus.SUCCESS, ChunkedPacketSender.forFileTransfer()
      .chunkSize(256)
      .transferMode(45)
      .withExtraData(jsonData)
      .sessionUniqueId(sessionId)
      .source(new ByteArrayInputStream(chunkData))
      .packetSplitter(packet -> {
        this.validatePacket(packet, sessionId, jsonData, packetSplits, chunkData);
        packetSplits.incrementAndGet();
      })
      .build()
      .transferChunkedData()
      .get());
  }

  @Test
  @Order(10)
  @Timeout(20)
  void testNetworkChannelSplitter() throws Exception {
    AtomicInteger packetSplits = new AtomicInteger();
    byte[] chunkData = this.generateRandomChunkData();

    UUID sessionId = UUID.randomUUID();
    JsonDocument jsonData = JsonDocument.newDocument("test", "1234");

    NetworkChannelsPacketSplitter splitter = new NetworkChannelsPacketSplitter(IntStream.range(0, 10)
      .mapToObj($ -> this.mockNetworkChannel(packet ->
        this.validatePacket(packet, sessionId, jsonData, packetSplits, chunkData)))
      .collect(Collectors.toList()));

    Assertions.assertEquals(TransferStatus.SUCCESS, ChunkedPacketSender.forFileTransfer()
      .chunkSize(256)
      .transferMode(45)
      .withExtraData(jsonData)
      .sessionUniqueId(sessionId)
      .source(new ByteArrayInputStream(chunkData))
      .packetSplitter(packet -> {
        splitter.accept(packet);
        packetSplits.incrementAndGet();
      })
      .build()
      .transferChunkedData()
      .get());
  }

  private byte[] generateRandomChunkData() {
    byte[] data = new byte[4096];
    ThreadLocalRandom.current().nextBytes(data);
    return data;
  }

  private void validatePacket(IPacket packet, UUID sessionId, JsonDocument extra, AtomicInteger splits, byte[] data) {
    Assertions.assertEquals(256, packet.getContent().readInt());
    Assertions.assertEquals(45, packet.getContent().readInt());
    Assertions.assertEquals(sessionId, packet.getContent().readUniqueId());
    Assertions.assertArrayEquals(extra.toByteArray(), packet.getContent().readByteArray());
    Assertions.assertEquals(splits.get(), packet.getContent().readInt());

    boolean isFinalPacket = packet.getContent().readBoolean();
    Assertions.assertEquals(splits.get() == data.length / 256, isFinalPacket);

    if (isFinalPacket) {
      Assertions.assertEquals(data.length / 256, packet.getContent().readInt());
    }

    // this prevents a weird bug happening. When copying an array beginning at the length of the array (in this case
    // 4096) it will not throw an exception as expected but give you back an array with the expected 256 size full
    // of zeros which will cause this test to fail.
    int sourcePosition = splits.get() * 256;
    byte[] contentAtPosition = sourcePosition == data.length
      ? new byte[0]
      : Arrays.copyOfRange(data, sourcePosition, (splits.get() + 1) * 256);

    Assertions.assertArrayEquals(
      contentAtPosition,
      packet.getContent().readByteArray()
    );
  }

  private INetworkChannel mockNetworkChannel(Consumer<IPacket> packetSyncSendHandler) {
    INetworkChannel channel = Mockito.mock(INetworkChannel.class);
    Mockito
      .doAnswer(invocation -> {
        packetSyncSendHandler.accept(invocation.getArgument(0));
        return null;
      })
      .when(channel)
      .sendPacketSync(Mockito.any(IPacket.class));

    return channel;
  }
}
