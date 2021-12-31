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

package de.dytanic.cloudnet.driver;

/**
 * Represents the current environment of the currently running CloudNet driver implementation. By default, every running
 * instance is marked as {@link #EMBEDDED}. A {@link #CLOUDNET} representation is every implementation which is capable
 * of managing services like the default node implementation. A {@link #WRAPPER} implementation on the other hand means
 * that CloudNet manages a service process like a Paper server software instance.
 *
 * @author Pasqual K. (derklaro@cloudnetservice.eu)
 * @see CloudNetDriver#environment()
 */
public enum DriverEnvironment {

  /**
   * A CloudNet implementation which is capable of managing services.
   */
  CLOUDNET,

  /**
   * A CloudNet implementation which manages a service process like a Paper server software instance.
   */
  WRAPPER,

  /**
   * The default environment when running embedded in another environment.
   */
  EMBEDDED
}
