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
 * A maintainer being returned in responses by a module repository.
 *
 * @since 4.0
 */
public interface ModuleRepoMaintainer extends Named {

  /**
   * Get the id of the maintainer.
   *
   * @return the id of the maintainer.
   */
  @NonNull
  UUID id();

  /**
   * Get the id of the user who is this maintainer.
   *
   * @return the id of the user who is this maintainer.
   */
  @NonNull
  UUID userId();

  /**
   * Get the url to the avatar of the user.
   *
   * @return the url to the avatar of the user.
   */
  @NonNull
  String avatarUrl();

  /**
   * Get the role of the maintainer for the associated module.
   *
   * @return the role of the maintainer for the associated module.
   */
  @NonNull
  Role role();

  /**
   * The roles that a maintainer of a module can have.
   *
   * @since 4.0
   */
  enum Role {

    /**
     * The maintainer is the owner of the module.
     */
    OWNER,
    /**
     * The maintainer is an admin of the module.
     */
    ADMIN,
    /**
     * The maintainer is a regular maintainer of the module.
     */
    MAINTAINER,
  }
}
