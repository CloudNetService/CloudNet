/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.impl.network.chunk;

import eu.cloudnetservice.driver.impl.junit.EnableServicesInject;
import eu.cloudnetservice.driver.impl.network.chunk.splitter.NetworkChannelsPacketSplitter;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.chunk.ChunkSessionInformation;
import eu.cloudnetservice.driver.network.chunk.ChunkedPacketSender;
import eu.cloudnetservice.driver.network.chunk.TransferStatus;
import eu.cloudnetservice.driver.network.protocol.Packet;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

@EnableServicesInject
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ChunkedPacketSenderTest {

  @Test
  @Order(0)
  @Timeout(20)
  void testChunkPacketSender() throws Exception {
    var packetSplits = new AtomicInteger();
    var chunkData = this.generateRandomChunkData();

    var sessionId = UUID.randomUUID();
    DataBuf dataBuf = DataBuf.empty().writeString("hello").writeInt(10).writeString("world");

    Assertions.assertEquals(TransferStatus.SUCCESS, ChunkedPacketSender.forStreamTransfer()
      .chunkSize(256)
      .withExtraData(dataBuf)
      .sessionUniqueId(sessionId)
      .transferChannel("hello_world")
      .source(new ByteArrayInputStream(chunkData))
      .packetSplitter(packet -> {
        this.validatePacket(packet, sessionId, packetSplits, chunkData);
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
    var packetSplits = new AtomicInteger();
    var chunkData = this.generateRandomChunkData();

    var sessionId = UUID.randomUUID();
    DataBuf dataBuf = DataBuf.empty().writeString("hello").writeInt(10).writeString("world");

    var splitter = new NetworkChannelsPacketSplitter(IntStream.range(0, 10)
      .mapToObj(_ -> this.mockNetworkChannel(packet -> this.validatePacket(packet, sessionId, packetSplits, chunkData)))
      .collect(Collectors.toList()));

    Assertions.assertEquals(TransferStatus.SUCCESS, ChunkedPacketSender.forStreamTransfer()
      .chunkSize(256)
      .withExtraData(dataBuf)
      .sessionUniqueId(sessionId)
      .transferChannel("hello_world")
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
    var data = new byte[4096];
    ThreadLocalRandom.current().nextBytes(data);
    return data;
  }

  private void validatePacket(Packet packet, UUID sessionId, AtomicInteger splits, byte[] data) {
    var info = packet.content().readObject(ChunkSessionInformation.class);
    var chunkIndex = packet.content().readInt();
    var finalChunk = packet.content().readBoolean();

    Assertions.assertEquals(256, info.chunkSize());
    Assertions.assertEquals(sessionId, info.sessionUniqueId());
    Assertions.assertEquals("hello_world", info.transferChannel());
    Assertions.assertEquals(splits.get(), chunkIndex);

    Assertions.assertEquals("hello", info.transferInformation().readString());
    Assertions.assertEquals(10, info.transferInformation().readInt());
    Assertions.assertEquals("world", info.transferInformation().readString());

    Assertions.assertEquals(splits.get() == data.length / 256, finalChunk);
    if (finalChunk) {
      Assertions.assertEquals(data.length / 256, chunkIndex);
    }

    // this prevents a weird bug happening. When copying an array beginning at the length of the array (in this case
    // 4096) it will not throw an exception as expected but give you back an array with the expected 256 size full
    // of zeros which will cause this test to fail.
    var sourcePosition = splits.get() * 256;
    var contentAtPosition = sourcePosition == data.length
      ? new byte[0]
      : Arrays.copyOfRange(data, sourcePosition, (splits.get() + 1) * 256);

    Assertions.assertArrayEquals(
      contentAtPosition,
      packet.content().readByteArray());
  }

  private NetworkChannel mockNetworkChannel(Consumer<Packet> packetSyncSendHandler) {
    var channel = Mockito.mock(NetworkChannel.class);
    Mockito
      .doAnswer(invocation -> {
        packetSyncSendHandler.accept(invocation.getArgument(0));
        return null;
      })
      .when(channel)
      .sendPacketSync(Mockito.any(Packet.class));

    return channel;
  }
}
