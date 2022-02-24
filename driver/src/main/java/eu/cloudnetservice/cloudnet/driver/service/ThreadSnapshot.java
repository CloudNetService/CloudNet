/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.driver.service;

import eu.cloudnetservice.cloudnet.common.Nameable;
import java.lang.Thread.State;
import java.lang.management.ThreadInfo;
import lombok.NonNull;

/**
 * Contains some basic information about a thread which is known to the jvm. The thread snapshot will not update the
 * information about the associated thread and only contains its data at a specific point of time. Make sure to get the
 * thread information again if an updated version of it is required.
 *
 * @param id          the numeric id of the running thread.
 * @param priority    the priority of the thread.
 * @param daemon      if the thread is a daemon thread.
 * @param name        the name of the thread.
 * @param threadState the current state of the thread.
 * @since 4.0
 */
public record ThreadSnapshot(
  long id,
  int priority,
  boolean daemon,
  @NonNull String name,
  @NonNull State threadState
) implements Nameable, Cloneable {

  /**
   * Creates a thread snapshot from the given thread.
   *
   * @param thread the thread to create the snapshot for.
   * @return the created thread snapshot for the given thread.
   * @throws NullPointerException if the given thread is null.
   */
  public static @NonNull ThreadSnapshot from(@NonNull Thread thread) {
    return new ThreadSnapshot(
      thread.getId(),
      thread.getPriority(),
      thread.isDaemon(),
      thread.getName(),
      thread.getState());
  }

  /**
   * Creates a thread snapshot from the given thread info.
   *
   * @param info the thread info to create the thread snapshot for.
   * @return the created thread snapshot based on the given thread info.
   * @throws NullPointerException if the given thread info is null.
   */
  public static @NonNull ThreadSnapshot from(@NonNull ThreadInfo info) {
    return new ThreadSnapshot(
      info.getThreadId(),
      info.getPriority(),
      info.isDaemon(),
      info.getThreadName(),
      info.getThreadState());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ThreadSnapshot clone() {
    try {
      return (ThreadSnapshot) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException();
    }
  }
}
