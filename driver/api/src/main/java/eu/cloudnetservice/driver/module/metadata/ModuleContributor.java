/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.module.metadata;

import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Information about someone that contributed to the module.
 *
 * @since 4.0
 */
public interface ModuleContributor {

  /**
   * Get the display name of the contributor, can for example be the real name or GitHub username.
   *
   * @return the display name of the contributor.
   */
  @NonNull
  String name();

  /**
   * Get additional information about the contributor, in a key-value form. This could for example contain contact
   * information per platform or the timezone.
   *
   * @return additional information about the contributor.
   */
  @NonNull
  @Unmodifiable
  Map<String, String> additionalInformation();
}
