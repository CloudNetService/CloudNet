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

package eu.cloudnetservice.modules.replacer.model;

import eu.cloudnetservice.modules.replacer.model.condition.ConditionRule;
import eu.cloudnetservice.modules.replacer.type.ReplaceType;
import eu.cloudnetservice.modules.replacer.type.SearchType;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public record PlaceholderReplacement(
  String token,
  @Nullable SearchType searchType,
  @Nullable ReplaceType replaceType,
  @Nullable List<String> values,
  @Nullable List<ConditionRule> conditions
) {
}
