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

package eu.cloudnetservice.cloudnet.driver.network.rpc.defaults.object.serializers;

import com.google.common.base.Verify;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.network.rpc.object.ObjectMapper;
import eu.cloudnetservice.cloudnet.driver.network.rpc.object.ObjectSerializer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class MapObjectSerializer implements ObjectSerializer<Map<?, ?>> {

  private final Supplier<Map<?, ?>> mapFactory;

  protected MapObjectSerializer(Supplier<Map<?, ?>> mapFactory) {
    this.mapFactory = mapFactory;
  }

  public static @NonNull MapObjectSerializer of(@NonNull Supplier<Map<?, ?>> mapFactory) {
    return new MapObjectSerializer(mapFactory);
  }

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
    Verify.verify(type instanceof ParameterizedType, "Map rpc read called without parameterized type");
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
