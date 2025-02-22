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

import com.google.common.base.Preconditions;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.service.CloudService;
import eu.cloudnetservice.node.service.ServiceConsoleLineHandler;
import eu.cloudnetservice.node.service.ServiceConsoleLogCache;
import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.NonNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractServiceLogCache implements ServiceConsoleLogCache {

  protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractServiceLogCache.class);

  protected final CloudService service;

  protected final Queue<String> cachedLogMessages = new ConcurrentLinkedQueue<>();
  protected final Set<ServiceConsoleLineHandler> handlers = ConcurrentHashMap.newKeySet();

  protected volatile int logCacheSize;
  protected volatile boolean alwaysPrintErrorStreamToConsole;

  public AbstractServiceLogCache(@NonNull Configuration configuration, @NonNull CloudService service) {
    this.service = service;
    this.logCacheSize = configuration.maxServiceConsoleLogCacheSize();
    this.alwaysPrintErrorStreamToConsole = configuration.printErrorStreamLinesFromServices();
  }

  @Override
  public @NonNull CloudService service() {
    return this.service;
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
    // drain the cache
    while (this.cachedLogMessages.size() > this.logCacheSize) {
      this.cachedLogMessages.poll();
    }
    // print the line to the console if enabled
    if (this.alwaysPrintErrorStreamToConsole && comesFromErrorStream) {
      LOGGER.warn("[{}/WARN]: {}", this.service.serviceId().name(), entry);
    }
    // add the line
    this.cachedLogMessages.add(entry);
    // call all handlers
    if (!this.handlers.isEmpty()) {
      for (var handler : this.handlers) {
        handler.handleLine(this, entry, comesFromErrorStream);
      }
    }
  }
}
