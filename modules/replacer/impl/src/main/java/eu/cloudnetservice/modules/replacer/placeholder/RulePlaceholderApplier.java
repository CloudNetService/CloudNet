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

package eu.cloudnetservice.modules.replacer.placeholder;

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.replacer.model.PlaceholderReplacement;
import eu.cloudnetservice.modules.replacer.model.Replacement;
import eu.cloudnetservice.modules.replacer.model.config.Replacer;
import eu.cloudnetservice.modules.replacer.replacement.SearchReplacer;
import eu.cloudnetservice.modules.replacer.replacement.ValueSelector;
import eu.cloudnetservice.modules.replacer.replacement.ValueSelectorFactory;
import eu.cloudnetservice.modules.replacer.type.ReplaceType;
import eu.cloudnetservice.modules.replacer.type.SearchType;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class RulePlaceholderApplier {

  private final SearchReplacer searchReplacer;
  private final ValueSelectorFactory selectorFactory;

  public RulePlaceholderApplier() {
    this(new SearchReplacer(), new ValueSelectorFactory());
  }

  public RulePlaceholderApplier(@NonNull SearchReplacer searchReplacer, @NonNull ValueSelectorFactory selectorFactory) {
    this.searchReplacer = searchReplacer;
    this.selectorFactory = selectorFactory;
  }

  public String applyBuiltIns(String content, Map<String, String> builtIns, ServiceInfoSnapshot serviceInfo) {
    var updated = content;
    for (var entry : builtIns.entrySet()) {
      updated = this.applyPlaceholder(
        updated,
        entry.getKey(),
        SearchType.ALL,
        ReplaceType.FIRST,
        new PlaceholderReplacement(entry.getKey(), SearchType.ALL, ReplaceType.FIRST, List.of(entry.getValue()), null),
        serviceInfo);
    }

    return updated;
  }

  public String applyRule(String content, Replacement rule, Replacer configuration, ServiceInfoSnapshot serviceInfo) {
    List<PlaceholderReplacement> replacements = rule.placeholders();
    if (replacements == null || replacements.isEmpty()) {
      return content;
    }

    var result = content;
    var defaultSearchType = configuration.defaults() != null
      ? configuration.defaults().searchType()
      : SearchType.ALL;
    var defaultReplaceType = configuration.defaults() != null
      ? configuration.defaults().replaceType()
      : ReplaceType.FIRST;

    for (PlaceholderReplacement replacement : replacements) {
      var searchType = replacement.searchType() != null ? replacement.searchType() : defaultSearchType;
      var replaceType = replacement.replaceType() != null ? replacement.replaceType() : defaultReplaceType;

      result = this.applyPlaceholder(result, replacement.token(), searchType, replaceType, replacement, serviceInfo);
    }

    return result;
  }

  private String applyPlaceholder(
    String content,
    @Nullable String token,
    SearchType searchType,
    ReplaceType replaceType,
    PlaceholderReplacement replacement,
    ServiceInfoSnapshot serviceInfo
  ) {
    if (token == null || token.isEmpty()) {
      return content;
    }

    ValueSelector selector = this.selectorFactory.selector(replaceType, replacement, serviceInfo);
    if (selector == null) {
      return content;
    }

    return this.searchReplacer.apply(content, token, searchType, selector);
  }
}
