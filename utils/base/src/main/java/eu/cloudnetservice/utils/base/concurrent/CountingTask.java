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

package eu.cloudnetservice.utils.base.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.Nullable;

/**
 * The counting task is a task that starts at a specified count and is capable of counting both up and down. The task is
 * not completed until the count is 0 or less. Once the task is completed it is not usable anymore.
 *
 * @param <V> the generic type of the task.
 * @since 4.0
 */
public class CountingTask<V> extends CompletableFuture<V> {

  private final V resultValue;
  private final AtomicInteger count;

  /**
   * Constructs a new counting task with a starting count and a value to complete after counting to 0.
   *
   * @param resultValue  the result value to complete this task with.
   * @param initialCount the initial count to start counting from.
   */
  public CountingTask(@Nullable V resultValue, int initialCount) {
    if (initialCount <= 0) {
      this.complete(resultValue);
      // no need to populate the fields
      this.count = null;
      this.resultValue = null;
    } else {
      this.resultValue = resultValue;
      this.count = new AtomicInteger(initialCount);
    }
  }

  /**
   * Increments the count of this task by one and publishes the count change.
   */
  public void countUp() {
    this.count.incrementAndGet();
    this.publishCountChange();
  }

  /**
   * Decrements the count of this task by one and publishes the count change. If the count is 0 or less this task is
   * completed with the given result value.
   */
  public void countDown() {
    this.count.decrementAndGet();
    this.publishCountChange();
  }

  /**
   * Sets the count of this counting task to the given one and publishes the count change.
   *
   * @param target the value to count to.
   */
  public void countTo(int target) {
    this.count.set(target);
    this.publishCountChange();
  }

  /**
   * Gets the current count of this task.
   *
   * @return the count of this task.
   */
  public int count() {
    return this.count.get();
  }

  /**
   * Checks if the count of this task is 0 or less if that is the case this task is completed with its result value.
   */
  protected void publishCountChange() {
    if (this.count() <= 0) {
      this.complete(this.resultValue);
    }
  }
}
