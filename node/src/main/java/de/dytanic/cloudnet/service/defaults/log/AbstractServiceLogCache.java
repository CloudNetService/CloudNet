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

package de.dytanic.cloudnet.service.defaults.log;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.IServiceConsoleLogCache;
import de.dytanic.cloudnet.service.ServiceConsoleLineHandler;
import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.NonNull;
import org.jetbrains.annotations.UnmodifiableView;

public abstract class AbstractServiceLogCache implements IServiceConsoleLogCache {

  protected static final Logger LOGGER = LogManager.logger(AbstractServiceLogCache.class);

  protected final ICloudService service;

  protected final Queue<String> cachedLogMessages = new ConcurrentLinkedQueue<>();
  protected final Set<ServiceConsoleLineHandler> handlers = ConcurrentHashMap.newKeySet();

  protected volatile int logCacheSize;
  protected volatile boolean alwaysPrintErrorStreamToConsole;

  public AbstractServiceLogCache(@NonNull CloudNet cloudNet, @NonNull ICloudService service) {
    this.service = service;
    this.logCacheSize = cloudNet.getConfig().maxServiceConsoleLogCacheSize();
    this.alwaysPrintErrorStreamToConsole = cloudNet.getConfig().printErrorStreamLinesFromServices();
  }

  @Override
  public @NonNull ICloudService service() {
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
    this.handlers.add(Preconditions.checkNotNull(handler, "handler"));
  }

  @Override
  public void removeHandler(@NonNull ServiceConsoleLineHandler handler) {
    this.handlers.remove(Preconditions.checkNotNull(handler, "handler"));
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
      LOGGER.warning(String.format("[%s/SERR]: %s", this.service.serviceId().name(), entry));
    }
    // add the line
    this.cachedLogMessages.add(entry);
    // call all handlers
    if (!this.handlers.isEmpty()) {
      for (var handler : this.handlers) {
        handler.handleLine(this, entry);
      }
    }
  }
}
