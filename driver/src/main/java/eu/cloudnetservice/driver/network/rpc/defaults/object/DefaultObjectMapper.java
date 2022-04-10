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

package eu.cloudnetservice.driver.network.rpc.defaults.object;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.buffer.DataBufable;
import eu.cloudnetservice.driver.network.rpc.defaults.object.data.DataClassSerializer;
import eu.cloudnetservice.driver.network.rpc.defaults.object.serializers.CollectionObjectSerializer;
import eu.cloudnetservice.driver.network.rpc.defaults.object.serializers.DataBufObjectSerializer;
import eu.cloudnetservice.driver.network.rpc.defaults.object.serializers.DataBufableObjectSerializer;
import eu.cloudnetservice.driver.network.rpc.defaults.object.serializers.EnumObjectSerializer;
import eu.cloudnetservice.driver.network.rpc.defaults.object.serializers.FunctionalObjectSerializer;
import eu.cloudnetservice.driver.network.rpc.defaults.object.serializers.JsonDocumentObjectSerializer;
import eu.cloudnetservice.driver.network.rpc.defaults.object.serializers.MapObjectSerializer;
import eu.cloudnetservice.driver.network.rpc.defaults.object.serializers.OptionalObjectSerializer;
import eu.cloudnetservice.driver.network.rpc.defaults.object.serializers.PathObjectSerializer;
import eu.cloudnetservice.driver.network.rpc.defaults.object.serializers.UUIDObjectSerializer;
import eu.cloudnetservice.driver.network.rpc.exception.MissingObjectSerializerException;
import eu.cloudnetservice.driver.network.rpc.object.ObjectMapper;
import eu.cloudnetservice.driver.network.rpc.object.ObjectSerializer;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingDeque;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default object mapper implementation.
 *
 * @since 4.0
 */
public class DefaultObjectMapper implements ObjectMapper {

  public static final ObjectMapper DEFAULT_MAPPER;
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
    // special case for byte arrays, just write them directly
    .put(byte[].class, FunctionalObjectSerializer.of(DataBuf::readByteArray, DataBuf.Mutable::writeByteArray))
    // uuid
    .put(UUID.class, new UUIDObjectSerializer())
    //    ==== nested types ====
    // optional
    .put(Optional.class, new OptionalObjectSerializer())
    // some collection types, collection is always mapped to list
    .put(Collection.class, CollectionObjectSerializer.of(ArrayList::new))
    .put(List.class, CollectionObjectSerializer.of(ArrayList::new))
    .put(Set.class, CollectionObjectSerializer.of(HashSet::new))
    .put(NavigableSet.class, CollectionObjectSerializer.of(TreeSet::new))
    .put(Vector.class, CollectionObjectSerializer.of(Vector::new))
    .put(Queue.class, CollectionObjectSerializer.of(LinkedList::new))
    .put(BlockingQueue.class, CollectionObjectSerializer.of(LinkedBlockingDeque::new))
    // some map types, map is always mapped to HashMap
    .put(Map.class, MapObjectSerializer.of(HashMap::new))
    .put(ConcurrentMap.class, MapObjectSerializer.of(ConcurrentHashMap::new))
    .put(NavigableMap.class, MapObjectSerializer.of(TreeMap::new))
    .put(ConcurrentNavigableMap.class, MapObjectSerializer.of(ConcurrentSkipListMap::new))
    //    ==== object data class types ====
    // data classes
    .put(Path.class, new PathObjectSerializer())
    .put(DataBuf.class, new DataBufObjectSerializer())
    .put(DataBufable.class, new DataBufableObjectSerializer())
    .put(JsonDocument.class, new JsonDocumentObjectSerializer())
    .put(Enum.class, new EnumObjectSerializer())
    .put(Object.class, new DataClassSerializer())
    .build();

  static {
    // This is required as IJ wants the field ABOVE the map with the default types which results in an error
    DEFAULT_MAPPER = new DefaultObjectMapper();
  }

  private final Map<Type, TypeToken<?>> typeTokenCache = new ConcurrentHashMap<>();
  private final Map<Type, ObjectSerializer<?>> registeredSerializers = new ConcurrentHashMap<>();

  /**
   * Constructs a new default object mapper instance with all default object serializers already registered. This call
   * is equivalent to {@code new DefaultObjectMapper(true)}.
   */
  public DefaultObjectMapper() {
    this(true);
  }

