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

package eu.cloudnetservice.ext.component;

import lombok.NonNull;

final class MappingConverter<C> extends ComponentConverter<C> {

  private final ComponentConverter<?> original;
  private final ComponentConverter<C> downstream;

  MappingConverter(
    @NonNull ComponentConverter<?> original,
    @NonNull ComponentConverter<C> downstream
  ) {
    super(downstream.source, downstream.target);
    this.original = original;
    this.downstream = downstream;
  }

  @Override
  public @NonNull String convertText(@NonNull String input) {
    var convertedInput = this.original.convertText(input);
    return this.downstream.convertText(convertedInput);
  }

  @Override
  public @NonNull C convert(@NonNull String input) {
    var convertedInput = this.original.convertText(input);
    return this.downstream.convert(convertedInput);
  }
}
