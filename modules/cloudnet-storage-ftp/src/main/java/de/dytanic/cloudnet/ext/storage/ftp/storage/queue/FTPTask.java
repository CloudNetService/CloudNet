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

package de.dytanic.cloudnet.ext.storage.ftp.storage.queue;

import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import java.util.Optional;
import java.util.concurrent.Callable;

class FTPTask<V> extends ListenableTask<V> {

  private Exception exception;

  FTPTask(Callable<V> callable) {
    super(callable);

    super.onFailure(throwable -> this.exception = (Exception) throwable);
  }

  FTPTask(Callable<V> callable, Runnable finishedRunnable) {
    super(callable);

    super.onFailure(throwable -> this.exception = (Exception) throwable);
    super.onComplete(ignored -> finishedRunnable.run());
    super.onCancelled(ignored -> finishedRunnable.run());
  }

  @Override
  public V getDef(V def) {
    try {
      return super.get();
    } catch (Exception exception) {
      return def;
    }
  }

  Optional<V> getOptionalValue(V def) {
    try {
      return Optional.ofNullable(super.get());
    } catch (InterruptedException ignored) {
      return Optional.ofNullable(def);
    }
  }

  Exception getException() {
    return this.exception;
  }

}
