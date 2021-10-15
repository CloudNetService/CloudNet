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

package de.dytanic.cloudnet.driver.network.netty.codec;

import de.dytanic.cloudnet.driver.network.exception.SilentDecoderException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ByteProcessor;
import java.util.List;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class NettyPacketLengthDeserializer extends ByteToMessageDecoder {

  private static final SilentDecoderException BAD_LENGTH = new SilentDecoderException("Bad packet length");
  private static final SilentDecoderException INVALID_VAR_INT = new SilentDecoderException("Invalid var int");

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    if (!ctx.channel().isActive()) {
      in.clear();
      return;
    }

    VarIntByteProcessor processor = new VarIntByteProcessor();
    int varIntByteEnding = in.forEachByte(processor);

    if (processor.result != VarIntByteProcessor.ProcessingResult.OK) {
      throw INVALID_VAR_INT;
    } else {
      int varInt = processor.varInt;
      int bytesRead = processor.bytesRead;

      if (varInt < 0) {
        in.clear();
        throw BAD_LENGTH;
      } else if (varInt == 0) {
        // empty packet, ignore it
        in.readerIndex(varIntByteEnding + 1);
      } else {
        int minimumReadableBytes = varInt + bytesRead;
        if (in.isReadable(minimumReadableBytes)) {
          out.add(in.retainedSlice(varIntByteEnding + 1, varInt));
          in.skipBytes(minimumReadableBytes);
        }
      }
    }
  }

  private static final class VarIntByteProcessor implements ByteProcessor {

    private int varInt;
    private int bytesRead;
    private ProcessingResult result;

    public VarIntByteProcessor() {
      this.result = ProcessingResult.TOO_SHORT;
    }

    @Override
    public boolean process(byte value) throws Exception {
      this.varInt |= (value & 0x7F) << this.bytesRead++ * 7;
      if (this.bytesRead > 5) {
        this.result = ProcessingResult.TOO_BIG;
        return false;
      } else if ((value & 0x80) != 128) {
        this.result = ProcessingResult.OK;
        return false;
      } else {
        return true;
      }
    }

    private enum ProcessingResult {

      OK,
      TOO_SHORT,
      TOO_BIG
    }
  }
}
