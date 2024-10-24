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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.buffer.DataBufable;
import eu.cloudnetservice.driver.network.object.ObjectMapper;
import eu.cloudnetservice.driver.network.object.ObjectSerializer;
import eu.cloudnetservice.utils.base.ClassAllocationUtil;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.function.Supplier;
import lombok.NonNull;

/**
 * An object codec implementation for all classes that implement {@link DataBufable}.
 *
 * @since 4.0
 */
public final class DataBufableObjectSerializer implements ObjectSerializer<DataBufable> {

  private final Cache<Class<?>, Supplier<Object>> cachedClassAllocators = Caffeine.newBuilder()
    .expireAfterAccess(Duration.ofHours(6))
    .build();

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DataBufable read(
    @NonNull DataBuf source,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    if (!(type instanceof Class<?> targetClass)) {
      throw new IllegalArgumentException("target type must be class");
    }

    // allocate an instance of the class & read the data from the given data buf
    var classAllocator = this.cachedClassAllocators.get(targetClass, ClassAllocationUtil::makeInstanceFactory);
    var allocatedInstance = (DataBufable) classAllocator.get();
    allocatedInstance.readData(source);
    return allocatedInstance;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(
    @NonNull DataBuf.Mutable dataBuf,
    @NonNull DataBufable object,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    object.writeData(dataBuf);
  }
}
