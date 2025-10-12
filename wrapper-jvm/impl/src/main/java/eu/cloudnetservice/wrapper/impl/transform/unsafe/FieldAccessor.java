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
import java.lang.invoke.VarHandle;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * An accessor for a field in a class.
 *
 * @since 4.0
 */
final class FieldAccessor {

  private final Class<?> fieldType;
  private final ValueTypeKind kind;
  private final WeakReference<Field> fieldRef;

  private final VarHandle varHandle;
  private final MethodHandle putMethodHandle; // null if VarHandle can support set (field is not trusted final)

  /**
   * Constructs a new field accessor from the given field.
   *
   * @param field the field to construct the accessor for.
   * @throws NullPointerException         if the given field is null.
   * @throws ReflectiveOperationException if an exception occurs while getting the required reflective values.
   */
  private FieldAccessor(@NonNull Field field) throws ReflectiveOperationException {
    this.fieldType = field.getType();
    this.kind = ValueTypeKind.of(this.fieldType);
    this.fieldRef = new WeakReference<>(field);

    // resolve field var handle, add leading dummy Object argument for static handles (easier calling later)
    var lookup = OpConstants.TRUSTED_LOOKUP.get();
    var fieldVh = lookup.unreflectVarHandle(field);
    var isStatic = Modifier.isStatic(field.getModifiers());
    this.varHandle = switch (isStatic) {
      case false -> fieldVh;
      case true -> MethodHandles.dropCoordinates(fieldVh, 0, Object.class);
    };

    // resolve field put method handle, only needed when the var handle cannot be used to set the field value
    // if present a static field put mh will have a leading dummy object argument for calling convenience
    var vhCanSet = this.varHandle.isAccessModeSupported(VarHandle.AccessMode.SET);
    this.putMethodHandle = switch (vhCanSet) {
      case true -> null; // can do everything with VH
      case false -> {
        if (isStatic) {
          var mh = lookup.findStaticSetter(field.getDeclaringClass(), field.getName(), field.getType());
          yield MethodHandles.dropArguments(mh, 0, Object.class);
        } else {
          yield lookup.findSetter(field.getDeclaringClass(), field.getName(), field.getType());
        }
      }
    };
  }

