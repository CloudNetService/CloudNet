/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.node.command.defaults;

import cloud.commandframework.execution.CommandSuggestionProcessor;
import cloud.commandframework.execution.preprocessor.CommandPreprocessingContext;
import com.google.common.base.Strings;
import eu.cloudnetservice.common.util.StringUtil;
import eu.cloudnetservice.node.command.source.CommandSource;
import info.debatty.java.stringsimilarity.JaroWinkler;
import info.debatty.java.stringsimilarity.interfaces.StringSimilarity;
import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;

/**
 * {@inheritDoc}
 */
@Singleton
final class DefaultSuggestionProcessor implements CommandSuggestionProcessor<CommandSource> {

  private static final double MINIMUM_SIMILARITY = 0.7;
  private static final double MAXIMUM_SIMILARITY = 1.0;

  private static final StringSimilarity SIMILARITY_ALGORITHM = new JaroWinkler();
  private static final Comparator<Map.Entry<String, Double>> SIMILARITY_COMPARATOR = Collections.reverseOrder(
    Comparator.comparingDouble(Map.Entry::getValue));

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull List<String> apply(
    @NonNull CommandPreprocessingContext<CommandSource> context,
    @NonNull List<String> allSuggestions
  ) {
    // íf there is no input yet, just return all suggestions
    var input = context.getInputQueue().peek();
    if (Strings.isNullOrEmpty(input)) {
      return allSuggestions;
    }

    // filter out the suggestions which are the closest to what the user provided
    var lowerCasedInput = StringUtil.toLower(input);
    var matches = allSuggestions.stream()
      .map(suggestion -> {
        var distance = SIMILARITY_ALGORITHM.similarity(StringUtil.toLower(suggestion), lowerCasedInput);
        return Map.entry(suggestion, distance);
      })
      .filter(entry -> entry.getValue() >= MINIMUM_SIMILARITY)
      .sorted(SIMILARITY_COMPARATOR)
      .collect(Collectors.toCollection(LinkedList::new));

    // check if we got at least one match
    var bestMatch = matches.peek();
    if (bestMatch == null) {
      return List.of();
    }

    // check if the topmost match is a match of 100% - in that case only return that match
    if (bestMatch.getValue() == MAXIMUM_SIMILARITY) {
      return List.of(bestMatch.getKey());
    } else {
      return matches.stream().map(Map.Entry::getKey).toList();
    }
  }
}
