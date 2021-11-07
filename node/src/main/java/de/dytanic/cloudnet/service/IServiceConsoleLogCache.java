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

package de.dytanic.cloudnet.service;

import java.util.Collection;
import java.util.Queue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

public interface IServiceConsoleLogCache {

  @NotNull
  Queue<String> getCachedLogMessages();

  @NotNull
  IServiceConsoleLogCache update();

  @NotNull
  ICloudService getService();

  int getLogCacheSize();

  void setLogCacheSize(int cacheSize);

  boolean alwaysPrintErrorStreamToConsole();

  void setAlwaysPrintErrorStreamToConsole(boolean value);

  void addHandler(@NotNull ServiceConsoleLineHandler handler);

  void removeHandler(@NotNull ServiceConsoleLineHandler handler);

  @NotNull
  @UnmodifiableView Collection<ServiceConsoleLineHandler> getHandlers();
}
