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

package de.dytanic.cloudnet.driver.network.rpc.defaults.object;

import com.google.common.base.Verify;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectMapper;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectSerializer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CollectionObjectSerializer implements ObjectSerializer<Collection<?>> {

  private final Supplier<Collection<?>> collectionFactory;

  protected CollectionObjectSerializer(Supplier<Collection<?>> collectionFactory) {
    this.collectionFactory = collectionFactory;
  }

  public static @NotNull CollectionObjectSerializer of(@NotNull Supplier<Collection<?>> collectionFactory) {
    return new CollectionObjectSerializer(collectionFactory);
  }

  @Override
  public @Nullable Collection<?> read(
    @NotNull DataBuf source,
    @NotNull Type type,
    @NotNull ObjectMapper caller
  ) {
    // create a new instance of the collection
    Collection<?> collection = this.collectionFactory.get();
    // read the size of the collection
    int collectionSize = source.readInt();
    // if the collection is empty, break
    if (collectionSize == 0) {
      return collection;
    }
    // ensure that the type is parameterized
    Verify.verify(type instanceof ParameterizedType, "Collection rpc read called without parameterized type");
    // read the parameter type of the collection
    Type parameterType = ((ParameterizedType) type).getActualTypeArguments()[0];
    // read the collection content
    for (int i = 0; i < collectionSize; i++) {
      collection.add(caller.readObject(source, parameterType));
    }
    // read done
    return collection;
  }

  @Override
  public void write(
    @NotNull DataBuf.Mutable dataBuf,
    @Nullable Collection<?> object,
    @NotNull Type type,
    @NotNull ObjectMapper caller
  ) {
    dataBuf.writeInt(object == null ? 0 : object.size());
    if (object != null) {
      for (Object o : object) {
        caller.writeObject(dataBuf, o);
      }
    }
  }
}
