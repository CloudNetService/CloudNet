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

package eu.cloudnetservice.common.document.property;

import eu.cloudnetservice.common.document.gson.JsonDocument;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

@ToString
@EqualsAndHashCode
public class JsonDocPropertyHolder implements DocPropertyHolder {

  protected final JsonDocument properties;

  protected JsonDocPropertyHolder(@NonNull JsonDocument properties) {
    this.properties = properties;
  }

  @Override
  public @NonNull <E> DocPropertyHolder property(@NonNull DocProperty<E> docProperty, @Nullable E val) {
    docProperty.append(this.properties, val);
    return this;
  }

  @Override
  public <E> @UnknownNullability E property(@NonNull DocProperty<E> docProperty) {
    return docProperty.get(this.properties);
  }

  @Override
  public @NonNull <E> DocPropertyHolder removeProperty(@NonNull DocProperty<E> docProperty) {
    docProperty.remove(this.properties);
    return this;
  }

  @Override
  public <E> boolean hasProperty(@NonNull DocProperty<E> docProperty) {
    return docProperty.isAppendedTo(this.properties);
  }

  @Override
  public @NonNull JsonDocument properties() {
    return this.properties;
  }
}
