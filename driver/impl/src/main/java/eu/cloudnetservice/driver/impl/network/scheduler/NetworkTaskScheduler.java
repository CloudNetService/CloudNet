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

package eu.cloudnetservice.driver.impl.network.scheduler;

import java.util.concurrent.Executor;

/**
 * A scheduler for tasks that are triggered by incoming network calls and should be handled non-blocking (for execute
 * invocations) with the lowest latency possible.
 *
 * @since 4.0
 */
public interface NetworkTaskScheduler extends Executor {

  /**
   * Triggers a shutdown operation on this scheduler, interrupting all currently running tasks and preventing new tasks
   * from being scheduled.
   */
  void shutdown();
}
