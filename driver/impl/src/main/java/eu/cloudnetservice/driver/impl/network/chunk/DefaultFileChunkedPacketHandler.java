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

import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.chunk.ChunkSessionInformation;
import eu.cloudnetservice.driver.network.chunk.ChunkedPacketHandler;
import eu.cloudnetservice.driver.network.chunk.TransferStatus;
import eu.cloudnetservice.utils.base.io.FileUtil;
import java.io.IOException;
import java.io.InputStream;
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
  protected final Lock lock = new ReentrantLock();

  protected int writtenFileParts = -1;
  protected int expectedFileParts = -1;

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

    this.tempFilePath = tempFilePath;
    this.writeCompleteHandler = completeHandler;
    this.targetFile = this.openTempFile();
  }

  /**
   * Opens a random access file at the provided temp path, creating the file if it does not exist. Note that this method
   * does not create the parent directory of the file, it must exist prior to invocation.
   *
   * @return the opened temp file for random access.
   * @throws IllegalStateException if the temp file cannot be opened or created.
   */
  private @NonNull RandomAccessFile openTempFile() {
    try {
      // create the file in case it does not exist
      if (Files.notExists(this.tempFilePath)) {
        Files.createFile(this.tempFilePath);
      }

      var pathAsFile = this.tempFilePath.toFile();
      return new RandomAccessFile(pathAsFile, "rwd");
    } catch (IOException exception) {
      throw new IllegalStateException("cannot open chunk transfer temp file for writing", exception);
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

    // check if the given chunk is the last chunk in the transfer, set the amount of chunks to expect
    // this is used in case not all file parts were received when the final packet arrives
    var isFinalPacket = dataBuf.readBoolean();
    if (isFinalPacket) {
      this.expectedFileParts = chunkPosition;
    }

    this.lock.lock();
    try {
      // check if the data transfer is still running
      if (this.transferStatus != TransferStatus.RUNNING) {
        throw new IllegalStateException("chunked transfer received data after completion");
      }

      // write the packet content to disk
      this.writePacketContent(chunkPosition, dataBuf);
      this.writtenFileParts++;

      // clean up in case the last chunk was just received
      if (this.expectedFileParts != -1 && this.expectedFileParts == this.writtenFileParts) {
        this.transferStatus = TransferStatus.SUCCESS;
        this.targetFile.close();

        // call the write completion handler, if present
        if (this.writeCompleteHandler != null) {
          var closeStream = true;
          var stream = InputStream.nullInputStream();
          try {
            // open the stream to the data and post it to the write handler
            stream = Files.newInputStream(this.tempFilePath, StandardOpenOption.DELETE_ON_CLOSE);
            closeStream = this.writeCompleteHandler.handleSessionComplete(this.chunkSessionInformation, stream);
          } finally {
            if (closeStream) {
              stream.close();
            }
          }
        }

        return true;
      }

      // not the last chunk, continue processing
      return false;
    } catch (IOException exception) {
      this.transferStatus = TransferStatus.FAILURE;
      throw new IllegalStateException("Unexpected exception handling chunk part", exception);
    } finally {
      this.lock.unlock();
    }
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
    var filePosition = Math.multiplyFull(chunkPosition, this.chunkSessionInformation.chunkSize());
    this.targetFile.seek(filePosition);
    this.targetFile.write(dataBuf.readByteArray());
  }
}
