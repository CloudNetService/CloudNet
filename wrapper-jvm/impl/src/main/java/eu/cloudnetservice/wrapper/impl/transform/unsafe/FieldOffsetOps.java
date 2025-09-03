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

package eu.cloudnetservice.wrapper.impl.transform.unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Registry that holds the offsets for fields.
 *
 * @since 4.0
 */
final class FieldOffsetOps {

  private static final int FIELD_COUNT_IN_CLASS = Class.class.getDeclaredFields().length;
  private static final ClassValue<PerClassFieldCache> CLASS_FIELD_CACHE = new ClassValue<>() {
    @Override
    protected @NonNull PerClassFieldCache computeValue(@NonNull Class<?> type) {
      return new PerClassFieldCache();
    }
  };

  private static final VarHandle SLOT_CACHE; // access to PerClassFieldCache.accessorsPerFieldSlot
  private static final VarHandle SLOT_ELEMENT; // access to elements in PerClassFieldCache.accessorsPerFieldSlot

  static {
    try {
      var lookup = MethodHandles.lookup();
      SLOT_CACHE = lookup.findVarHandle(PerClassFieldCache.class, "accessorsPerFieldSlot", FieldAccessor[].class);
      SLOT_ELEMENT = MethodHandles.arrayElementVarHandle(FieldAccessor[].class);
    } catch (NoSuchFieldException | IllegalAccessException exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  private FieldOffsetOps() {
    throw new UnsupportedOperationException();
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
   * Gets the offset of the given field in the clazz hierarchy.
   *
   * @param field the field to get the offset of.
   * @return the offset of the given class in the class hierarchy.
   * @throws NullPointerException if the given field is null.
   */
  public static long fieldOffset(@NonNull Field field) {
    var offset = fieldSlot(field);
    var clazz = field.getDeclaringClass();
    while ((clazz = clazz.getSuperclass()) != null) {
      var clazzFields = clazz.getDeclaredFields();
      offset += clazzFields.length;
    }

    if (Modifier.isStatic(field.getModifiers())) {
      // to get a static field, a class instance (declaring class of the field)
      // is passed to getXXX(instance, offset) methods. these methods can therefore
      // not differentiate between a request to a get an instance field of the "Class"
      // class and a static field, and therefore the fields in "Class" are always visited
      // by the method too. to prevent a conflict with invalid static field offsets caused
      // by static fields in the class "Class", we move the offset here by the count of
      // fields in "Class" to allow for a correct offset calculation when reading the value
      offset += FIELD_COUNT_IN_CLASS;
    }

    return offset;
  }

  /**
   * Get the slot of the given field in its declaring class.
   *
   * @param field the field to get the slot of.
   * @return the slot of the field in its declaring class.
   * @throws NullPointerException if the given field is null.
   */
  private static long fieldSlot(@NonNull Field field) {
    var clazz = field.getDeclaringClass();
    var fields = clazz.getDeclaredFields();
    for (var slot = 0; slot < fields.length; slot++) {
      var atSlot = fields[slot];
      if (atSlot.equals(field)) {
        return slot;
      }
    }

    throw new AssertionError();
  }

  /**
   * Resolves the field associated with the given instance and offset.
   *
   * @param base   the field base to get the field based of.
   * @param offset the offset of the field to get.
   * @return the field with the given offset in the given base.
   * @throws NullPointerException if the given base is null.
   */
  public static @Nullable FieldAccessor fieldFromOffset(@NonNull Object base, long offset) {
    var offsetAsInt = (int) offset;
    if (offset < 0 || offset != offsetAsInt) {
      // bail out early, a field with such an index is impossible
      return null;
    }

    var clazz = base instanceof Class<?> c ? c : base.getClass();
    var perClassCache = CLASS_FIELD_CACHE.get(clazz);
    for (; ; ) {
      var slots = perClassCache.accessorsPerFieldSlot;
      if (offsetAsInt < slots.length) {
        // check if the accessor was already computed and is cached
        var accessor = (FieldAccessor) SLOT_ELEMENT.getVolatile(slots, offsetAsInt);
        if (accessor != null) {
          return accessor;
        }

        // accessor is not yet cached, insert into cache
        var newAccessor = resolveFieldAccessor(clazz, base, offsetAsInt);
        var storedAccessor = (FieldAccessor) SLOT_ELEMENT.compareAndExchange(slots, offsetAsInt, null, newAccessor);
        return storedAccessor != null ? storedAccessor : newAccessor;
      }

      // slow path: expand array to insert new accessor into slot, then continue the loop
      var newArrayLength = Math.max(offsetAsInt + 1, Math.max(8, slots.length << 1));
      var resizedSlots = Arrays.copyOf(slots, newArrayLength);
      SLOT_CACHE.compareAndSet(perClassCache, slots, resizedSlots);
    }
  }

  /**
   * Resolves a field accessor for the field in the given class with the given field offset.
   *
   * @param clazz  the class to find the field in.
   * @param base   the field base to get the field based of.
   * @param offset the offset of the field to find.
   * @return an accessor for the field in the given class at the given offset.
   * @throws NullPointerException     if the given class or base is null.
   * @throws IllegalArgumentException if an invalid offset was given.
   */
  private static @NonNull FieldAccessor resolveFieldAccessor(
    @NonNull Class<?> clazz,
    @NonNull Object base,
    int offset
  ) {
    var classHierarchy = new ArrayList<Class<?>>();
    for (var c = clazz; c != null; c = c.getSuperclass()) {
      if (base instanceof Class<?> && c == Object.class) {
        // special case handling: if a class was provided as the instance to operate
        // on, this can have 2 meanings: either the caller requests a static field or
        // an instance field in the class "Class" which does usually not appear in
        // instance.getDeclaredFields() [which is intended as the fields in Class are
        // not inherited into whatever the given class is when looking though the
        // reflection api] - we therefore need to insert the class "Class" into the
        // hierarchy manually here to allow finding fields in there too
        classHierarchy.add(Class.class);
      }

      classHierarchy.add(c);
    }

    var remaining = offset;
    for (var c : classHierarchy.reversed()) {
      var fields = c.getDeclaredFields();
      if (remaining < fields.length) {
        var field = fields[remaining];
        return FieldAccessor.make(field);
      }

      remaining -= fields.length;
    }

    throw new IllegalArgumentException("caller provided invalid field offset");
  }

  /**
   * Cache for field accessors within a specific class.
   *
   * @since 4.0
   */
  private static final class PerClassFieldCache {

    /**
     * Array mapping of field slot to field accessor.
     */
    @SuppressWarnings("FieldMayBeFinal") // modified via VarHandle
    private volatile FieldAccessor[] accessorsPerFieldSlot = new FieldAccessor[0];
  }
}
