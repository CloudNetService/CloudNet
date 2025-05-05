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

import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.object.ObjectMapper;
import eu.cloudnetservice.driver.network.object.ObjectSerializer;
import io.leangen.geantyref.GenericTypeReflector;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.function.IntFunction;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * An object serializer which can read and write any type of collection to/from a buffer.
 *
 * @since 4.0
 */
public final class CollectionObjectSerializer implements ObjectSerializer<Collection<?>> {

  private final IntFunction<Collection<Object>> collectionFactory;

  /**
   * Constructs a new collection object serializer instance.
   *
   * @param collectionFactory the factory to create the underlying collection instance.
   * @throws NullPointerException if the given factory is null.
   */
  private CollectionObjectSerializer(@NonNull IntFunction<Collection<Object>> collectionFactory) {
    this.collectionFactory = collectionFactory;
  }

  /**
   * Constructs a new collection object serializer instance.
   *
   * @param collectionFactory the factory to create the collection instance, receiving the site as parameter.
   * @return the collection factory instance.
   * @throws NullPointerException if the given factory is null.
   */
  public static @NonNull CollectionObjectSerializer of(@NonNull IntFunction<Collection<Object>> collectionFactory) {
    return new CollectionObjectSerializer(collectionFactory);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Collection<?> read(
    @NonNull DataBuf source,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    if (!(type instanceof ParameterizedType pt) || !GenericTypeReflector.isFullyBound(pt)) {
      // not a parameterized type or bounds are not tight enough to deserialize (e.g. Collection<?>)
      throw new IllegalArgumentException(
        String.format("expected fully bound parameterized type to deserialize Collection, got %s", type));
    }

    var typeArguments = pt.getActualTypeArguments();
    if (typeArguments.length != 1) {
      // must have one type argument to deserialize
      throw new IllegalArgumentException(
        String.format("expected 1 type argument to deserialize Collection, got %d", typeArguments.length));
    }

    // reconstruct the map data
    var collectionSize = source.readInt();
    var collection = this.collectionFactory.apply(collectionSize);
    for (var index = 0; index < collectionSize; index++) {
      var entry = caller.readObject(source, typeArguments[0]);
      collection.add(entry);
    }

    return collection;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(
    @NonNull DataBuf.Mutable dataBuf,
    @NonNull Collection<?> object,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    dataBuf.writeInt(object.size());
    for (var entry : object) {
      caller.writeObject(dataBuf, entry);
    }
  }
}
