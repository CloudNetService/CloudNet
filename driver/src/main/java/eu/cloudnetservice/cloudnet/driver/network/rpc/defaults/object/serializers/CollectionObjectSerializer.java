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
import java.util.Collection;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class CollectionObjectSerializer implements ObjectSerializer<Collection<?>> {

  private final Supplier<Collection<?>> collectionFactory;

  protected CollectionObjectSerializer(Supplier<Collection<?>> collectionFactory) {
    this.collectionFactory = collectionFactory;
  }

  public static @NonNull CollectionObjectSerializer of(@NonNull Supplier<Collection<?>> collectionFactory) {
    return new CollectionObjectSerializer(collectionFactory);
  }

  @Override
  public @Nullable Collection<?> read(
    @NonNull DataBuf source,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    // create a new instance of the collection
    var collection = this.collectionFactory.get();
    // read the size of the collection
    var collectionSize = source.readInt();
    // if the collection is empty, break
    if (collectionSize == 0) {
      return collection;
    }
    // ensure that the type is parameterized
    Verify.verify(type instanceof ParameterizedType, "Collection rpc read called without parameterized type");
    // read the parameter type of the collection
    var parameterType = ((ParameterizedType) type).getActualTypeArguments()[0];
    // read the collection content
    for (var i = 0; i < collectionSize; i++) {
      collection.add(caller.readObject(source, parameterType));
    }
    // read done
    return collection;
  }

  @Override
  public void write(
    @NonNull DataBuf.Mutable dataBuf,
    @Nullable Collection<?> object,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    dataBuf.writeInt(object == null ? 0 : object.size());
    if (object != null) {
      for (Object o : object) {
        caller.writeObject(dataBuf, o);
      }
    }
  }
}
