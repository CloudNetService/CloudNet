/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.driver.service.property;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import java.lang.reflect.Type;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public class DefaultJsonServiceProperty<T> implements ServiceProperty<T> {

  private final String key;
  private final Type type;
  private final Class<T> classType;

  private boolean allowModifications = true;

  private DefaultJsonServiceProperty(String key, Type type, Class<T> classType) {
    this.key = key;
    this.type = type;
    this.classType = classType;
  }

  @NotNull
  public static <T> DefaultJsonServiceProperty<T> createFromClass(@NotNull String key, @NotNull Class<T> classType) {
    return new DefaultJsonServiceProperty<>(key, null, classType);
  }

  @NotNull
  public static <T> DefaultJsonServiceProperty<T> createFromType(@NotNull String key, @NotNull Type type) {
    return createFromType(key, type, false);
  }

  @NotNull
  public static <T> DefaultJsonServiceProperty<T> createFromType(@NotNull String key, @NotNull Type type,
    boolean forbidModifications) {
    DefaultJsonServiceProperty<T> property = new DefaultJsonServiceProperty<>(key, type, null);
    if (forbidModifications) {
      property.forbidModification();
    }
    return property;
  }

  public DefaultJsonServiceProperty<T> forbidModification() {
    this.allowModifications = false;
    return this;
  }

  @NotNull
  @Override
  public Optional<T> get(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
    if (!serviceInfoSnapshot.getProperties().contains(this.key)) {
      return Optional.empty();
    }
    return Optional.ofNullable(this.type != null ? serviceInfoSnapshot.getProperties().get(this.key, this.type)
      : serviceInfoSnapshot.getProperties().get(this.key, this.classType));
  }

  @Override
  public void set(@NotNull ServiceInfoSnapshot serviceInfoSnapshot, T value) {
    Preconditions.checkArgument(this.allowModifications, "This property doesn't support modifying the value");
    serviceInfoSnapshot.getProperties().append(this.key, value);
  }
}
