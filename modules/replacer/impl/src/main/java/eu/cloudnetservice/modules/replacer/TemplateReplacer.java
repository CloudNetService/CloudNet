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

package eu.cloudnetservice.modules.replacer;

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.replacer.files.ContentReader;
import eu.cloudnetservice.modules.replacer.files.ContentWriter;
import eu.cloudnetservice.modules.replacer.files.FileSelector;
import eu.cloudnetservice.modules.replacer.match.RuleMatcher;
import eu.cloudnetservice.modules.replacer.model.Replacement;
import eu.cloudnetservice.modules.replacer.model.config.Replacer;
import eu.cloudnetservice.modules.replacer.placeholder.BuiltInPlaceholderProvider;
import eu.cloudnetservice.modules.replacer.placeholder.RulePlaceholderApplier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class TemplateReplacer {

  private final Replacer configuration;
  private final List<Replacement> rules;

  private final RuleMatcher ruleMatcher = new RuleMatcher();
  private final FileSelector fileSelector = new FileSelector();
  private final ContentReader contentReader = new ContentReader();
  private final ContentWriter contentWriter = new ContentWriter();
  private final BuiltInPlaceholderProvider builtInPlaceholderProvider = new BuiltInPlaceholderProvider();
  private final RulePlaceholderApplier rulePlaceholderApplier = new RulePlaceholderApplier();

  public TemplateReplacer(@NonNull Replacer configuration, @NonNull List<Replacement> rules) {
    this.configuration = configuration;
    this.rules = List.copyOf(rules);
  }

  public void apply(@NonNull ServiceInfoSnapshot serviceInfo, @NonNull Path serviceDirectory, @Nullable String template) {
    var matchingRules = this.ruleMatcher.matchingRules(this.rules, serviceInfo, template);
    var allGlobs = this.ruleMatcher.collectGlobs(this.configuration, matchingRules);
    if (allGlobs.isEmpty()) {
      return;
    }

    var pathMatchers = this.ruleMatcher.toPathMatchers(allGlobs);
    var rulePathMatchers = matchingRules.stream()
      .collect(Collectors.toMap(Function.identity(), rule -> this.ruleMatcher.resolveGlobs(rule, this.configuration)));

    var builtInsEnabled = this.configuration.builtInPlaceholdersEnabled();
    var builtIns = builtInsEnabled ? this.builtInPlaceholderProvider.build(serviceInfo) : Map.<String, String>of();

    try (var files = this.fileSelector.findFiles(serviceDirectory, pathMatchers)) {
      files.forEach(path -> this.applyToFile(path, serviceInfo, matchingRules, rulePathMatchers, builtInsEnabled, builtIns));
    } catch (IOException exception) {
      // ignore discovery errors
    }
  }

  private void applyToFile(
    Path path,
    ServiceInfoSnapshot serviceInfo,
    List<Replacement> matchingRules,
    Map<Replacement, List<PathMatcher>> rulePathMatchers,
    boolean builtInsEnabled,
    Map<String, String> builtIns
  ) {
    if (!this.withinSizeLimit(path)) {
      return;
    }

    var content = this.contentReader.read(path);
    if (content == null) {
      return;
    }

    var updated = content;
    if (builtInsEnabled) {
      updated = this.rulePlaceholderApplier.applyBuiltIns(updated, builtIns, serviceInfo);
    }

    for (var rule : matchingRules) {
      var matchers = rulePathMatchers.get(rule);
      if (matchers != null && !matchers.isEmpty() && !this.ruleMatcher.matchesAny(path, matchers)) {
        continue;
      }

      updated = this.rulePlaceholderApplier.applyRule(updated, rule, this.configuration, serviceInfo);
    }

    this.contentWriter.writeIfChanged(path, content, updated);
  }

  private boolean withinSizeLimit(Path path) {
    var limitSection = this.configuration.limits();
    long limit = limitSection == null ? 0L : limitSection.maxFileSizeBytes();
    if (limit <= 0L) {
      return true;
    }

    try {
      return Files.size(path) <= limit;
    } catch (IOException exception) {
      return false;
    }
  }
}

