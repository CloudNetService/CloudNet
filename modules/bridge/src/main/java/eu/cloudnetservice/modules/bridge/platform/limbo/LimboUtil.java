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

package eu.cloudnetservice.modules.bridge.platform.limbo;

import com.loohp.limbo.player.Player;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

final class LimboUtil {

  private static final Function<Player, String> LOCALE_GETTER;
  private static final Map<String, Locale> LOCALE_CACHE = new HashMap<>();

  static {
    LOCALE_GETTER = player -> "en_US";
  }

  private LimboUtil() {
    throw new UnsupportedOperationException();
  }

  public static @Nullable Locale playerLocale(@NonNull Player player) {
    // resolve the language using the getter, do nothing if we were not able to resolve the language for some reason
    var localeTag = LOCALE_GETTER.apply(player);
    if (localeTag == null) {
      return null;
    }

    // resolve the locale tag from the cache, put it in if missing
    return LOCALE_CACHE.computeIfAbsent(localeTag, tag -> Locale.forLanguageTag(tag.replace('_', '-')));
  }
}
