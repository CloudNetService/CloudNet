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

package eu.cloudnetservice.modules.replacer.match;

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.replacer.model.Replacement;
import eu.cloudnetservice.modules.replacer.model.TargetDefinition;
import eu.cloudnetservice.modules.replacer.model.config.Replacer;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class RuleMatcher {

  public List<Replacement> matchingRules(
    @NonNull List<Replacement> rules,
    @NonNull ServiceInfoSnapshot serviceInfo,
    @Nullable String template
  ) {
    return rules.stream()
      .filter(this::isRuleEnabled)
      .filter(rule -> this.matches(rule, serviceInfo, template))
      .toList();
  }

  public List<String> collectGlobs(@NonNull Replacer configuration, @NonNull List<Replacement> matchingRules) {
    var globs = new ArrayList<String>();
    if (configuration.paths() != null) {
      globs.addAll(this.nonNullList(configuration.paths().filePatterns()));
    }
    for (var rule : matchingRules) {
      if (rule.files() != null && !rule.files().isEmpty()) {
        globs.addAll(rule.files());
      }
    }
    return globs.stream().distinct().toList();
  }

  public List<PathMatcher> resolveGlobs(@NonNull Replacement rule, @NonNull Replacer configuration) {
    var globs = rule.files();
    if (globs == null || globs.isEmpty()) {
      globs = configuration.paths() != null ? configuration.paths().filePatterns() : List.of();
    }
    return this.nonNullList(globs).stream()
      .map(glob -> FileSystems.getDefault().getPathMatcher("glob:**/" + glob))
      .collect(Collectors.toList());
  }

  public List<PathMatcher> toPathMatchers(@NonNull List<String> globs) {
    return globs.stream()
      .map(glob -> FileSystems.getDefault().getPathMatcher("glob:**/" + glob))
      .toList();
  }

  public boolean matchesAny(@NonNull Path path, @NonNull List<PathMatcher> matchers) {
    for (var matcher : matchers) {
      if (matcher.matches(path)) {
        return true;
      }
    }
    return false;
  }

  private boolean matches(Replacement rule, ServiceInfoSnapshot serviceInfo, @Nullable String template) {
    List<TargetDefinition> targets = rule.targets();
    if (targets == null || targets.isEmpty()) {
      return true;
    }
    for (TargetDefinition target : targets) {
      if (this.matchesTarget(target, serviceInfo, template)) {
        return true;
      }
    }
    return false;
  }

  private boolean matchesTarget(TargetDefinition target, ServiceInfoSnapshot serviceInfo, @Nullable String template) {
    if (target == null) {
      return true;
    }
    if (target.task() != null && !Objects.equals(target.task(), serviceInfo.serviceId().taskName())) {
      return false;
    }
    if (target.service() != null && !Objects.equals(target.service(), serviceInfo.serviceId().name())) {
      return false;
    }
    if (target.environment() != null
      && !this.environmentMatches(target.environment(), serviceInfo.serviceId().environmentName())) {
      return false;
    }
    if (target.group() != null && !this.hasGroup(serviceInfo, target.group())) {
      return false;
    }

    return target.template() == null || this.matchesTemplate(target.template(), serviceInfo, template);
  }

  private boolean hasGroup(ServiceInfoSnapshot serviceInfo, String desiredGroup) {
    var configuration = serviceInfo.configuration();
    if (configuration.groups().isEmpty()) {
      return false;
    }

    for (var group : configuration.groups()) {
      if (Objects.equals(group, desiredGroup)) {
        return true;
      }
    }

    return false;
  }

  private boolean isRuleEnabled(Replacement rule) {
    return rule.enabled() == null || rule.enabled();
  }

  private boolean matchesTemplate(String targetTemplate, ServiceInfoSnapshot serviceInfo, @Nullable String template) {
    if (template != null) {
      return Objects.equals(targetTemplate, template);
    }

    var templates = serviceInfo.configuration().templates();
    return templates.stream().anyMatch(entry -> Objects.equals(entry.name(), targetTemplate));
  }

  private boolean environmentMatches(String desired, @Nullable String actual) {
    if (desired == null) {
      return true;
    }
    if (actual == null) {
      return false;
    }

    return desired.equalsIgnoreCase(actual);
  }

  private List<String> nonNullList(@Nullable List<String> input) {
    return input == null ? List.of() : input;
  }
}
