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

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.replacer.model.PlaceholderReplacement;
import eu.cloudnetservice.modules.replacer.type.ReplaceType;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class ValueSelectorFactory {

  private final ConditionalSelector conditionalSelector;

  public ValueSelectorFactory() {
    this(new ConditionalSelector());
  }

  public ValueSelectorFactory(@NonNull ConditionalSelector conditionalSelector) {
    this.conditionalSelector = conditionalSelector;
  }

  public @Nullable ValueSelector selector(
    ReplaceType replaceType,
    PlaceholderReplacement replacement,
    ServiceInfoSnapshot serviceInfo
  ) {
    var values = replacement.values() == null ? List.<String>of() : replacement.values();
    return switch (replaceType) {
      case FIRST -> values.isEmpty() ? null : _ -> values.getFirst();
      case RANDOM -> values.isEmpty() ? null : _ -> values.get(ThreadLocalRandom.current().nextInt(values.size()));
      case SEQUENTIAL -> values.isEmpty() ? null : occurrence -> values.get(occurrence % values.size());
      case CONDITIONAL -> this.conditionalSelector.selector(replacement.conditions(), serviceInfo);
    };
  }
}