  /**
   * Constructs a new field accessor from the given field.
   *
   * @param field the field to construct the accessor for.
   * @throws NullPointerException  if the given field is null.
   * @throws IllegalStateException if an exception occurs while getting the required reflective values.
   */
  public static @NonNull FieldAccessor make(@NonNull Field field) {
    try {
      return new FieldAccessor(field);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
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
   * Gets the value of the wrapped field in the given instance.
   *
   * @param instance the instance to get the value from, possibly null.
   * @param op       the operation to use for getting the value.
   * @return the value of the given field in the given instance, possibly null.
   * @throws NullPointerException if the given get operation type is null.
   */
  public @Nullable Object get(@Nullable Object instance, @NonNull OpConstants.GetOp op) {
    return switch (op) {
      case DEFAULT -> this.varHandle.get(instance);
      case VOLATILE -> this.varHandle.getVolatile(instance);
    };
  }

  /**
   * Puts the given value into the wrapped field in the given instance. Does nothing in case the given value might not
   * be supported by the given field. This method does not guarantee the requested memory semantics if the field is
   * trusted final.
   *
   * @param instance the instance to set the value in, possibly null.
   * @param value    the value to set into the given field, possibly null.
   * @param op       the operation to use for setting the value.
   * @throws NullPointerException if the given set operation type is null.
   * @throws Throwable            if an unexpected exception occurs while writing the field value.
   */
  public void put(
    @Nullable Object instance,
    @Nullable Object value,
    @NonNull OpConstants.SetOp op
  ) throws Throwable {
    var convertedVal = convertFieldValue(value, this.fieldType, this.kind);
    if (this.putMethodHandle == null) {
      switch (op) {
        case DEFAULT -> this.varHandle.set(instance, convertedVal);
        case VOLATILE -> this.varHandle.setVolatile(instance, convertedVal);
        case RELEASE -> this.varHandle.setRelease(instance, convertedVal);
      }
    } else {
      this.putMethodHandle.invoke(instance, convertedVal);
    }
  }

  /**
   * Gets the value of the wrapped field in the given instance and sets it to the given value. Returns null if the field
   * value was null or the given value is not supported by the given field. This method does not guarantee the requested
   * memory semantics if the field is trusted final.
   *
   * @param instance the instance to get the value from, possibly null.
   * @param value    the value to set into the given field, possibly null.
   * @return the old value of the given field in the given instance, possibly null.
   * @throws Throwable if an unexpected exception occurs while reading or writing the field value.
   */
  public @Nullable Object getAndPut(
    @Nullable Object instance,
    @Nullable Object value
  ) throws Throwable {
    var convertedVal = convertFieldValue(value, this.fieldType, this.kind);
    if (this.putMethodHandle == null) {
      return this.varHandle.getAndSet(instance, convertedVal);
    } else {
      var currentValue = this.varHandle.get(instance);
      this.putMethodHandle.invoke(instance, convertedVal);
      return currentValue;
    }
  }

  /**
   * Gets the current value of the wrapped field in the given instance and adds the given value to it. This method does
   * not guarantee the requested memory semantics if the field is trusted final.
   *
   * @param instance the instance to get the value from, possibly null.
   * @param value    the value to add to the current field value.
   * @return the old value of the given field in the given instance, possibly null.
   * @throws Throwable if an unexpected exception occurs while reading or writing the field value.
   */
  // NOTE: kind can only be INT or LONG
  @SuppressWarnings("ConstantConditions")
  public @Nullable Object getAndAdd(
    @Nullable Object instance,
    @Nullable Object value
  ) throws Throwable {
    var convertedVal = convertFieldValue(value, this.fieldType, this.kind);
    if (this.putMethodHandle == null) {
      return this.varHandle.getAndAdd(instance, convertedVal);
    } else {
      // field and converted value has to be a primitive at this point, so casting is safe
      var oldVal = (Number) this.varHandle.get(instance);
      var valueSum = ((Number) convertedVal).longValue() + oldVal.longValue();
      var fieldVal = convertFieldValue(valueSum, this.fieldType, this.kind);
      this.putMethodHandle.invoke(instance, fieldVal);
      return oldVal;
    }
  }

  /**
   * Sets the value of the wrapped field in case the current value of the field is equal to the given expected value.
   * This method does not guarantee the requested memory semantics if the field is trusted final.
   *
   * @param instance the instance that contains the field to compare and swap the value of, possibly null.
   * @param expected the expected value of the field, possibly null.
   * @param value    the value to set into the given field, possibly null.
   * @return true if the field was successfully set, false otherwise.
   * @throws NullPointerException if the given type kind is null.
   * @throws Throwable            if an unexpected exception occurs while reading or writing the field value.
   */
  public boolean compareAndSet(
    @Nullable Object instance,
    @Nullable Object expected,
    @Nullable Object value
  ) throws Throwable {
    var convertedVal = convertFieldValue(value, this.fieldType, this.kind);
    var convertedExp = convertFieldValue(expected, this.fieldType, this.kind);
    if (this.putMethodHandle == null) {
      return this.varHandle.compareAndSet(instance, convertedExp, convertedVal);
    } else {
      var witness = this.varHandle.get(instance);
      var witnessIsExpected = this.kind.areValuesEqual(witness, convertedExp);
      if (witnessIsExpected) {
        this.putMethodHandle.invoke(instance, convertedVal);
        return true;
      }
      return false;
    }
  }

  /**
   * Get the field that was used to create this accessor. Note that the return value might be null in case the field was
   * garbage collected. Only intended for use in tests.
   *
   * @return the field that was used to create this accessor.
   */
  @VisibleForTesting
  public @Nullable Field wrappedField() {
    return this.fieldRef.get();
  }
}
