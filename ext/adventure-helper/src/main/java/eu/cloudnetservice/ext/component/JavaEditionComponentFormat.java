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

abstract class JavaEditionComponentFormat<C> implements ComponentFormat<C> {

  protected static final char COLOR_CHAR = '§';
  protected static final char LEGACY_CHAR = '&';

  @Override
  public char colorIndicationChar() {
    return COLOR_CHAR;
  }

  @Override
  public boolean charIsValidColorIndicationChar(char[] fullData, int pos, char current) {
    return current == COLOR_CHAR || current == LEGACY_CHAR;
  }

  @Override
  public boolean nextSegmentIsLegacyFormatting(char[] fullData, int pos, char current, char next) {
    return (current == COLOR_CHAR || current == LEGACY_CHAR)
      && ((next >= '0' && next <= '9')
      || (next >= 'a' && next <= 'f')
      || (next >= 'k' && next <= 'o')
      || next == 'r');
  }

  @Override
  public @NonNull <T> ComponentConverter<T> converterTo(@NonNull ComponentFormat<T> targetFormat) {
    return new ComponentConverter<>(this, targetFormat);
  }
}
