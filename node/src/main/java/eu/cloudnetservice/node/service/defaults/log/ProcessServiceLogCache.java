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

package eu.cloudnetservice.node.service.defaults.log;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.common.util.StringUtil;
import eu.cloudnetservice.driver.service.ServiceId;
import eu.cloudnetservice.node.config.Configuration;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class ProcessServiceLogCache extends AbstractServiceLogCache {

  private final ProcessServiceLogReadScheduler scheduler;

  private volatile ProcessHandle targetProcess;
  private NonBlockingReader outStreamReader;
  private NonBlockingReader errStreamReader;

  public ProcessServiceLogCache(
    @NonNull Configuration configuration,
    @NonNull ServiceId associatedServiceId,
    @NonNull ProcessServiceLogReadScheduler scheduler
  ) {
    super(configuration, associatedServiceId);
    this.scheduler = scheduler;
  }

  public void start(@NonNull Process process) {
    Preconditions.checkState(this.targetProcess == null);
    this.targetProcess = process.toHandle();
    this.outStreamReader = new NonBlockingReader(process.inputReader(StandardCharsets.UTF_8));
    this.errStreamReader = new NonBlockingReader(process.errorReader(StandardCharsets.UTF_8));
    this.scheduler.schedule(this);
  }

  public void stop() {
    try {
      var outReader = this.outStreamReader;
      var errReader = this.errStreamReader;
      if (outReader != null && errReader != null) {
        outReader.shutdown = true;
        errReader.shutdown = true;
        // Processes killed by the termination timeout could have remaining content in the output streams.
        // Read all remaining content and then close the streams
        this.readLinesFromStream(outReader, false);
        this.readLinesFromStream(errReader, true);
        outReader.close();
        errReader.close();
        this.outStreamReader = null;
        this.errStreamReader = null;
      }

      // no longer targeting a process, always reset the target process
      // in case something went wrong elsewhere to allow re-using this
      // log cache in that case anyway
      this.targetProcess = null;
    } catch (IOException exception) {
      LOGGER.error("Failed to close process streams of service {}", this.associatedServiceId.name(), exception);
    }
  }

  public boolean readProcessOutputContent() {
    try {
      var outReader = this.outStreamReader;
      var errReader = this.errStreamReader;
      if (outReader == null || errReader == null) {
        return false;
      }

      // try to read all lines from both stream if content is available
      // these calls do not block in case the readers have no content
      // available yet
      this.readLinesFromStream(outReader, false);
      this.readLinesFromStream(errReader, true);

      // check if the target process terminated, we can stop reading
      // the data streams in that case
      // the data that was buffered is now removed from the reader and
      // no now data will become available if the process is dead
      var targetProcess = this.targetProcess;
      if (targetProcess == null || !targetProcess.isAlive()) {
        this.stop(); // call stop to ensure that the termination is properly handled (prevent state mismatch)
        return false;
      }

      return true;
    } catch (IOException exception) {
      // stream close and read can happen concurrently, so in case the stream
      // closed we don't want to log the exception but rather signal that the
      // service was stopped. "stream closed" is the message for both the reader
      // being closed and the file descriptor being no longer available (process terminated)
      var message = StringUtil.toLower(exception.getMessage());
      if (message != null && message.equals("stream closed")) {
        this.stop(); // call stop to ensure that the termination is properly handled (prevent state mismatch)
        LOGGER.debug("Encountered closed out/err stream for service {}, stopping", associatedServiceId);
        return false;
      } else {
        LOGGER.error("Unable to read out/err stream of service {}", this.associatedServiceId, exception);
        return true; // couldn't read this time, but maybe we can read next time?
      }
    }
  }

  private void readLinesFromStream(@NonNull NonBlockingReader stream, boolean errStream) throws IOException {
    while (true) {
      var line = stream.readLine();
      if (line == null) {
        break;
      }

      this.handleItem(line, errStream);
    }
  }

  private static class NonBlockingReader {

    private final BufferedReader reader;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private boolean shutdown = false;
    private boolean lastCR = false;

    public NonBlockingReader(BufferedReader reader) {
      this.reader = reader;
    }

    /**
     * Tries to read a line, if no line is available returns null
     */
    private synchronized @Nullable String readLine() throws IOException {
      // Strategy: Collect everything into the buffer until we hit a newline
      // Then return the line excluding the (CR and) BR character(s)
      while (this.reader.ready()) {
        var read = this.reader.read();
        if (this.lastCR) {
          // Make sure to skip CR when creating the line
          if (read != '\n') {
            this.buffer.write('\r');
          }
          this.lastCR = false;
        } else if (read == '\r') {
          this.lastCR = true;
          // Don't append \r to buffer
          continue;
        }

        if (read == '\n') {
          // finished newline
          var line = this.buffer.toString(StandardCharsets.UTF_8);
          this.buffer.reset();
          return line;
        }
        this.buffer.write(read);
      }
      if (this.shutdown && this.buffer.size() > 0) {
        var line = this.buffer.toString(StandardCharsets.UTF_8);
        this.buffer.reset();
        return line;
      }
      return null;
    }

    private void close() throws IOException {
      this.reader.close();
    }
  }
}
