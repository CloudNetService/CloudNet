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

package eu.cloudnetservice.driver.impl.network.object.data;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.object.ObjectMapper;
import eu.cloudnetservice.driver.network.object.ObjectSerializer;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCIgnore;
import io.vavr.Tuple2;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.NonNull;

/**
 * A serializer for all data classes and arrays which have no special serializer available.
 *
 * @since 4.0
 */
public final class DataClassSerializer implements ObjectSerializer<Object> {

  private final Cache<Class<?>, Tuple2<DataClassCodec, AllocationStatistic>> dataClassCodecCache = Caffeine.newBuilder()
    .expireAfterAccess(Duration.ofHours(8)) // release generated classes for GC if not needed
    .build();

  /**
   * Validates that an instance of the given class can be constructed in runtime. This means that the class is not
   * abstract, an interface and does not represent a primitive type.
   *
   * @param clazz the class to check.
   * @throws IllegalArgumentException if the given class is not instantiable.
   * @throws NullPointerException     if the given class is null.
   */
  private static void ensureClassIsInstantiable(@NonNull Class<?> clazz) {
    var notInstantiable = clazz.isPrimitive()
      || clazz.isHidden()
      || clazz.isInterface()
      || clazz.isLocalClass()
      || clazz.isAnonymousClass()
      || Modifier.isAbstract(clazz.getModifiers());
    if (notInstantiable) {
      throw new IllegalArgumentException(String.format("class %s is not instantiable", clazz.getName()));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Object read(
    @NonNull DataBuf source,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    if (!(type instanceof Class<?> clazz)) {
      throw new IllegalArgumentException("data class serializer called with non-class type");
    }

    if (clazz.isArray()) {
      // type is an array, deserialize each element
      var arraySize = source.readInt();
      var componentType = clazz.getComponentType();
      var targetArray = Array.newInstance(componentType, arraySize);
      for (var index = 0; index < arraySize; index++) {
        var deserializedValue = caller.readObject(source, componentType);
        Array.set(targetArray, index, deserializedValue);
      }

      return targetArray;
    } else {
      // not an array, deserialize if possible
      ensureClassIsInstantiable(clazz);
      var dataClassCodec = this.dataClassCodecCache.get(clazz, target -> {
        var createdCodec = this.createDataClassCodec(target);
        return new Tuple2<>(createdCodec, new AllocationStatistic());
      })._1();
      return dataClassCodec.deserialize(source, caller);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(
    @NonNull DataBuf.Mutable dataBuf,
    @NonNull Object object,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    if (!(type instanceof Class<?> clazz)) {
      throw new IllegalArgumentException("data class serializer called with non-class type");
    }

    if (clazz.isArray()) {
      // type is an array, serialize each element
      var arrayLength = Array.getLength(object);
      dataBuf.writeInt(arrayLength);
      for (var index = 0; index < arrayLength; index++) {
        var entry = Array.get(object, index);
        caller.writeObject(dataBuf, entry);
      }
    } else {
      // not an array, serialize if possible
      ensureClassIsInstantiable(clazz);
      var codecInfo = this.dataClassCodecCache.get(clazz, target -> {
        var createdCodec = this.createDataClassCodec(target);
        return new Tuple2<>(createdCodec, new AllocationStatistic());
      });
      var dataClassCodec = codecInfo._1();
      var allocStatistic = codecInfo._2();

      // keep track of the bytes written into the buffer during the serialization
      // to prevent many resizes while writing of the data into the buffer
      var prevByteCount = dataBuf.readableBytes();
      try {
        dataBuf.ensureWriteable(allocStatistic.average());
        dataClassCodec.serialize(dataBuf, caller, object);
      } finally {
        var newByteCount = dataBuf.readableBytes();
        allocStatistic.add(newByteCount - prevByteCount);
      }
    }
  }

  /**
   * Generates the data class codec for the given target type. The result of the method is cached, and should not be
   * called directly due to some heavy work that is required to construct the data class codec.
   *
   * @param targetType the target type to construct the data class codec for.
   * @return the generated data class codec for the given target type.
   * @throws IllegalStateException if the given target type is missing an all-args constructor.
   * @throws NullPointerException  if the given target type is null.
   */
  private @NonNull DataClassCodec createDataClassCodec(@NonNull Class<?> targetType) {
    // traverse class hierarchy, in order
    var currentTarget = targetType;
    List<Field> allFields = new ArrayList<>();    // all fields found in the hierarchy
    List<Class<?>> hierarchy = new ArrayList<>(); // the complete class hierarchy
    do {
      hierarchy.add(currentTarget);
      var fields = currentTarget.getDeclaredFields();
      for (var field : fields) {
        var modifiers = field.getModifiers();
        if (field.isSynthetic()
          || Modifier.isStatic(modifiers)
          || Modifier.isTransient(modifiers)
          || field.isAnnotationPresent(RPCIgnore.class)) {
          // field is ignored
          continue;
        }

        allFields.add(field); // keep insertion order
      }
    } while ((currentTarget = currentTarget.getSuperclass()) != Object.class);

    // ensure that the target constructor for invocation exists
    var fieldTypes = allFields.stream().map(Field::getType).toArray(Class<?>[]::new);
    try {
      targetType.getDeclaredConstructor(fieldTypes);
    } catch (NoSuchMethodException _) {
      var paramTypeNames = Arrays.stream(fieldTypes).map(Class::getName).toList();
      throw new IllegalStateException(String.format(
        "%s does not have a constructor with with param types %s",
        targetType.getName(), paramTypeNames));
    }

    // generate the class codec
    return DataClassCodecGenerator.generateClassCodec(allFields, hierarchy);
  }
}
