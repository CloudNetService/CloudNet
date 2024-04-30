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

package eu.cloudnetservice.driver.network.rpc.defaults.object.serializers;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.rpc.object.ObjectMapper;
import eu.cloudnetservice.driver.network.rpc.object.ObjectSerializer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * An object serializer which can de- serialize any kind of map from/to a buffer.
 *
 * @since 4.0
 */
public class MapObjectSerializer implements ObjectSerializer<Map<?, ?>> {

  private final Supplier<Map<?, ?>> mapFactory;

  /**
   * Constructs a new map object serializer instance.
   *
   * @param mapFactory the map factory to use to construct the map instances.
   * @throws NullPointerException if the given map factory is null.
   */
  protected MapObjectSerializer(@NonNull Supplier<Map<?, ?>> mapFactory) {
    this.mapFactory = mapFactory;
  }

  /**
   * Constructs a new map object serializer instance.
   *
   * @param mapFactory the map factory to use to construct the map instances.
   * @return the created map object serializer instance.
   * @throws NullPointerException if the given map factory is null.
   */
  public static @NonNull MapObjectSerializer of(@NonNull Supplier<Map<?, ?>> mapFactory) {
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
    // create a new instance of the map
    var map = this.mapFactory.get();
    // read the size of the map
    var mapSize = source.readInt();
    // if the map is empty, break
    if (mapSize == 0) {
      return map;
    }
    // ensure that the type is parameterized
    Preconditions.checkState(type instanceof ParameterizedType, "Map rpc read called without parameterized type");
    // read the parameter type of the collection
    var keyType = ((ParameterizedType) type).getActualTypeArguments()[0];
    var valueType = ((ParameterizedType) type).getActualTypeArguments()[1];
    // read the map content
    for (var i = 0; i < mapSize; i++) {
      map.put(caller.readObject(source, keyType), caller.readObject(source, valueType));
    }
    // read done
    return map;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(
    @NonNull DataBuf.Mutable dataBuf,
    @Nullable Map<?, ?> object,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    dataBuf.writeInt(object == null ? 0 : object.size());
    if (object != null) {
      for (Map.Entry<?, ?> entry : object.entrySet()) {
        caller.writeObject(dataBuf, entry.getKey());
        caller.writeObject(dataBuf, entry.getValue());
      }
    }
  }
}
