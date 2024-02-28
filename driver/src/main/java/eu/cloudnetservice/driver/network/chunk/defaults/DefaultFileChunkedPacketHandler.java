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

package eu.cloudnetservice.driver.network.chunk.defaults;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.chunk.ChunkedPacketHandler;
import eu.cloudnetservice.driver.network.chunk.TransferStatus;
import eu.cloudnetservice.driver.network.chunk.data.ChunkSessionInformation;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a handler for a chunked packet transfer which transfers a file.
 *
 * @since 4.0
 */
public class DefaultFileChunkedPacketHandler extends DefaultChunkedPacketProvider implements ChunkedPacketHandler {

  protected final Path tempFilePath;
  protected final RandomAccessFile targetFile;
  protected final Callback writeCompleteHandler;
  protected final Lock lock = new ReentrantLock(true);

  protected int writtenFileParts = -1;
  protected Integer expectedFileParts;

  /**
   * Creates the session handler initially. Sessions should be manged by some sort of handler which is responsible for
   * handling incoming chunk parts as well.
   *
   * @param sessionInformation the information transferred by the sender initially.
   * @param completeHandler    the handler to call when the file transfer finished successfully.
   * @throws NullPointerException if the given session information is null.
   */
  public DefaultFileChunkedPacketHandler(
    @NonNull ChunkSessionInformation sessionInformation,
    @Nullable Callback completeHandler
  ) {
    this(sessionInformation, completeHandler, FileUtil.createTempFile());
  }

  /**
   * Creates the session handler initially. Sessions should be manged by some sort of handler which is responsible for
   * handling incoming chunk parts as well.
   *
   * @param sessionInformation the information transferred by the sender initially.
   * @param completeHandler    the handler to call when the file transfer finished successfully.
   * @param tempFilePath       the path to the temp file to write the received data to.
   * @throws NullPointerException if the given session information or temp path is null.
   */
  public DefaultFileChunkedPacketHandler(
    @NonNull ChunkSessionInformation sessionInformation,
    @Nullable Callback completeHandler,
    @NonNull Path tempFilePath
  ) {
    super(sessionInformation);

    // general information
    this.tempFilePath = tempFilePath;
    this.writeCompleteHandler = completeHandler;
    // open the temp file raf access
    try {
      // create the file
      if (Files.notExists(tempFilePath)) {
        Files.createFile(tempFilePath);
      }
      // open the file
      this.targetFile = new RandomAccessFile(this.tempFilePath.toFile(), "rwd");
    } catch (IOException exception) {
      throw new AssertionError("Unable to open raf to temp file, this should not happen", exception);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean handleChunkPart(int chunkPosition, @NonNull DataBuf dataBuf) {
    // if the handling failed before we skip the handling of the packet
    if (this.transferStatus == TransferStatus.FAILURE) {
      return false;
    }
    // validate that this is still in the running state when receiving the packet
    Preconditions.checkState(this.transferStatus == TransferStatus.RUNNING, "Received transfer part after success");
    // extract some information from the body
    var isFinalPacket = dataBuf.readBoolean();
    if (isFinalPacket) {
      this.expectedFileParts = dataBuf.readInt();
    }
    // execute the write operation with the content of the packet
    try {
      // we can only perform one write operation at a time
      this.lock.lock();
      // execute
      this.writePacketContent(chunkPosition, dataBuf);
      // update the data transfer status
      this.updateStatus();
      // check if the expected ending is reached
      if (this.transferStatus == TransferStatus.SUCCESS) {
        // the file was written completely
        this.targetFile.close();
        // post the result to the complete handler
        if (this.writeCompleteHandler == null) {
          // no handler - will be handled otherwise
          return true;
        }
        // delete the file after posting
        try (var inputStream = Files.newInputStream(this.tempFilePath, StandardOpenOption.DELETE_ON_CLOSE)) {
          this.writeCompleteHandler.handleSessionComplete(this.chunkSessionInformation, inputStream);
          return true;
        }
      }
      // not completed yet
      return false;
    } catch (IOException exception) {
      this.transferStatus = TransferStatus.FAILURE;
      throw new IllegalStateException("Unexpected exception handling chunk part", exception);
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Callback callback() {
    return this.writeCompleteHandler;
  }

  /**
   * Writes the content of a chunk part to the backing file, increasing the amount of written bytes by one.
   *
   * @param chunkPosition the index of the chunk to write.
   * @param dataBuf       the buf transferred to this handler, the next content should be the actual chunk data.
   * @throws IOException          if an i/o error occurs during the chunk write.
   * @throws NullPointerException if the given buffer is null.
   */
  protected void writePacketContent(int chunkPosition, @NonNull DataBuf dataBuf) throws IOException {
    // calculate the index of to which we need to sink in order to write
    var targetIndex = chunkPosition * this.chunkSessionInformation.chunkSize();
    // sink to the index of the chunk position we need to write to
    this.targetFile.seek(targetIndex);
    // write the content into the file at the current offset we sunk to
    this.targetFile.write(dataBuf.readByteArray());
    // notify our index about the write operation
    this.writtenFileParts++;
  }

  /**
   * Updates the status of the file transfer. This method will set the status to completed when:
   * <ol>
   *   <li>The current status is {@code RUNNING}.
   *   <li>The amount of chunk parts of the transfer is known.
   *   <li>The amount of written chunk parts matches the amount of expected chunk parts.
   * </ol>
   */
  protected void updateStatus() {
    // we only need to update the status when the transfer is running but the whole content was written
    if (this.transferStatus == TransferStatus.RUNNING
      && this.expectedFileParts != null
      && this.expectedFileParts == this.writtenFileParts
    ) {
      this.transferStatus = TransferStatus.SUCCESS;
    }
  }
}
