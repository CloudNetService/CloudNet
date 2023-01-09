/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.ext.updater.defaults;

import eu.cloudnetservice.ext.updater.Updater;
import eu.cloudnetservice.ext.updater.UpdaterRegistry;
import java.util.Deque;
import java.util.LinkedList;
import lombok.NonNull;

public abstract class DefaultUpdaterRegistry<T, C> implements UpdaterRegistry<T, C> {

  protected final Deque<Updater<T>> updaters = new LinkedList<>();

  @Override
  public void runUpdater(@NonNull C context, boolean onlyRequiredUpdates) throws Exception {
    var updaterContext = this.provideContext(context);
    for (var updater : this.updaters) {
      updater.executeUpdates(updaterContext, onlyRequiredUpdates);
    }
  }

  @Override
  public void registerUpdater(@NonNull Updater<T> updater) {
    this.updaters.addLast(updater);
  }

  protected abstract @NonNull T provideContext(@NonNull C provisionContext) throws Exception;
}
