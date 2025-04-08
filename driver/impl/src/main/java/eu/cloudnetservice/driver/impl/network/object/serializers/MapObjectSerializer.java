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
import java.util.Map;
import java.util.function.IntFunction;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * An object serializer which can de- serialize any kind of map from/to a buffer.
 *
 * @since 4.0
 */
public final class MapObjectSerializer implements ObjectSerializer<Map<?, ?>> {

  private final IntFunction<Map<Object, Object>> mapFactory;

  /**
   * Constructs a new map object serializer instance.
   *
   * @param mapFactory the map factory to use to construct the map instances.
   * @throws NullPointerException if the given map factory is null.
   */
  private MapObjectSerializer(@NonNull IntFunction<Map<Object, Object>> mapFactory) {
    this.mapFactory = mapFactory;
  }

  /**
   * Constructs a new map object serializer instance.
   *
   * @param mapFactory the map factory to use to construct the map instances, receiving the map size as parameter.
   * @return the created map object serializer instance.
   * @throws NullPointerException if the given map factory is null.
   */
  public static @NonNull MapObjectSerializer of(@NonNull IntFunction<Map<Object, Object>> mapFactory) {
    return new MapObjectSerializer(mapFactory);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Map<?, ?> read(
    @NonNull DataBuf source,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    if (!(type instanceof ParameterizedType pt) || !GenericTypeReflector.isFullyBound(pt)) {
      // not a parameterized type or bounds are not tight enough to deserialize (e.g. Map<?, ?>)
      throw new IllegalArgumentException(
        String.format("expected fully bound parameterized type to deserialize Map, got %s", type));
    }

    var typeArguments = pt.getActualTypeArguments();
    if (typeArguments.length != 2) {
      // must have two type arguments to deserialize
      throw new IllegalArgumentException(
        String.format("expected 2 type arguments to deserialize Map, got %d", typeArguments.length));
    }

    // reconstruct the map data
    var mapSize = source.readInt();
    var mapInstance = this.mapFactory.apply(mapSize);
    for (var index = 0; index < mapSize; index++) {
      var entryKey = caller.readObject(source, typeArguments[0]);
      var entryValue = caller.readObject(source, typeArguments[1]);
      mapInstance.put(entryKey, entryValue);
    }

    return mapInstance;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(
    @NonNull DataBuf.Mutable dataBuf,
    @NonNull Map<?, ?> object,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    dataBuf.writeInt(object.size());
    for (var entry : object.entrySet()) {
      caller.writeObject(dataBuf, entry.getKey());
      caller.writeObject(dataBuf, entry.getValue());
    }
  }
}
