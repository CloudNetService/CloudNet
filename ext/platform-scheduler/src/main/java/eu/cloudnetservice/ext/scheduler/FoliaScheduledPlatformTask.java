/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.ext.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.NonNull;

/**
 * A scheduled platform task implementation for Folia's scheduler.
 *
 * @since 4.0
 */
final class FoliaScheduledPlatformTask implements ScheduledPlatformTask {

  private final ScheduledTask scheduledTask;

  /**
   * Creates a new scheduled platform task for a given Folia scheduled task.
   *
   * @param scheduledTask the scheduled task to wrap.
   * @throws NullPointerException if the given scheduled task is null.
   */
  FoliaScheduledPlatformTask(@NonNull ScheduledTask scheduledTask) {
    this.scheduledTask = scheduledTask;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void cancel() {
    this.scheduledTask.cancel();
  }
}
