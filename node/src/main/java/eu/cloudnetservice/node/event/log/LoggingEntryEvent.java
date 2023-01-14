/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.node.event.log;

import eu.cloudnetservice.driver.event.Event;
import java.util.logging.LogRecord;
import lombok.NonNull;

public final class LoggingEntryEvent extends Event {

  private final LogRecord record;

  public LoggingEntryEvent(@NonNull LogRecord record) {
    this.record = record;
  }

  public @NonNull LogRecord logEntry() {
    return this.record;
  }
}
