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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implements all unsafe operations that are related to on-heap fields.
 *
 * @since 4.0
 */
final class FieldOps {

  private FieldOps() {
    throw new UnsupportedOperationException();
  }

  /**
   * Checks and converts the given field value if necessary. Throws an exception when an attempt is made to write an
   * incompatible type into a reference field.
   *
   * @param value        the value to convert.
   * @param requiredType the field type.
   * @param kind         the field type kind.
   * @return the value converted into a form that can be written into the target field.
   * @throws NullPointerException          if the given required type or required type kind is null.
   * @throws UnsupportedOperationException when an incompatible value for the target reference field type is provided.
   */
  private static @Nullable Object convertFieldValue(
    @Nullable Object value,
    @NonNull Class<?> requiredType,
    @NonNull ValueTypeKind kind
  ) {
    if (kind == ValueTypeKind.REF) {
      if (value != null && !requiredType.isAssignableFrom(value.getClass())) {
        // value is unsupported by the field, this type of method call was allowed with unsafe
        // but is no longer allowed as we cannot support this with the safe replacements of the jvm
        throw new UnsupportedOperationException(
          "Tried to put value of type " + value.getClass() + " into field of type " + requiredType);
      }

      return value;
    }

    // convert primitive types between each other as close as possible
    return switch (kind) {
      case BYTE -> convertNumber(value, (byte) 0, Number::byteValue);
      case SHORT -> convertNumber(value, (short) 0, Number::shortValue);
      case INT -> convertNumber(value, 0, Number::intValue);
      case LONG -> convertNumber(value, 0L, Number::longValue);
      case FLOAT -> convertNumber(value, 0F, Number::floatValue);
      case DOUBLE -> convertNumber(value, 0D, Number::doubleValue);
      case BOOL -> switch (value) {
        case Boolean b -> b;
        case Number n -> n.byteValue() == 1;
        case Character c -> c != '\0';
        case null, default -> false;
      };
      case CHAR -> switch (value) {
        case Character c -> c;
        case Number n -> (char) n.byteValue();
        case Boolean b -> b ? '\1' : '\0';
        case null, default -> '\0';
      };
      default -> throw new AssertionError();
    };
  }

  /**
   * Converts the given value into a number of the expected type, conditionally returning the given default value if the
   * given value cannot be converted somehow.
   *
   * @param value        the value to convert into the expected number.
   * @param defaultValue the default value of the number type.
   * @param numToType    a conversion function for a general number to the expected number type.
   * @return the given value, converted to a number of the requested type.
   * @throws NullPointerException if the given default value or number converter is null.
   */
  private static @NonNull Object convertNumber(
    @Nullable Object value,
    @NonNull Object defaultValue,
    @NonNull Function<Number, Object> numToType
  ) {
    return switch (value) {
      case Number n -> numToType.apply(n);
      case Boolean b -> numToType.apply(b ? 1 : 0);
      case Character c -> numToType.apply((byte) c.charValue());
      case null, default -> defaultValue;
    };
  }

  /**
   * Gets the value of the given field in the given instance.
   *
   * @param field    the field to get the value of.
   * @param instance the instance to get the value from, possibly null.
   * @param op       the operation to use for getting the value.
   * @return the value of the given field in the given instance, possibly null.
   * @throws NullPointerException if the given field or get operation type is null.
   * @throws Throwable            if an unexpected exception occurs while reading the field value.
   */
  static @Nullable Object fieldGet(
    @NonNull Field field,
    @Nullable Object instance,
    @NonNull OpConstants.GetOp op
  ) throws Throwable {
    var lookup = OpConstants.TRUSTED_LOOKUP.get();
    var isStatic = Modifier.isStatic(field.getModifiers());
    var handle = lookup.unreflectVarHandle(field);
    if (isStatic) {
      return switch (op) {
        case DEFAULT -> handle.get();
        case VOLATILE -> handle.getVolatile();
      };
    } else {
      return switch (op) {
        case DEFAULT -> handle.get(instance);
        case VOLATILE -> handle.getVolatile(instance);
      };
    }
  }

