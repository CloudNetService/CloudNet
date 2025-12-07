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

package eu.cloudnetservice.modules.replacer.replacement;

import eu.cloudnetservice.modules.replacer.type.SearchType;
import org.jetbrains.annotations.Nullable;

public final class SearchReplacer {

  public String apply(String content, String token, SearchType searchType, ValueSelector selector) {
    return switch (searchType) {
      case FIRST -> this.replaceFirst(content, token, selector.nextValue(0));
      case ALL -> this.replaceAll(content, token, selector);
    };
  }

  public String replaceFirst(String content, String token, @Nullable String value) {
    if (value == null) {
      return content;
    }

    var idx = content.indexOf(token);
    if (idx < 0) {
      return content;
    }

    return content.substring(0, idx) + value + content.substring(idx + token.length());
  }

  public String replaceAll(String content, String token, ValueSelector selector) {
    var idx = content.indexOf(token);
    if (idx < 0) {
      return content;
    }

    var builder = new StringBuilder();
    var lastIndex = 0;
    var occurrence = 0;
    while (idx >= 0) {
      var value = selector.nextValue(occurrence++);
      if (value == null) {
        builder.append(content, lastIndex, content.length());
        return builder.toString();
      }

      builder.append(content, lastIndex, idx).append(value);
      lastIndex = idx + token.length();
      idx = content.indexOf(token, lastIndex);
    }
    builder.append(content, lastIndex, content.length());
    return builder.toString();
  }
}
