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

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.buffer.DataBufable;
import de.dytanic.cloudnet.driver.network.rpc.defaults.object.data.DataClassSerializer;
import de.dytanic.cloudnet.driver.network.rpc.exception.MissingObjectSerializerException;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectMapper;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectSerializer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultObjectMapper implements ObjectMapper {

  private static final Map<Type, ObjectSerializer<?>> DEFAULT_SERIALIZERS = ImmutableMap.<Type, ObjectSerializer<?>>builder()
    //    ==== primitive types ====
    // boolean
    .put(boolean.class, FunctionalObjectSerializer.of(DataBuf::readBoolean, DataBuf.Mutable::writeBoolean))
    .put(Boolean.class, FunctionalObjectSerializer.of(DataBuf::readBoolean, DataBuf.Mutable::writeBoolean))
    // byte
    .put(byte.class, FunctionalObjectSerializer.of(DataBuf::readByte, DataBuf.Mutable::writeByte))
    .put(Byte.class, FunctionalObjectSerializer.of(DataBuf::readByte, DataBuf.Mutable::writeByte))
    // short
    .put(short.class, FunctionalObjectSerializer.of(DataBuf::readShort, DataBuf.Mutable::writeShort))
    .put(Short.class, FunctionalObjectSerializer.of(DataBuf::readShort, DataBuf.Mutable::writeShort))
    // integer
    .put(int.class, FunctionalObjectSerializer.of(DataBuf::readInt, DataBuf.Mutable::writeInt))
    .put(Integer.class, FunctionalObjectSerializer.of(DataBuf::readInt, DataBuf.Mutable::writeInt))
    // long
    .put(long.class, FunctionalObjectSerializer.of(DataBuf::readLong, DataBuf.Mutable::writeLong))
    .put(Long.class, FunctionalObjectSerializer.of(DataBuf::readLong, DataBuf.Mutable::writeLong))
    // float
    .put(float.class, FunctionalObjectSerializer.of(DataBuf::readFloat, DataBuf.Mutable::writeFloat))
    .put(Float.class, FunctionalObjectSerializer.of(DataBuf::readFloat, DataBuf.Mutable::writeFloat))
    // double
    .put(double.class, FunctionalObjectSerializer.of(DataBuf::readDouble, DataBuf.Mutable::writeDouble))
    .put(Double.class, FunctionalObjectSerializer.of(DataBuf::readDouble, DataBuf.Mutable::writeDouble))
    // char
    .put(char.class, FunctionalObjectSerializer.of(DataBuf::readChar, DataBuf.Mutable::writeChar))
    .put(Character.class, FunctionalObjectSerializer.of(DataBuf::readChar, DataBuf.Mutable::writeChar))
    // string
    .put(String.class, FunctionalObjectSerializer.of(DataBuf::readString, DataBuf.Mutable::writeString))
    //    ==== nested types ====
    // optional
    .put(Optional.class, new OptionalObjectSerializer())
    // some collection types, collection is always mapped to list
    .put(Collection.class, CollectionObjectSerializer.of(ArrayList::new))
    .put(List.class, CollectionObjectSerializer.of(ArrayList::new))
    .put(Set.class, CollectionObjectSerializer.of(HashSet::new))
    .put(Vector.class, CollectionObjectSerializer.of(Vector::new))
    // some map types, map is always mapped to HashMap
    .put(Map.class, MapObjectSerializer.of(HashMap::new))
    .put(ConcurrentMap.class, MapObjectSerializer.of(ConcurrentHashMap::new))
    .put(SortedMap.class, MapObjectSerializer.of(TreeMap::new))
    //    ==== object data class types ====
    // data classes
    .put(DataBufable.class, new DataBufableObjectSerializer())
    .put(Object.class, new DataClassSerializer())
    .build();

  private final Map<Type, TypeToken<?>> typeTokenCache = new ConcurrentHashMap<>();
  private final Map<Type, ObjectSerializer<?>> registeredSerializers = new ConcurrentHashMap<>();

  public DefaultObjectMapper() {
    this(true);
  }

  public DefaultObjectMapper(boolean registerDefaultSerializers) {
    if (registerDefaultSerializers) {
      this.registeredSerializers.putAll(DEFAULT_SERIALIZERS);
    }
  }

  @Override
  public @NotNull ObjectMapper unregisterBinding(@NotNull Type type, boolean superTypes) {
    if (superTypes) {
      TypeToken<?> typeToken = this.typeTokenCache.computeIfAbsent(type, TypeToken::of);
      // unregister all sub-types of the type
      for (TypeToken<?> subType : typeToken.getTypes()) {
        this.registeredSerializers.remove(subType.getType());
        this.registeredSerializers.remove(subType.getRawType());
      }
    } else {
      // we don't need to unregister the sub-types of the type, skip the lookup
      this.registeredSerializers.remove(type);
    }
    return this;
  }

  @Override
  public @NotNull <T> ObjectMapper registerBinding(
    @NotNull Type type,
    @NotNull ObjectSerializer<T> serializer,
    boolean superTypes
  ) {
    if (superTypes) {
      TypeToken<?> typeToken = this.typeTokenCache.computeIfAbsent(type, TypeToken::of);
      // register all sub-types of the type
      for (TypeToken<?> token : typeToken.getTypes()) {
        this.registeredSerializers.putIfAbsent(token.getType(), serializer);
        this.registeredSerializers.putIfAbsent(token.getRawType(), serializer);
      }
    } else {
      // we don't need to register the sub-types of the type, skip the lookup
      this.registeredSerializers.putIfAbsent(type, serializer);
    }
    return this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public @NotNull DataBuf.Mutable writeObject(@NotNull DataBuf.Mutable dataBuf, @Nullable Object object) {
    return dataBuf.writeNullable(object, (buffer, obj) -> {
      // Get the type token of the type
      TypeToken<?> typeToken = this.typeTokenCache.computeIfAbsent(obj.getClass(), TypeToken::of);
      // get the registered serializer for the type
      ObjectSerializer<?> serializer = null;
      for (TypeToken<?> type : typeToken.getTypes()) {
        serializer = this.getSerializerForType(type);
        if (serializer != null) {
          break;
        }
      }
      // check if a serializer was found
      if (serializer == null) {
        throw new MissingObjectSerializerException(obj.getClass());
      }
      // serialize the object into the buffer
      ((ObjectSerializer<Object>) serializer).write(buffer, obj, obj.getClass(), this);
    });
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> @Nullable T readObject(@NotNull DataBuf dataBuf, @NotNull Type type) {
    return dataBuf.readNullable(buffer -> {
      // Get the type token of the type
      TypeToken<?> typeToken = this.typeTokenCache.computeIfAbsent(type, TypeToken::of);
      // get the registered serializer for the type
      ObjectSerializer<?> serializer = null;
      for (TypeToken<?> subType : typeToken.getTypes()) {
        serializer = this.getSerializerForType(subType);
        if (serializer != null) {
          break;
        }
      }
      // check if a serializer was found
      if (serializer == null) {
        throw new MissingObjectSerializerException(type);
      }
      // read the object from the buffer
      return ((ObjectSerializer<T>) serializer).read(buffer, type, this);
    });
  }

  protected @Nullable ObjectSerializer<?> getSerializerForType(@NotNull TypeToken<?> typeToken) {
    ObjectSerializer<?> byType = this.registeredSerializers.get(typeToken.getType());
    return byType == null ? this.registeredSerializers.get(typeToken.getRawType()) : byType;
  }
}
