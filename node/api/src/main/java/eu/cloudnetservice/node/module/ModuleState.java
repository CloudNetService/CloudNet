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

package eu.cloudnetservice.node.module;

/**
 * The states a module can be in.
 */
public enum ModuleState {

  /**
   * The module is currently being loaded. Module tasks for this state are supported.
   */
  LOADING,
  /**
   * The module finished loading is now running. Module tasks for this state are supported.
   */
  RUNNING,
  /**
   * The module is being reloaded. Module tasks for this state are supported.
   */
  RELOADING,
  /**
   * The module is being unloaded. Module tasks for this state are supported.
   */
  UNLOADING,
  /**
   * The module was either never loaded or was unloaded. Module tasks for this state are unsupported.
   */
  UNLOADED,
  /**
   * The module was removed from the system and can no longer be loaded. Module tasks for this state are unsupported.
   */
  REMOVED,
}
