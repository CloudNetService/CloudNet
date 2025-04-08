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

package eu.cloudnetservice.driver.language;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A provider for message translations. Given a key of a translations and arguments for translation this provider is
 * responsible for returning a fully translated message. How the translation is obtained and the arguments inserted into
 * the translation is left to the provider implementation.
 *
 * @since 4.0
 */
@FunctionalInterface
public interface TranslationProvider {

  /**
   * Get the translation for the given key with the given arguments inserted into it. This method returns null in case
   * the given translation key is not mapped to a translation in this provider.
   *
   * @param key  the key of the translation to get.
   * @param args the arguments that can be inserted into the message.
   * @return the translated message, filled with the given arguments or null if the message key is unknown.
   * @throws NullPointerException     if the given key or arguments array is null.
   * @throws IllegalArgumentException if one of the given placeholder arguments is invalid.
   */
  @Nullable
  String translate(@NonNull String key, Object... args);
}
