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

package eu.cloudnetservice.cloudnet.common.concurrent;

import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.Nullable;

public class CountingTask<V> extends CompletableTask<V> implements Task<V> {

  private final V resultValue;
  private final AtomicInteger count;

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

  public void countUp() {
    this.count.incrementAndGet();
    this.publishCountChange();
  }

  public void countDown() {
    this.count.decrementAndGet();
    this.publishCountChange();
  }

  public void countTo(int target) {
    this.count.set(target);
    this.publishCountChange();
  }

  public void addToCount(int count) {
    this.count.addAndGet(count);
    this.publishCountChange();
  }

  protected void publishCountChange() {
    if (this.count.get() <= 0) {
      this.complete(this.resultValue);
    }
  }
}
