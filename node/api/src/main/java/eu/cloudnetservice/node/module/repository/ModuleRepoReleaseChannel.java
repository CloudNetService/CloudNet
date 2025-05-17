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

package eu.cloudnetservice.node.module.repository;

import eu.cloudnetservice.driver.base.Named;
import java.util.UUID;
import lombok.NonNull;

/**
 * A release channel of a module, used to publish different versions of artifacts on (e.g. unstable, testing versions in
 * a testing channel, stable versions in a release channel).
 *
 * @since 4.0
 */
public interface ModuleRepoReleaseChannel extends Named {

  /**
   * Get the id of the release channel.
   *
   * @return the id of the release channel.
   */
  @NonNull
  UUID id();

  /**
   * Get a short description of the release channel.
   *
   * @return a short description of the release channel.
   */
  @NonNull
  String description();

  /**
   * Get if the channel is protected and therefore hidden (unavailable) for everyone.
   *
   * @return if the channel is protected and therefore hidden.
   */
  boolean hidden();
}
