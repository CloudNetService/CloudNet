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

import eu.cloudnetservice.cloudnet.driver.network.exception.SilentDecoderException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ByteProcessor;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * An adapted version of the MinecraftVarintFrameDecoder in the PaperMC/Velocity project. This processor works the same
 * way as the velocity version, but has more comments what it's actually doing and is simplified where needed for our
 * use case.
 *
 * @since 4.0
 */
@Internal
public final class NettyPacketLengthDeserializer extends ByteToMessageDecoder {

  private static final SilentDecoderException BAD_LENGTH = new SilentDecoderException("Bad packet length");
  private static final SilentDecoderException INVALID_VAR_INT = new SilentDecoderException("Invalid decoder var int");

  /**
   * {@inheritDoc}
   */
  @Override
  protected void decode(@NonNull ChannelHandlerContext ctx, @NonNull ByteBuf in, @NonNull List<Object> out) {
    // ensure that the channel we're reading from is still open
    if (!ctx.channel().isActive()) {
      in.clear();
      return;
    }

    // try to read the var int from the input buffer
    var processor = new VarIntByteProcessor();
    var varIntByteEnding = in.forEachByte(processor);

    // sanely handle the result of the decoding process
    // -1 indicates us that an overflow happened (we've tried to iterate beyond the ending of the buffer)
    if (varIntByteEnding == -1) {
      // skip packets which are just zeroes, do not clear the buffer elsewhere as we want to continue reading
      if (processor.result == ProcessingResult.ZERO) {
        in.clear();
      }
      return;
    }

    // if the buffer int only persists of zeroes there is a chance that the next var int starts and the end of the
    // current read result, continue there
    if (processor.result == ProcessingResult.ZERO) {
      in.readerIndex(varIntByteEnding);
      return;
    }

    // best case scenario - the read succeeded, validate the submitted data anyway.
    if (processor.result == ProcessingResult.OK) {
      var varInt = processor.varInt;
      var byteAmount = processor.bytesRead;

      // the length of the packet might not be less than 0, hard fail there
      if (varInt < 0) {
        in.clear();
        throw BAD_LENGTH;
      }

      // skip empty packets silently
      if (varInt == 0) {
        in.readerIndex(varIntByteEnding + 1);
        return;
      }

      // check if the packet data supplied in the buffer is actually at least the transmitted size
      var minBytes = byteAmount + varInt;
      if (in.isReadable(minBytes)) {
        out.add(in.retainedSlice(varIntByteEnding + 1, varInt));
        in.skipBytes(minBytes);
      }

      // stop here
      return;
    }

    // an invalid (too large) var int was supplied, hard stop here
    if (processor.result == ProcessingResult.TOO_BIG) {
      in.clear();
      throw INVALID_VAR_INT;
    }
  }

  /**
   * The result of a var int processing.
   *
   * @since 4.0
   */
  private enum ProcessingResult {

    /**
     * The var int was processed successfully.
     */
    OK,
    /**
     * The var int only contains zeroes.
     */
    ZERO,
    /**
     * The var int sent to the server is longer than 5 bytes.
     */
    TOO_BIG,
    /**
     * No var int was read from the buffer, probably too short.
     */
    TOO_SHORT
  }

  /**
   * An implementation of a byte processing specially for reading 5 bytes from the buffer (maximum length of a var int)
   * and decoding it.
   *
   * @since 4.0
   */
  private static final class VarIntByteProcessor implements ByteProcessor {

    private int varInt;
    private int bytesRead;
    private ProcessingResult result;

    /**
     * Constructs a new var int processor instance, setting the initial result to TOO_SHORT.
     */
    public VarIntByteProcessor() {
      this.result = ProcessingResult.TOO_SHORT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean process(byte value) throws Exception {
      // check if the current byte is 0. If so, and we've read no byte before this means either that the encoded var int
      // is encoded weirdly or that the next coming bytes are only zeroes. Continue anyway as there might be a chance that
      // there are still valid bytes following
      if (value == 0 && this.bytesRead == 0) {
        this.result = ProcessingResult.ZERO;
        return true;
      }

      // skip zero buffer processing
      if (this.result == ProcessingResult.ZERO) {
        return false;
      }

      // actually read and append the next byte of the var int to the buffer, validate that we are not reading too much.
      this.varInt |= (value & 0x7F) << this.bytesRead++ * 7;
      if (this.bytesRead > 5) {
        this.result = ProcessingResult.TOO_BIG;
        return false;
      }

      // indication that the end of the var int has been reached, that read was successful.
      if ((value & 0x80) != 128) {
        this.result = ProcessingResult.OK;
        return false;
      }

      // continue reading
      return true;
    }
  }
}
