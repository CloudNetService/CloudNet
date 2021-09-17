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

package de.dytanic.cloudnet.driver.service;

import java.lang.Thread.State;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import org.jetbrains.annotations.NotNull;

/**
 * The information of a thread running on any process in the Cloud.
 */
@ToString
@EqualsAndHashCode
public class ThreadSnapshot {

  private final long id;
  private final int priority;
  private final boolean daemon;

  private final String name;
  private final Thread.State threadState;

  @Deprecated
  @ScheduledForRemoval
  public ThreadSnapshot(long id, String name, Thread.State threadState, boolean daemon, int priority) {
    this(id, priority, daemon, name, threadState);
  }

  public ThreadSnapshot(@NotNull Thread thread) {
    this(thread.getId(), thread.getPriority(), thread.isDaemon(), thread.getName(), thread.getState());
  }

  public ThreadSnapshot(long id, int priority, boolean daemon, String name, State threadState) {
    this.id = id;
    this.priority = priority;
    this.daemon = daemon;
    this.name = name;
    this.threadState = threadState;
  }

  public long getId() {
    return this.id;
  }

  public @NotNull String getName() {
    return this.name;
  }

  public @NotNull Thread.State getThreadState() {
    return this.threadState;
  }

  public boolean isDaemon() {
    return this.daemon;
  }

  public int getPriority() {
    return this.priority;
  }
}
