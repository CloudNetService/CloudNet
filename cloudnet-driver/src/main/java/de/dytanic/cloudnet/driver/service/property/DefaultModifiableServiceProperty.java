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
import java.util.Optional;
import java.util.function.BiFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultModifiableServiceProperty<I, T> implements ServiceProperty<T> {

  private final ServiceProperty<I> wrapped;
  private BiFunction<ServiceInfoSnapshot, I, T> getModifier;
  private BiFunction<ServiceInfoSnapshot, T, I> setModifier;

  private DefaultModifiableServiceProperty(ServiceProperty<I> wrapped) {
    this.wrapped = wrapped;
  }

  public static <I, T> DefaultModifiableServiceProperty<I, T> wrap(ServiceProperty<I> wrapped) {
    return new DefaultModifiableServiceProperty<>(wrapped);
  }

  public DefaultModifiableServiceProperty<I, T> modifyGet(BiFunction<ServiceInfoSnapshot, I, T> getModifier) {
    this.getModifier = getModifier;
    return this;
  }

  public DefaultModifiableServiceProperty<I, T> modifySet(BiFunction<ServiceInfoSnapshot, T, I> setModifier) {
    this.setModifier = setModifier;
    return this;
  }

  @NotNull
  @Override
  public Optional<T> get(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
    Preconditions.checkNotNull(this.getModifier, "This property doesn't support getting a value");
    return this.wrapped.get(serviceInfoSnapshot).map(i -> this.getModifier.apply(serviceInfoSnapshot, i));
  }

  @Override
  public void set(@NotNull ServiceInfoSnapshot serviceInfoSnapshot, @Nullable T value) {
    Preconditions.checkNotNull(this.setModifier, "This property doesn't support modifying a value");
    this.wrapped.set(serviceInfoSnapshot, this.setModifier.apply(serviceInfoSnapshot, value));
  }
}
