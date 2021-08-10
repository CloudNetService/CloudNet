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

package de.dytanic.cloudnet.driver.module;

import java.util.Arrays;
import org.jetbrains.annotations.NotNull;

public enum ModuleLifeCycle {
  /**
   * The initial state of a module until the module gets loaded. Defined module tasks for this state will never fire.
   */
  CREATED(1),
  /**
   * Used when the module instance was just created. In this stage no modules the module might depend on must be
   * loaded.
   */
  LOADED(2),
  /**
   * In this state the module ist started and every module dependency declared by the module was started before.
   */
  STARTED(3),
  /**
   * In this state the module is still loadable, it's only idling and not doing anything anymore.
   */
  STOPPED(2, 4),
  /**
   * In this state the module is completely unloaded and will switch to the {@link #UNUSEABLE} state shortly.
   */
  UNLOADED(1, 5),
  /**
   * In this state the module wrapper instance is empty, it is not associated with a module anymore. Defined module
   * tasks for this state will never fire.
   */
  UNUSEABLE;

  private final int[] possibleChangeTargetOrdinals;

  /**
   * Creates a new module lifecycle enum constant instance.
   *
   * @param possibleChangeTargetOrdinals all ordinal indexes of other lifecycles this lifecycle can be changed to.
   */
  ModuleLifeCycle(int... possibleChangeTargetOrdinals) {
    this.possibleChangeTargetOrdinals = possibleChangeTargetOrdinals;
    Arrays.sort(this.possibleChangeTargetOrdinals);
  }

  /**
   * Checks if a module can change from this state to the given {@code target}.
   *
   * @param target the target state the module want's to change to.
   * @return If the module can change from the current into the {@code target} state.
   */
  public boolean canChangeTo(@NotNull ModuleLifeCycle target) {
    return Arrays.binarySearch(this.possibleChangeTargetOrdinals, target.ordinal()) >= 0;
  }
}
