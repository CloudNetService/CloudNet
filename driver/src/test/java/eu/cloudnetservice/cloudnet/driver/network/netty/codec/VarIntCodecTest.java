/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.driver.network.netty.codec;

import eu.cloudnetservice.cloudnet.driver.network.netty.NettyUtil;
import eu.cloudnetservice.cloudnet.driver.network.netty.codec.NettyPacketLengthDeserializer.ProcessingResult;
import eu.cloudnetservice.cloudnet.driver.network.netty.codec.NettyPacketLengthDeserializer.VarIntByteProcessor;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VarIntCodecTest {

  @Test
  void testNettyUtilVarIntWriteRead() {
    var random = new Random();
    var buffer = NettyUtil.allocator().allocate(5 + Long.BYTES);

    for (int curr = 1; curr < 5_000_000; curr += 31) {
      // write an extra long to try trick the deserializer
      NettyUtil.writeVarInt(buffer, curr);
      buffer.writeLong(random.nextLong());

      // read
      Assertions.assertEquals(curr, NettyUtil.readVarInt(buffer));

      // reset
      buffer.resetOffsets();
    }
  }

  @Test
  void testVarIntByteDecoderWriteRead() {
    var random = new Random();
    var processor = new VarIntByteProcessor();
    var buffer = NettyUtil.allocator().allocate(5 + Long.BYTES);

    for (int curr = 1; curr < 5_000_000; curr += 31) {
      // write an extra long to try trick the deserializer
      NettyUtil.writeVarInt(buffer, curr);
      buffer.writeLong(random.nextLong());

      // read
      buffer.openCursor().process(processor);
      Assertions.assertEquals(curr, processor.varInt);
      Assertions.assertEquals(ProcessingResult.OK, processor.result);

      // reset
      buffer.resetOffsets();
      processor.varInt = 0;
      processor.bytesRead = 0;
      processor.result = ProcessingResult.TOO_SHORT;
    }
  }
}
