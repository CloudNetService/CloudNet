/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.wrapper.impl.transform.unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import lombok.NonNull;

/**
 * Registry that holds the offsets for fields.
 *
 * @since 4.0
 */
final class FieldOffsetOps {

  /**
   * The amount of fields declared in {@code Class}, offset of all static fields.
   */
  static final int CLASS_FIELD_COUNT;
  /**
   * The offset of the field at index 0 in a field table.
   */
  static final long FIELD_BASE_OFFSET = 16;
  /**
   * The distance between the offsets of two fields. Large enough to hold every field type, so offsets are aligned and
   * offset arithmetic (for example offset + 4) never silently addresses another field.
   */
  static final long FIELD_OFFSET_SCALE = 8;

  // MethodHandle to get all fields in a class, unfiltered
  private static final MethodHandle GET_DECLARED_FIELDS;
  private static final VarHandle ACCESSOR_ELEMENT = MethodHandles.arrayElementVarHandle(FieldAccessor[].class);
  private static final ClassValue<FieldTable> FIELD_TABLES = new ClassValue<>() {
    @Override
    protected @NonNull FieldTable computeValue(@NonNull Class<?> type) {
      return new FieldTable(hierarchyFields(type));
    }
  };

  static {
    try {
      var lookup = OpConstants.TRUSTED_LOOKUP.get();
      GET_DECLARED_FIELDS = lookup.findVirtual(
        Class.class,
        "getDeclaredFields0",
        MethodType.methodType(Field[].class, boolean.class));
      CLASS_FIELD_COUNT = declaredFields(Class.class).length;
    } catch (NoSuchMethodException | IllegalAccessException exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  private FieldOffsetOps() {
    throw new UnsupportedOperationException();
  }

  /**
   * Get all fields that are declared in the given class, without applying the reflection filter.
   *
   * @param type the class to get the declared fields of.
   * @return all fields declared in the given class.
   * @throws NullPointerException if the given type is null.
   */
  private static @NonNull Field[] declaredFields(@NonNull Class<?> type) {
    try {
      return (Field[]) GET_DECLARED_FIELDS.invokeExact(type, false);
    } catch (RuntimeException | Error exception) {
      throw exception;
    } catch (Throwable throwable) {
      throw new IllegalStateException(throwable);
    }
  }

  /**
   * Get all fields declared in the class hierarchy of the given class, starting with the fields of the top-most super
   * class. The index of a field in the returned array determines the offset of the field.
   *
   * @param type the class to get the hierarchy fields of.
   * @return all fields declared in the class hierarchy of the given class.
   * @throws NullPointerException if the given type is null.
   */
  private static @NonNull Field[] hierarchyFields(@NonNull Class<?> type) {
    var declaredFields = declaredFields(type);
    var superclass = type.getSuperclass();
    if (superclass == null) {
      return declaredFields;
    }

    var superFields = FIELD_TABLES.get(superclass).fields;
    var fields = Arrays.copyOf(superFields, superFields.length + declaredFields.length);
    System.arraycopy(declaredFields, 0, fields, superFields.length, declaredFields.length);
    return fields;
  }

  /**
   * Get the base object to use for the given static field.
   *
   * @param field the field to get the base of.
   * @return the base of the given field.
   * @throws NullPointerException if the given field is null.
   */
  public static @NonNull Object staticFieldBase(@NonNull Field field) {
    return field.getDeclaringClass();
  }

  /**
   * Converts the given index of a field in a field table into the offset of the field.
   *
   * @param index the index of the field in the field table.
   * @return the offset of the field at the given index.
   */
  static long indexToOffset(long index) {
    return FIELD_BASE_OFFSET + index * FIELD_OFFSET_SCALE;
  }

  /**
   * Gets the offset of the given field in the clazz hierarchy.
   *
   * @param field the field to get the offset of.
   * @return the offset of the given class in the class hierarchy.
   * @throws NullPointerException     if the given field is null.
   * @throws IllegalArgumentException if the given field is not declared in its declaring class.
   */
  public static long fieldOffset(@NonNull Field field) {
    var fields = FIELD_TABLES.get(field.getDeclaringClass()).fields;
    for (var index = fields.length - 1; index >= 0; index--) {
      if (fields[index].equals(field)) {
        var baseFieldIndex = Modifier.isStatic(field.getModifiers()) ? CLASS_FIELD_COUNT + index : index;
        return indexToOffset(baseFieldIndex);
      }
    }

    throw new IllegalArgumentException("Field " + field + " is not declared in its declaring class");
  }

  /**
   * Resolves the field associated with the given instance and offset.
   *
   * @param base   the field base to get the field based of.
   * @param offset the offset of the field to get.
   * @return the field with the given offset in the given base.
   * @throws NullPointerException     if the given base is null.
   * @throws IllegalArgumentException if no field with the given offset exists in the given base.
   */
  public static @NonNull FieldAccessor fieldFromOffset(Object base, long offset) {
    if (offset < FIELD_BASE_OFFSET || (offset - FIELD_BASE_OFFSET) % FIELD_OFFSET_SCALE != 0) {
      throw invalidOffset(base, offset);
    }

    // access to a static field in the given class
    var index = (offset - FIELD_BASE_OFFSET) / FIELD_OFFSET_SCALE;
    if (base instanceof Class<?> clazz && index >= CLASS_FIELD_COUNT) {
      var accessor = fieldFromOffset(clazz, base, index - CLASS_FIELD_COUNT, offset);
      if (!accessor.isStatic()) {
        throw invalidOffset(base, offset);
      }
      return accessor;
    }

    // access to an instance field
    var accessor = fieldFromOffset(base.getClass(), base, index, offset);
    if (accessor.isStatic()) {
      throw invalidOffset(base, offset);
    }

    return accessor;
  }

  /**
   * Resolves the field at the given index in the field table of the given class.
   *
   * @param type   the class whose field table should be used.
   * @param base   the field base that was passed by the caller, for exception messages.
   * @param index  the index of the field in the field table.
   * @param offset the offset that was passed by the caller, for exception messages.
   * @return the field at the given index in the field table of the given class.
   * @throws IllegalArgumentException if the given index is out of bounds.
   */
  private static @NonNull FieldAccessor fieldFromOffset(Class<?> type, Object base, long index, long offset) {
    var table = FIELD_TABLES.get(type);
    if (index < 0 || index >= table.accessors.length) {
      throw invalidOffset(base, offset);
    }

    return table.accessor((int) index);
  }

  /**
   * Constructs the exception to throw when an invalid field offset is passed by a caller.
   *
   * @param base   the field base that was passed by the caller.
   * @param offset the offset that was passed by the caller.
   * @return the exception to throw for the invalid offset.
   */
  private static @NonNull IllegalArgumentException invalidOffset(Object base, long offset) {
    var type = base instanceof Class<?> clazz ? clazz : base.getClass();
    return new IllegalArgumentException("Invalid field offset " + offset + " for base of type " + type.getName());
  }

  /**
   * The table of fields in the class hierarchy of a class.
   *
   * @param fields    all fields declared in the class hierarchy, the index of a field determines its offset.
   * @param accessors the lazily resolved field accessors, same order as the fields.
   * @since 4.0
   */
  private record FieldTable(@NonNull Field[] fields, FieldAccessor[] accessors) {

    /**
     * Constructs a new field table for the given fields.
     *
     * @param fields all fields declared in the class hierarchy, the index of a field determines its offset.
     * @throws NullPointerException if the given fields array is null.
     */
    FieldTable(@NonNull Field[] fields) {
      this(fields, new FieldAccessor[fields.length]);
    }

    /**
     * Get the accessor for the field at the given index, resolving it if it was not resolved yet.
     *
     * @param index the index of the field to get the accessor for.
     * @return the accessor for the field at the given index.
     */
    @NonNull
    FieldAccessor accessor(int index) {
      var accessor = this.accessors[index];
      return accessor != null ? accessor : this.resolveAccessor(index);
    }

    /**
     * Resolves and caches the accessor for the field at the given index.
     *
     * @param index the index of the field to resolve the accessor for.
     * @return the accessor for the field at the given index.
     */
    @NonNull
    private FieldAccessor resolveAccessor(int index) {
      var field = this.fields[index];
      var declaringTable = FIELD_TABLES.get(field.getDeclaringClass());
      var newAccessor = declaringTable == this ? FieldAccessor.make(field) : declaringTable.accessor(index);
      var storedAccessor = (FieldAccessor) ACCESSOR_ELEMENT.compareAndExchange(
        this.accessors,
        index,
        null,
        newAccessor);
      return storedAccessor != null ? storedAccessor : newAccessor;
    }
  }
}
