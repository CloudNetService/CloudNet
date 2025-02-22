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

package eu.cloudnetservice.node.impl.service.defaults.log;

import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.service.CloudService;
import eu.cloudnetservice.node.service.ServiceConsoleLogCache;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import lombok.NonNull;

public class ProcessServiceLogCache extends AbstractServiceLogCache {

  protected final Supplier<Process> processSupplier;

  protected final byte[] buffer = new byte[2048];
  protected final StringBuffer stringBuffer = new StringBuffer();

  public ProcessServiceLogCache(
    @NonNull Supplier<Process> processSupplier,
    @NonNull Configuration configuration,
    @NonNull CloudService service
  ) {
    super(configuration, service);
    this.processSupplier = processSupplier;
  }

  @Override
  public @NonNull ServiceConsoleLogCache update() {
    // check if we can currently update
    var process = this.processSupplier.get();
    if (process != null) {
      try {
        this.readStream(process.getInputStream(), false);
        this.readStream(process.getErrorStream(), true);
      } catch (IOException exception) {
        LOGGER.error("Exception updating content of console for service {}",
          this.service.serviceId().name(),
          exception);
        // reset the string buffer
        this.stringBuffer.setLength(0);
      }
    }
    // for chaining
    return this;
  }

  protected void readStream(@NonNull InputStream stream, boolean isErrorStream) throws IOException {
    int len;
    while (stream.available() > 0 && (len = stream.read(this.buffer, 0, this.buffer.length)) != -1) {
      this.stringBuffer.append(new String(this.buffer, 0, len, StandardCharsets.UTF_8));
    }

    // check if we got a result we can work with
    var content = this.stringBuffer.toString();
    if (content.contains("\n") || content.contains("\r")) {
      for (var input : content.split("\r")) {
        for (var text : input.split("\n")) {
          if (!text.trim().isEmpty()) {
            this.handleItem(text, isErrorStream);
          }
        }
      }
    }

    // reset the string buffer
    this.stringBuffer.setLength(0);
  }
}
