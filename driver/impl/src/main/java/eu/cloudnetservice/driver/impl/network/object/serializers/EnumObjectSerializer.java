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

package eu.cloudnetservice.driver.impl.network.object.serializers;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.object.ObjectMapper;
import eu.cloudnetservice.driver.network.object.ObjectSerializer;
import java.lang.reflect.Type;
import java.time.Duration;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A serializer to write enum constants to a data buf.
 *
 * @since 4.0
 */
public final class EnumObjectSerializer implements ObjectSerializer<Enum<?>> {

  private final LoadingCache<Class<?>, Object[]> enumConstantCache = Caffeine.newBuilder()
    .expireAfterAccess(Duration.ofHours(8))
    .build(Class::getEnumConstants);

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Enum<?> read(
    @NonNull DataBuf source,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    if (!(type instanceof Class<?> clazz) || !clazz.isEnum()) {
      throw new IllegalArgumentException("enum serializer called with non-enum type");
    }

    // get the index of the enum constant & the actual constants of the enum class
    var enumConstantIndex = source.readInt();
    var enumConstants = this.enumConstantCache.get(clazz);
    return enumConstantIndex >= enumConstants.length ? null : (Enum<?>) enumConstants[enumConstantIndex];
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(
    @NonNull DataBuf.Mutable dataBuf,
    @NonNull Enum<?> object,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    dataBuf.writeInt(object.ordinal());
  }
}
