/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.driver.service.property;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.cloudnet.driver.service.ServiceInfoSnapshot;
import java.lang.reflect.Type;
import java.util.Optional;
import lombok.NonNull;

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

  @NonNull
  public static <T> DefaultJsonServiceProperty<T> createFromClass(@NonNull String key, @NonNull Class<T> classType) {
    return new DefaultJsonServiceProperty<>(key, null, classType);
  }

  @NonNull
  public static <T> DefaultJsonServiceProperty<T> createFromType(@NonNull String key, @NonNull Type type) {
    return createFromType(key, type, false);
  }

  @NonNull
  public static <T> DefaultJsonServiceProperty<T> createFromType(
    @NonNull String key,
    @NonNull Type type,
    boolean forbidModifications
  ) {
    var property = new DefaultJsonServiceProperty<T>(key, type, null);
    property.allowModifications = !forbidModifications;
    return property;
  }

  public DefaultJsonServiceProperty<T> forbidModification() {
    this.allowModifications = false;
    return this;
  }

  @NonNull
  @Override
  public Optional<T> read(@NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
    if (!serviceInfoSnapshot.properties().contains(this.key)) {
      return Optional.empty();
    }

    return Optional.ofNullable(this.type != null
      ? serviceInfoSnapshot.properties().get(this.key, this.type)
      : serviceInfoSnapshot.properties().get(this.key, this.classType));
  }

  @Override
  public void write(@NonNull ServiceInfoSnapshot serviceInfoSnapshot, T value) {
    Preconditions.checkArgument(this.allowModifications, "This property doesn't support modifying the value");
    serviceInfoSnapshot.properties().append(this.key, value);
  }
}