  /**
   * Puts the given value into the given field in the given instance. Does nothing in case the given value might not be
   * supported by the given field. This method does not guarantee the requested memory semantics if the field is final.
   *
   * @param kind     the type kind of the given field.
   * @param field    the field to set the value of.
   * @param instance the instance to set the value in, possibly null.
   * @param val      the value to set into the given field, possibly null.
   * @param op       the operation to use for setting the value.
   * @throws NullPointerException if the given type kind, field or set operation type is null.
   * @throws Throwable            if an unexpected exception occurs while writing the field value.
   */
  static void fieldPut(
    @NonNull ValueTypeKind kind,
    @NonNull Field field,
    @Nullable Object instance,
    @Nullable Object val,
    @NonNull OpConstants.SetOp op
  ) throws Throwable {
    var convertedVal = convertFieldValue(val, field.getType(), kind);
    var lookup = OpConstants.TRUSTED_LOOKUP.get();
    var isStatic = Modifier.isStatic(field.getModifiers());
    if (Modifier.isFinal(field.getModifiers())) {
      // the field is final, cannot use var handles, fall back to default impl
      fieldPutMh(field, instance, convertedVal, lookup);
    } else {
      // field is non-final, can use var handles
      var handle = lookup.unreflectVarHandle(field);
      if (isStatic) {
        switch (op) {
          case DEFAULT -> handle.set(convertedVal);
          case VOLATILE -> handle.setVolatile(convertedVal);
          case RELEASE -> handle.setRelease(convertedVal);
        }
      } else {
        switch (op) {
          case DEFAULT -> handle.set(instance, convertedVal);
          case VOLATILE -> handle.setVolatile(instance, convertedVal);
          case RELEASE -> handle.setRelease(instance, convertedVal);
        }
      }
    }
  }

  /**
   * Puts the given field value using method handles, only used as a fallback when var handles cannot be used.
   *
   * @param field    the field to set the value of.
   * @param instance the instance to set the value in, possibly null.
   * @param val      the value to set into the given field, possibly null.
   * @throws NullPointerException if the given type kind, field or set operation type is null.
   * @throws Throwable            if an unexpected exception occurs while writing the field value.
   */
  private static void fieldPutMh(
    @NonNull Field field,
    @Nullable Object instance,
    @Nullable Object val,
    @NonNull MethodHandles.Lookup lookup
  ) throws Throwable {
    var isStatic = Modifier.isStatic(field.getModifiers());
    if (isStatic) {
      var setter = lookup.findStaticSetter(field.getDeclaringClass(), field.getName(), field.getType());
      setter.invoke(val);
    } else {
      var setter = lookup.findSetter(field.getDeclaringClass(), field.getName(), field.getType());
      setter.invoke(instance, val);
    }
  }

  /**
   * Gets the value of the given field in the given instance and sets it to the given value. Returns null if the field
   * value was null or the given value is not supported by the given field. This method does not guarantee the requested
   * memory semantics if the field is final.
   *
   * @param kind     the type kind of the given field.
   * @param field    the field to get the value of.
   * @param instance the instance to get the value from, possibly null.
   * @param val      the value to set into the given field, possibly null.
   * @return the old value of the given field in the given instance, possibly null.
   * @throws NullPointerException if the given type kind or field is null.
   * @throws Throwable            if an unexpected exception occurs while reading or writing the field value.
   */
  static @Nullable Object fieldGetPut(
    @NonNull ValueTypeKind kind,
    @NonNull Field field,
    @Nullable Object instance,
    @Nullable Object val
  ) throws Throwable {
    var convertedVal = convertFieldValue(val, field.getType(), kind);
    var lookup = OpConstants.TRUSTED_LOOKUP.get();
    var isStatic = Modifier.isStatic(field.getModifiers());
    if (Modifier.isFinal(field.getModifiers())) {
      // the field is final, cannot use var handles, fall back to default impl
      var oldVal = fieldGet(field, instance, OpConstants.GetOp.DEFAULT);
      fieldPutMh(field, instance, convertedVal, lookup);
      return oldVal;
    } else {
      // field is non-final, can use var handles
      var handle = lookup.unreflectVarHandle(field);
      return isStatic ? handle.getAndSet(convertedVal) : handle.getAndSet(instance, convertedVal);
    }
  }