  /**
   * Constructs a new default object mapper instance and optionally registers the default serializers to it.
   *
   * @param registerDefaultSerializers true if the default serializers should get registered, false otherwise.
   */
  public DefaultObjectMapper(boolean registerDefaultSerializers) {
    if (registerDefaultSerializers) {
      this.registeredSerializers.putAll(DEFAULT_SERIALIZERS);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ObjectMapper unregisterBinding(@NonNull Type type, boolean superTypes) {
    if (superTypes) {
      var typeToken = this.typeTokenCache.computeIfAbsent(type, TypeToken::of);
      // unregister all subtypes of the type
      for (TypeToken<?> subType : typeToken.getTypes()) {
        this.registeredSerializers.remove(subType.getType());
        this.registeredSerializers.remove(subType.getRawType());
      }
    } else {
      // we don't need to unregister the subtypes of the type, skip the lookup
      this.registeredSerializers.remove(type);
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ObjectMapper unregisterBindings(@NonNull ClassLoader classLoader) {
    for (var entry : this.registeredSerializers.entrySet()) {
      if (entry.getValue().getClass().getClassLoader().equals(classLoader)) {
        this.registeredSerializers.remove(entry.getKey(), entry.getValue());
      }
    }
    // for chaining
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull <T> ObjectMapper registerBinding(
    @NonNull Type type,
    @NonNull ObjectSerializer<T> serializer,
    boolean superTypes
  ) {
    if (superTypes) {
      var typeToken = this.typeTokenCache.computeIfAbsent(type, TypeToken::of);
      // register all subtypes of the type
      for (TypeToken<?> token : typeToken.getTypes()) {
        this.registeredSerializers.putIfAbsent(token.getType(), serializer);
        this.registeredSerializers.putIfAbsent(token.getRawType(), serializer);
      }
    } else {
      // we don't need to register the subtypes of the type, skip the lookup
      this.registeredSerializers.putIfAbsent(type, serializer);
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public @NonNull <T> DataBuf.Mutable writeObject(@NonNull DataBuf.Mutable dataBuf, @Nullable T object) {
    return dataBuf.writeNullable(object, (buffer, obj) -> {
      // Get the type token of the type
      var typeToken = (TypeToken<T>) this.typeTokenCache.computeIfAbsent(obj.getClass(), TypeToken::of);
      // get the registered serializer for the type
      ObjectSerializer<T> serializer = null;
      for (TypeToken<?> type : typeToken.getTypes()) {
        serializer = this.serializerForType(type);
        if (serializer != null && serializer.preWriteCheckAccepts(obj, this)) {
          break;
        }
      }
      // check if a serializer was found
      if (serializer == null || !serializer.preWriteCheckAccepts(obj, this)) {
        throw new MissingObjectSerializerException(obj.getClass());
      }
      // serialize the object into the buffer
      serializer.write(buffer, obj, obj.getClass(), this);
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> @Nullable T readObject(@NonNull DataBuf dataBuf, @NonNull Type type) {
    return dataBuf.readNullable(buffer -> {
      // Get the type token of the type
      var typeToken = this.typeTokenCache.computeIfAbsent(type, TypeToken::of);
      // get the registered serializer for the type
      ObjectSerializer<?> serializer = null;
      for (TypeToken<?> subType : typeToken.getTypes()) {
        serializer = this.serializerForType(subType);
        if (serializer != null && serializer.preReadCheckAccepts(type, this)) {
          break;
        }
      }
      // check if a serializer was found
      if (serializer == null || !serializer.preReadCheckAccepts(type, this)) {
        throw new MissingObjectSerializerException(type);
      }
      // read the object from the buffer
      return (T) serializer.read(buffer, type, this);
    });
  }

  /**
   * Finds the best matching serializer for the given type. The method first tries to get the serializer by the exact
   * type of the supplied type token, then by the raw type.
   *
   * @param typeToken the type token of the type to get.
   * @param <T>       the generic type of the object serializer to get.
   * @return the best matching object serializer for the given type.
   * @throws NullPointerException if the given type token is null.
   */
  @SuppressWarnings("unchecked")
  protected @Nullable <T> ObjectSerializer<T> serializerForType(@NonNull TypeToken<?> typeToken) {
    var byType = (ObjectSerializer<T>) this.registeredSerializers.get(typeToken.getType());
    return byType == null ? (ObjectSerializer<T>) this.registeredSerializers.get(typeToken.getRawType()) : byType;
  }
}
