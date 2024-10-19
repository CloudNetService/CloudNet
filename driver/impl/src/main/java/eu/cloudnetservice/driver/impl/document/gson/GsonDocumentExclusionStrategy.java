/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.impl.document.gson;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import eu.cloudnetservice.driver.document.annotations.DocumentValueIgnore;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * An exclusion strategy implementation that takes the {@link DocumentValueIgnore} annotation into account.
 *
 * @since 4.0
 */
final class GsonDocumentExclusionStrategy implements ExclusionStrategy {

  /**
   * The jvm static exclusion strategy for the serialize direction.
   */
  public static final GsonDocumentExclusionStrategy SERIALIZE =
    new GsonDocumentExclusionStrategy(DocumentValueIgnore.Direction.SERIALIZE);

  /**
   * The jvm static exclusion strategy for the deserialize direction.
   */
  public static final GsonDocumentExclusionStrategy DESERIALIZE =
    new GsonDocumentExclusionStrategy(DocumentValueIgnore.Direction.DESERIALIZE);

  private final DocumentValueIgnore.Direction direction;

  /**
   * Constructs a new exclusion strategy that ignores members that are annotated with {@link DocumentValueIgnore} and
   * exclude the given direction.
   *
   * @param direction the serialize direction of members to exclude.
   * @throws NullPointerException if the given direction is null.
   */
  private GsonDocumentExclusionStrategy(@NonNull DocumentValueIgnore.Direction direction) {
    this.direction = direction;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean shouldSkipField(@NonNull FieldAttributes field) {
    var ignoreAnnotation = field.getAnnotation(DocumentValueIgnore.class);
    return this.shouldExclude(ignoreAnnotation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean shouldSkipClass(@NonNull Class<?> clazz) {
    var ignoreAnnotation = clazz.getAnnotation(DocumentValueIgnore.class);
    return this.shouldExclude(ignoreAnnotation);
  }

  /**
   * Check if the given annotation instance is present and disables the serialisation into the direction handled by this
   * exclusion strategy.
   *
   * @param ignoreAnnotation the optional ignore annotation instance to validate.
   * @return true if the member should be excluded from the target direction, false otherwise.
   */
  private boolean shouldExclude(@Nullable DocumentValueIgnore ignoreAnnotation) {
    // include the field if the annotation is missing
    if (ignoreAnnotation == null) {
      return false;
    }

    // check if the annotation value has our direction enabled
    var directions = ignoreAnnotation.value();
    for (var excludedDirection : directions) {
      if (excludedDirection == this.direction) {
        return true;
      }
    }

    return false;
  }
}