  /**
   * Gets the current value of the given field in the given instance and adds the given value to it. This method does
   * not guarantee the requested memory semantics if the field is final.
   *
   * @param kind     the type kind of the given field.
   * @param field    the field to get and add to.
   * @param instance the instance to get the value from, possibly null.
   * @param value    the value to add to the current field value.
   * @return the old value of the given field in the given instance, possibly null.
   * @throws NullPointerException if the given type kind or field is null.
   * @throws Throwable            if an unexpected exception occurs while reading or writing the field value.
   */
  // NOTE: kind can only be INT or LONG
  @SuppressWarnings("ConstantConditions")
  static @Nullable Object fieldGetAdd(
    @NonNull ValueTypeKind kind,
    @NonNull Field field,
    @Nullable Object instance,
    @Nullable Object value
  ) throws Throwable {
    var convertedVal = convertFieldValue(value, field.getType(), kind);
    var lookup = OpConstants.TRUSTED_LOOKUP.get();
    var isStatic = Modifier.isStatic(field.getModifiers());
    if (Modifier.isFinal(field.getModifiers())) {
      // the field is final, cannot use var handles, fall back to default impl
      // field and converted value has to be a primitive at this point, so casting is safe
      var oldVal = (Number) fieldGet(field, instance, OpConstants.GetOp.DEFAULT);
      var valueSum = ((Number) convertedVal).longValue() + oldVal.longValue();
      var fieldVal = convertFieldValue(valueSum, field.getType(), kind);
      fieldPutMh(field, instance, fieldVal, lookup);
      return oldVal;
    } else {
      // field is non-final, can use var handles
      var handle = lookup.unreflectVarHandle(field);
      return isStatic ? handle.getAndAdd(convertedVal) : handle.getAndAdd(instance, convertedVal);
    }
  }

  /**
   * Sets the value of the given field in case the current value of the field is equal to the given expected value. This
   * method does not guarantee the requested memory semantics if the field is final.
   *
   * @param kind     the type kind of the given field.
   * @param field    the field to compare and swap the value of.
   * @param instance the instance that contains the field to compare and swap the value of, possibly null.
   * @param expected the expected value of the field, possibly null.
   * @param value    the value to set into the given field, possibly null.
   * @return true if the field was successfully set, false otherwise.
   * @throws NullPointerException if the given type kind or field is null.
   * @throws Throwable            if an unexpected exception occurs while reading or writing the field value.
   */
  static boolean fieldComparePut(
    @NonNull ValueTypeKind kind,
    @NonNull Field field,
    @Nullable Object instance,
    @Nullable Object expected,
    @Nullable Object value
  ) throws Throwable {
    var convertedVal = convertFieldValue(value, field.getType(), kind);
    var convertedExp = convertFieldValue(expected, field.getType(), kind);
    var lookup = OpConstants.TRUSTED_LOOKUP.get();
    var isStatic = Modifier.isStatic(field.getModifiers());
    if (Modifier.isFinal(field.getModifiers())) {
      // the field is final, cannot use var handles, fall back to default impl
      var witness = fieldGet(field, instance, OpConstants.GetOp.DEFAULT);
      var witnessIsExpected = kind.areValuesEqual(witness, convertedExp);
      if (witnessIsExpected) {
        fieldPutMh(field, instance, convertedVal, lookup);
        return true;
      }
      return false;
    } else {
      // field is non-final, can use var handles
      var handle = lookup.unreflectVarHandle(field);
      return isStatic
        ? handle.compareAndSet(convertedExp, convertedVal)
        : handle.compareAndSet(instance, convertedExp, convertedVal);
    }
  }
}
