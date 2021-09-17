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

package de.dytanic.cloudnet.driver.network.chunk.defaults;

import com.google.common.base.Verify;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.chunk.ChunkedPacketHandler;
import de.dytanic.cloudnet.driver.network.chunk.TransferStatus;
import de.dytanic.cloudnet.driver.network.chunk.data.ChunkSessionInformation;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jetbrains.annotations.NotNull;

public class DefaultFileChunkedPacketHandler extends DefaultChunkedPacketProvider implements ChunkedPacketHandler {

  protected final Path tempFilePath;
  protected final RandomAccessFile targetFile;
  protected final Callback writeCompleteHandler;
  protected final Lock lock = new ReentrantLock(true);
  protected final AtomicInteger writtenFileParts = new AtomicInteger();

  protected Integer expectedFileParts;

  protected DefaultFileChunkedPacketHandler(
    @NotNull ChunkSessionInformation sessionInformation,
    @NotNull Callback completeHandler
  ) {
    super(sessionInformation);

    // general information
    this.writeCompleteHandler = completeHandler;
    this.tempFilePath = FileUtils.createTempFile();
    // open the temp file raf access
    try {
      this.targetFile = new RandomAccessFile(this.tempFilePath.toFile(), "rwd");
    } catch (FileNotFoundException exception) {
      throw new AssertionError("Unable to open raf to temp file, this should not happen", exception);
    }
  }

  @Override
  public boolean handleChunkPart(int chunkPosition, @NotNull DataBuf dataBuf) {
    // if the handling failed before we skip the handling of the packet
    if (this.transferStatus == TransferStatus.FAILURE) {
      return false;
    }
    // validate that this is still in the running state when receiving the packet
    Verify.verify(this.transferStatus == TransferStatus.RUNNING, "Received transfer part after success");
    // extract some information from the body
    boolean isFinalPacket = dataBuf.readBoolean();
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
        try (InputStream inputStream = Files.newInputStream(this.tempFilePath, StandardOpenOption.DELETE_ON_CLOSE)) {
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

  @Override
  public @NotNull Callback getCallback() {
    return this.writeCompleteHandler;
  }

  protected void writePacketContent(int chunkPosition, @NotNull DataBuf dataBuf) throws IOException {
    // calculate the index of to which we need to sink in order to write
    int targetIndex = chunkPosition * this.chunkSessionInformation.getChunkSize();
    // sink to the index of the chunk position we need to write to
    this.targetFile.seek(targetIndex);
    // write the content into the file at the current offset we sunk to
    this.targetFile.write(dataBuf.readByteArray());
    // notify our index about the write operation
    this.writtenFileParts.incrementAndGet();
  }

  protected void updateStatus() {
    // we only need to update the status when the transfer is running but the whole content was written
    if (this.transferStatus == TransferStatus.RUNNING
      && this.expectedFileParts != null
      && this.expectedFileParts == this.writtenFileParts.get()
    ) {
      this.transferStatus = TransferStatus.SUCCESS;
    }
  }
}
