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
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import lombok.NonNull;

public class ProcessServiceLogCache extends AbstractServiceLogCache {

  private final ProcessServiceLogReadScheduler scheduler;

  private volatile ProcessHandle targetProcess;
  private NonBlockingLineReader outStreamReader;
  private NonBlockingLineReader errStreamReader;

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

    // assumes utf-8 for output encoding
    var charset = StandardCharsets.UTF_8;
    this.outStreamReader = new NonBlockingLineReader(new InputStreamReader(process.getInputStream(), charset));
    this.errStreamReader = new NonBlockingLineReader(new InputStreamReader(process.getErrorStream(), charset));

    this.targetProcess = process.toHandle();
    this.scheduler.schedule(this);
  }

  public void stop() {
    try {
      var outReader = this.outStreamReader;
      var errReader = this.errStreamReader;
      if (outReader != null && errReader != null) {
        outReader.close();
        errReader.close();
        this.outStreamReader = null;
        this.errStreamReader = null;

        // drain remaining buffered lines from the readers after closing
        this.readLinesFromStreams(outReader, errReader);
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
      this.readLinesFromStreams(outReader, errReader);

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

  private void readLinesFromStreams(
    @NonNull NonBlockingLineReader outReader,
    @NonNull NonBlockingLineReader errReader
  ) throws IOException {
    this.readLinesFromStream(outReader, false);
    this.readLinesFromStream(errReader, true);
  }

  private void readLinesFromStream(@NonNull NonBlockingLineReader reader, boolean errStream) throws IOException {
    while (reader.ready()) {
      var line = reader.readLine();
      if (line == null) {
        break;
      }

      this.handleItem(line, errStream);
    }
  }
}
