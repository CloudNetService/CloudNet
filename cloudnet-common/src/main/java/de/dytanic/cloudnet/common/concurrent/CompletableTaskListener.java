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

package de.dytanic.cloudnet.common.concurrent;

public class CompletableTaskListener<T> implements ITaskListener<T> {

  private final CompletableTask<T> task;

  public CompletableTaskListener(CompletableTask<T> task) {
    this.task = task;
  }

  @Override
  public void onCancelled(ITask<T> task) {
    this.task.cancel(true);
  }

  @Override
  public void onComplete(ITask<T> task, T t) {
    this.task.complete(t);
  }

  @Override
  public void onFailure(ITask<T> task, Throwable th) {
    this.task.fail(th);
  }
}
