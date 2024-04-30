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

package eu.cloudnetservice.ext.component;

import java.util.Map;
import lombok.NonNull;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public class MinimessageUtils {

  public static TagResolver[] tagsFromMap(@NonNull Map<String, String> placeholders) {
    return placeholders.entrySet()
            .stream()
            .map((entry) -> Placeholder.unparsed(entry.getKey(), entry.getValue()))
            .toArray((size) -> new TagResolver[size]);
  }

}
