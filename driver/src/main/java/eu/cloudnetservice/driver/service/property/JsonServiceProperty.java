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

package eu.cloudnetservice.driver.service.property;

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import java.lang.reflect.Type;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a service property which writes the value of it directly into the json document of the target service info
 * snapshot based on the given type.
 *
 * @param <T> the type of the property.
 * @since 4.0
 */
public final class JsonServiceProperty<T> implements ServiceProperty<T> {

  private final String key;
  private final Type type;

  private boolean allowModifications = true;

  /**
   * Constructs a new default json service property instance.
   *
   * @param key  the key of the property to use when writing/reading to the service properties.
   * @param type the type of property which gets written / read from the service properties.
   * @throws NullPointerException if the given key or type is null.
   */
  private JsonServiceProperty(@NonNull String key, @NonNull Type type) {
    this.key = key;
    this.type = type;
  }

  /**
   * Creates a new json service property based on the given key and class type.
   *
   * @param key       the key of the property to use when writing / reading to the service properties.
   * @param classType the type of property which gets written / read from the service properties.
   * @param <T>       the type of the class object being written to the properties.
   * @return a new json service property writing the given class type with the given key into the service properties.
   * @throws NullPointerException if the given key or class type is null.
   */
  @NonNull
  public static <T> JsonServiceProperty<T> createFromClass(@NonNull String key, @NonNull Class<T> classType) {
    return new JsonServiceProperty<>(key, classType);
  }

  /**
   * Creates a new json service property based on the given key and type.
   *
   * @param key  the key of the property to use when writing / reading to the service properties.
   * @param type the type of property which gets written / read from the service properties.
   * @param <T>  the type of the object being written to the properties.
   * @return a new json service property writing the given type with the given key into the service properties.
   * @throws NullPointerException if the given key or type is null.
   */
  @NonNull
  public static <T> JsonServiceProperty<T> createFromType(@NonNull String key, @NonNull Type type) {
    return new JsonServiceProperty<>(key, type);
  }

  /**
   * Disables the ability of users to write to this property. All write calls after this method call will result in an
   * exception and not modify the service properties.
   *
   * @return the same instance as used to call the method, for chaining.
   */
  public @NonNull JsonServiceProperty<T> forbidModification() {
    this.allowModifications = false;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable T read(@NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
    return serviceInfoSnapshot.properties().get(this.key, this.type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(@NonNull ServiceInfoSnapshot serviceInfoSnapshot, T value) {
    if (!this.allowModifications) {
      throw new UnsupportedOperationException("Writing is not supported for this property");
    }

    serviceInfoSnapshot.properties().append(this.key, value);
  }
}
