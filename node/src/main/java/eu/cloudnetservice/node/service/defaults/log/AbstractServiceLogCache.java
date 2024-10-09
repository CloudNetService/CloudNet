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
import eu.cloudnetservice.driver.service.ServiceId;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.service.ServiceConsoleLineHandler;
import eu.cloudnetservice.node.service.ServiceConsoleLogCache;
import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;
import lombok.NonNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractServiceLogCache implements ServiceConsoleLogCache {

  protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractServiceLogCache.class);
  protected static final Pattern ANSI_SEQUENCE_PATTERN = Pattern.compile("\u001b\\[[0-9;]*[A-Za-z]");

  protected final ServiceId associatedServiceId;

  protected final Queue<String> cachedLogMessages = new ConcurrentLinkedQueue<>();
  protected final Set<ServiceConsoleLineHandler> handlers = ConcurrentHashMap.newKeySet();

  protected volatile int logCacheSize;
  protected volatile boolean alwaysPrintErrorStreamToConsole;

  public AbstractServiceLogCache(@NonNull Configuration configuration, @NonNull ServiceId associatedServiceId) {
    this.associatedServiceId = associatedServiceId;
    this.logCacheSize = configuration.maxServiceConsoleLogCacheSize();
    this.alwaysPrintErrorStreamToConsole = configuration.printErrorStreamLinesFromServices();
  }

  @Override
  public @NonNull ServiceId associatedServiceId() {
    return this.associatedServiceId;
  }

  @Override
  public @NonNull Queue<String> cachedLogMessages() {
    return this.cachedLogMessages;
  }

  @Override
  public int logCacheSize() {
    return this.logCacheSize;
  }

  @Override
  public void logCacheSize(int cacheSize) {
    Preconditions.checkArgument(cacheSize >= 0, "Cache size must be higher or equal to 0");
    this.logCacheSize = cacheSize;
  }

  @Override
  public boolean alwaysPrintErrorStreamToConsole() {
    return this.alwaysPrintErrorStreamToConsole;
  }

  @Override
  public void alwaysPrintErrorStreamToConsole(boolean value) {
    this.alwaysPrintErrorStreamToConsole = value;
  }

  @Override
  public void addHandler(@NonNull ServiceConsoleLineHandler handler) {
    this.handlers.add(handler);
  }

  @Override
  public void removeHandler(@NonNull ServiceConsoleLineHandler handler) {
    this.handlers.remove(handler);
  }

  @Override
  public @NonNull @UnmodifiableView Collection<ServiceConsoleLineHandler> handlers() {
    return Collections.unmodifiableCollection(this.handlers);
  }

  protected void handleItem(@NonNull String entry, boolean comesFromErrorStream) {
    // first remove all ansi sequences from the given log line, this will remove
    // special colors but also operations like line clears that would be displayed
    // in the console if we don't remove them
    // empty log lines could be used for some kind of formatting, but are not really
    // not useful in any way usually, therefore we don't cache them at all
    entry = ANSI_SEQUENCE_PATTERN.matcher(entry).replaceAll("");
    if (entry.isBlank()) {
      return;
    }

    // insert the log line into the cache, unless the cache is disabled
    // if needed we also remove elements from the cache to stay in the provided size bounds
    if (this.logCacheSize > 0) {
      while (this.cachedLogMessages.size() > this.logCacheSize) {
        this.cachedLogMessages.poll();
      }

      this.cachedLogMessages.add(entry);
    }

    if (this.alwaysPrintErrorStreamToConsole && comesFromErrorStream) {
      LOGGER.warn("[{}/WARN]: {}", this.associatedServiceId.name(), entry);
    }

    if (!this.handlers.isEmpty()) {
      for (var handler : this.handlers) {
        handler.handleLine(this, entry, comesFromErrorStream);
      }
    }
  }
}
