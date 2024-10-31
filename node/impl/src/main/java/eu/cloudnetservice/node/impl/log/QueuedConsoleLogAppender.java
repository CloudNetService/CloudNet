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

package eu.cloudnetservice.node.impl.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import dev.derklaro.aerogel.binding.BindingBuilder;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.node.impl.event.log.LoggingEntryEvent;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import lombok.NonNull;

/**
 * A logging handler for developers, that can easy handle and get the logging outputs from this node instance
 */
public final class QueuedConsoleLogAppender extends ConsoleAppender<ILoggingEvent> {

  private final EventManager eventManager;

  /**
   * A queue that contain the last 128 logging output as LogEntries that should print into the console
   */
  private final Queue<ILoggingEvent> cachedQueuedLogEntries = new ConcurrentLinkedQueue<>();

  public QueuedConsoleLogAppender() {
    this.eventManager = InjectionLayer.boot().instance(EventManager.class);

    // we have to install the binding for the log appender ourselves because this class gets constructed by logback,
    // but we need the access in other classes
    InjectionLayer.boot().install(BindingBuilder.create().bind(QueuedConsoleLogAppender.class).toInstance(this));
  }

  public @NonNull Queue<ILoggingEvent> cachedLogEntries() {
    return this.cachedQueuedLogEntries;
  }

  public @NonNull Queue<String> formattedCachedLogLines() {
    return this.cachedQueuedLogEntries.stream()
      .map(event -> new String(super.encoder.encode(event)))
      .collect(Collectors.toCollection(LinkedList::new));
  }

  @Override
  protected void append(@NonNull ILoggingEvent event) {
    this.cachedQueuedLogEntries.offer(event);
    while (this.cachedQueuedLogEntries.size() > 128) {
      this.cachedQueuedLogEntries.poll();
    }

    this.eventManager.callEvent(new LoggingEntryEvent(event));
  }
}
