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

import de.dytanic.cloudnet.common.Nameable;
import java.lang.Thread.State;
import lombok.NonNull;

/**
 * The information of a thread running on any process in the Cloud.
 */
public record ThreadSnapshot(
  long id,
  int priority,
  boolean daemon,
  @NonNull String name,
  @NonNull State threadState
) implements Nameable, Cloneable {

  public static @NonNull ThreadSnapshot from(@NonNull Thread thread) {
    return new ThreadSnapshot(
      thread.getId(),
      thread.getPriority(),
      thread.isDaemon(),
      thread.getName(),
      thread.getState());
  }

  @Override
  public @NonNull ThreadSnapshot clone() {
    try {
      return (ThreadSnapshot) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException();
    }
  }
}
