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

public interface ComponentFormat<C> {

  int hexSegmentLength();

  char hexIndicationChar();

  char colorIndicationChar();

  boolean usesColorCharAsHexDelimiter();

  boolean charIsValidColorIndicationChar(char[] fullData, int pos, char current);

  boolean nextSegmentIsLegacyFormatting(char[] fullData, int pos, char current, char next);

  boolean nextSegmentIsHexadecimalFormatting(char[] fullData, int pos, char current, char next);

  @NonNull C encodeStringToComponent(@NonNull String text);

  @NonNull <T> ComponentConverter<T> converterTo(@NonNull ComponentFormat<T> targetFormat);
}
