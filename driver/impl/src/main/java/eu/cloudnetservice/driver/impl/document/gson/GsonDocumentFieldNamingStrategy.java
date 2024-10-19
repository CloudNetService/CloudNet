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

import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;
import eu.cloudnetservice.driver.document.annotations.DocumentFieldRename;
import java.lang.reflect.Field;
import lombok.NonNull;

/**
 * A field naming strategy implementation for gson to take the {@link DocumentFieldRename} annotation into account.
 *
 * @since 4.0
 */
final class GsonDocumentFieldNamingStrategy implements FieldNamingStrategy {

  /**
   * The jvm static instance of this strategy. There is no requirement to create multiple instances as each instance is
   * implemented thread safe.
   */
  public static final GsonDocumentFieldNamingStrategy INSTANCE = new GsonDocumentFieldNamingStrategy();

  /**
   * Sealed constructor for this naming strategy to prevent accidental instantiations. Use the jvm static singleton
   * instance instead.
   */
  private GsonDocumentFieldNamingStrategy() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String translateName(@NonNull Field field) {
    // check if the rename annotation is explicitly given to the field
    var renameAnnotation = field.getAnnotation(DocumentFieldRename.class);
    if (renameAnnotation != null) {
      return renameAnnotation.value();
    }

    // Fallback to the default field naming policy.
    // This policy value is taken from the field Gson.DEFAULT_FIELD_NAMING_STRATEGY
    return FieldNamingPolicy.IDENTITY.translateName(field);
  }
}
