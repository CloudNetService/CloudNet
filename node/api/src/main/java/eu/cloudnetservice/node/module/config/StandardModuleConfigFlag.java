/*
 * Copyright 2019-2025 CloudNetService team & contributors
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

package eu.cloudnetservice.node.module.config;

/**
 * Collection of standard module configuration flags.
 *
 * @since 4.0
 */
public enum StandardModuleConfigFlag implements ModuleConfigFlag {

  /**
   * Indicates that the configuration contains sensitive information that shouldn't be exposed to untrusted sources.
   * This does not mean that the api cannot access the configuration.
   */
  SENSITIVE,
}
