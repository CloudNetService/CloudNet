/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.common.tuple.Tuple2;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.buffer.DataBufable;
import eu.cloudnetservice.driver.network.rpc.defaults.object.data.DataClassSerializer;
import eu.cloudnetservice.driver.network.rpc.defaults.object.serializers.CollectionObjectSerializer;
import eu.cloudnetservice.driver.network.rpc.defaults.object.serializers.DataBufObjectSerializer;
import eu.cloudnetservice.driver.network.rpc.defaults.object.serializers.DataBufableObjectSerializer;
import eu.cloudnetservice.driver.network.rpc.defaults.object.serializers.DocumentObjectSerializer;
import eu.cloudnetservice.driver.network.rpc.defaults.object.serializers.EnumObjectSerializer;
import eu.cloudnetservice.driver.network.rpc.defaults.object.serializers.FunctionalObjectSerializer;
import eu.cloudnetservice.driver.network.rpc.defaults.object.serializers.MapObjectSerializer;
import eu.cloudnetservice.driver.network.rpc.defaults.object.serializers.OptionalObjectSerializer;
import eu.cloudnetservice.driver.network.rpc.defaults.object.serializers.PathObjectSerializer;
import eu.cloudnetservice.driver.network.rpc.defaults.object.serializers.PatternObjectSerializer;
import eu.cloudnetservice.driver.network.rpc.defaults.object.serializers.TimeObjectSerializer;
import eu.cloudnetservice.driver.network.rpc.defaults.object.serializers.UUIDObjectSerializer;
import eu.cloudnetservice.driver.network.rpc.exception.MissingObjectSerializerException;
import eu.cloudnetservice.driver.network.rpc.object.ObjectMapper;
import eu.cloudnetservice.driver.network.rpc.object.ObjectSerializer;
import jakarta.inject.Singleton;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
import java.util.regex.Pattern;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default object mapper implementation.
 *
 * @since 4.0
 */
@Singleton
@Provides(ObjectMapper.class)
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
    // pattern
    .put(Pattern.class, new PatternObjectSerializer())
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
    // java.time classes
    .put(Year.class, TimeObjectSerializer.YEAR_SERIALIZER)
    .put(Period.class, TimeObjectSerializer.PERIOD_SERIALIZER)
    .put(ZoneId.class, TimeObjectSerializer.ZONE_ID_SERIALIZER)
    .put(Instant.class, TimeObjectSerializer.INSTANT_SERIALIZER)
    .put(Duration.class, TimeObjectSerializer.DURATION_SERIALIZER)
    .put(MonthDay.class, TimeObjectSerializer.MONTH_DAY_SERIALIZER)
    .put(LocalDate.class, TimeObjectSerializer.LOCAL_DATE_SERIALIZER)
    .put(LocalTime.class, TimeObjectSerializer.LOCAL_TIME_SERIALIZER)
    .put(YearMonth.class, TimeObjectSerializer.YEAR_MONTH_SERIALIZER)
    .put(OffsetTime.class, TimeObjectSerializer.OFFSET_TIME_SERIALIZER)
    .put(LocalDateTime.class, TimeObjectSerializer.LOCAL_DATE_TIME_SERIALIZER)
    .put(ZonedDateTime.class, TimeObjectSerializer.ZONED_DATE_TIME_SERIALIZER)
    .put(OffsetDateTime.class, TimeObjectSerializer.OFFSET_DATE_TIME_SERIALIZER)
    // data classes
    .put(Path.class, new PathObjectSerializer())
    .put(DataBuf.class, new DataBufObjectSerializer())
    .put(DataBufable.class, new DataBufableObjectSerializer())
    .put(Document.class, new DocumentObjectSerializer())
    .put(Enum.class, new EnumObjectSerializer())
    .put(Object.class, new DataClassSerializer())
    .build();

  static {
    // This is required as IJ wants the field ABOVE the map with the default types which results in an error
    DEFAULT_MAPPER = new DefaultObjectMapper();
  }

  private final Map<Type, ObjectSerializer<?>> registeredSerializers = new ConcurrentHashMap<>();
  private final LoadingCache<Type, Collection<Tuple2<Type, Type>>> typeCache = Caffeine.newBuilder()
    .expireAfterAccess(Duration.ofDays(1))
    .scheduler(Scheduler.systemScheduler())
    .build(key -> {
      // extract all types from the given key, map them to the actual and raw type
      Collection<Tuple2<Type, Type>> types = new LinkedList<>();
      for (var type : TypeToken.of(key).getTypes()) {
        types.add(new Tuple2<>(type.getType(), type.getRawType()));
      }
      return types;
    });

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
      var subTypes = this.typeCache.get(type);
      // unregister all subtypes of the type
      for (var subType : subTypes) {
        this.registeredSerializers.remove(subType.first());
        this.registeredSerializers.remove(subType.second());
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
      var subTypes = this.typeCache.get(type);
      // register all subtypes of the type
      for (var subType : subTypes) {
        this.registeredSerializers.putIfAbsent(subType.first(), serializer);
        this.registeredSerializers.putIfAbsent(subType.second(), serializer);
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
  public @NonNull <T> DataBuf.Mutable writeObject(@NonNull DataBuf.Mutable dataBuf, @Nullable T object) {
    return dataBuf.writeNullable(object, (buffer, obj) -> {
      // Get the type token of the type
      var subTypes = this.typeCache.get(obj.getClass());
      // get the registered serializer for the type
      ObjectSerializer<T> serializer = null;
      for (var subType : subTypes) {
        serializer = this.serializerForType(subType);
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
      var subTypes = this.typeCache.get(type);
      // get the registered serializer for the type
      ObjectSerializer<?> serializer = null;
      for (var subType : subTypes) {
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
   * @param typePair the mapping pair of the actual type (first) and the raw type (second) of the serializer to get.
   * @param <T>      the generic type of the object serializer to get.
   * @return the best matching object serializer for the given type.
   * @throws NullPointerException if the given type token is null.
   */
  @SuppressWarnings("unchecked")
  protected @Nullable <T> ObjectSerializer<T> serializerForType(@NonNull Tuple2<Type, Type> typePair) {
    var byType = (ObjectSerializer<T>) this.registeredSerializers.get(typePair.first());
    return byType == null ? (ObjectSerializer<T>) this.registeredSerializers.get(typePair.second()) : byType;
  }
}
