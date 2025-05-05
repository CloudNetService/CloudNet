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

package eu.cloudnetservice.utils.base;

import java.lang.classfile.CodeBuilder;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * Various utils to generate code using the java classfile api. For internal use only.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class CodeGenerationUtil {

  // converts for primitive <-> wrapper types
  private static final PrimitiveWrapperConverter C_BYTE =
    new PrimitiveWrapperConverter(ConstantDescs.CD_Byte, ConstantDescs.CD_byte);
  private static final PrimitiveWrapperConverter C_SHORT =
    new PrimitiveWrapperConverter(ConstantDescs.CD_Short, ConstantDescs.CD_short);
  private static final PrimitiveWrapperConverter C_INT =
    new PrimitiveWrapperConverter(ConstantDescs.CD_Integer, ConstantDescs.CD_int);
  private static final PrimitiveWrapperConverter C_LONG =
    new PrimitiveWrapperConverter(ConstantDescs.CD_Long, ConstantDescs.CD_long);
  private static final PrimitiveWrapperConverter C_FLOAT =
    new PrimitiveWrapperConverter(ConstantDescs.CD_Float, ConstantDescs.CD_float);
  private static final PrimitiveWrapperConverter C_DOUBLE =
    new PrimitiveWrapperConverter(ConstantDescs.CD_Double, ConstantDescs.CD_double);
  private static final PrimitiveWrapperConverter C_CHAR =
    new PrimitiveWrapperConverter(ConstantDescs.CD_Character, ConstantDescs.CD_char);
  private static final PrimitiveWrapperConverter C_BOOL =
    new PrimitiveWrapperConverter(ConstantDescs.CD_Boolean, ConstantDescs.CD_boolean);

  // stuff for class defining
  private static final MethodHandles.Lookup TRUSTED_LOOKUP;
  private static final MethodHandles.Lookup.ClassOption[] NEST_MATE_DEFINE_OPTIONS =
    new MethodHandles.Lookup.ClassOption[]{MethodHandles.Lookup.ClassOption.NESTMATE};

  static {
    try {
      // we explicitly hack into the module so that we can access the field
      // this is mandatory as class generation wouldn't be possible the way we're doing it, if not like this
      // using '--add-opens' as we do is specifically meant for cases like this: don't allow it by default, but
      // if you're sure what you're doing, you can allow it
      var trustedLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
      trustedLookupField.setAccessible(true);
      TRUSTED_LOOKUP = (MethodHandles.Lookup) trustedLookupField.get(null);
    } catch (ReflectiveOperationException exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  private CodeGenerationUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Unboxes the current object on the stack of the given code builder into the given primitive type (excluding void).
   *
   * @param codeBuilder      the code builder to append the unbox operation to.
   * @param targetDescriptor the descriptor of the target primitive type to convert to.
   * @throws NullPointerException     if the given code builder or target type is null.
   * @throws IllegalArgumentException if the given target type is not primitive or void.
   */
  public static void unboxPrimitive(@NonNull CodeBuilder codeBuilder, @NonNull String targetDescriptor) {
    switch (targetDescriptor) {
      case "B" -> unboxPrimitive(codeBuilder, C_BYTE);
      case "S" -> unboxPrimitive(codeBuilder, C_SHORT);
      case "I" -> unboxPrimitive(codeBuilder, C_INT);
      case "J" -> unboxPrimitive(codeBuilder, C_LONG);
      case "F" -> unboxPrimitive(codeBuilder, C_FLOAT);
      case "D" -> unboxPrimitive(codeBuilder, C_DOUBLE);
      case "C" -> unboxPrimitive(codeBuilder, C_CHAR);
      case "Z" -> unboxPrimitive(codeBuilder, C_BOOL);
      default -> throw new IllegalArgumentException("invalid primitive type: " + targetDescriptor);
    }
  }

  /**
   * Applies the primitive to wrapper conversion by first casting to the wrapper type and then invoking the method to
   * convert the wrapper to the primitive representation.
   *
   * @param codeBuilder the code builder to apply the operation to.
   * @param converter   the converter information for the wrapper type.
   * @throws NullPointerException if the given code builder or converter is null.
   */
  private static void unboxPrimitive(@NonNull CodeBuilder codeBuilder, @NonNull PrimitiveWrapperConverter converter) {
    codeBuilder
      .checkcast(converter.wrapperType)
      .invokevirtual(converter.wrapperType, converter.fromWrapperMethod, converter.fromWrapperDesc);
  }

  /**
   * Boxes the given primitive type on the stack of the given code builder (excluding void).
   *
   * @param codeBuilder      the code builder to append the box operation to.
   * @param sourceDescriptor the descriptor of the primitive type that should get boxed.
   * @throws NullPointerException     if the given code builder or target type is null.
   * @throws IllegalArgumentException if the given source type is not primitive or void.
   */
  public static void boxPrimitive(@NonNull CodeBuilder codeBuilder, @NonNull String sourceDescriptor) {
    switch (sourceDescriptor) {
      case "B" -> boxPrimitive(codeBuilder, C_BYTE);
      case "S" -> boxPrimitive(codeBuilder, C_SHORT);
      case "I" -> boxPrimitive(codeBuilder, C_INT);
      case "J" -> boxPrimitive(codeBuilder, C_LONG);
      case "F" -> boxPrimitive(codeBuilder, C_FLOAT);
      case "D" -> boxPrimitive(codeBuilder, C_DOUBLE);
      case "C" -> boxPrimitive(codeBuilder, C_CHAR);
      case "Z" -> boxPrimitive(codeBuilder, C_BOOL);
      default -> throw new IllegalArgumentException("invalid primitive type: " + sourceDescriptor);
    }
  }

  /**
   * Applies the conversion from a primitive type to the corresponding wrapper type on the given code builder.
   *
   * @param codeBuilder the code builder to apply the operation to.
   * @param converter   the converter information for the wrapper type.
   * @throws NullPointerException if the given code builder or converter is null.
   */
  private static void boxPrimitive(@NonNull CodeBuilder codeBuilder, @NonNull PrimitiveWrapperConverter converter) {
    codeBuilder.invokestatic(converter.wrapperType, converter.toWrapperMethod, converter.toWrapperDesc);
  }

  /**
   * Defines a class based on the given class bytes using the class loader of the given parent class. The returned class
   * has not been initialized.
   *
   * @param parentClass the parent class to use the classloader for class defining of.
   * @param classData   the raw bytecode of the class to define.
   * @return the class object of the defined class.
   * @throws NullPointerException if the given parent class or class data is null.
   */
  public static @NonNull Class<?> defineClass(@NonNull Class<?> parentClass, byte[] classData) {
    try {
      // specifically use .in() here to switch to the class loader that is used by the requesting class
      // rather than using the bootstrap class loader used by trusted lookup
      return TRUSTED_LOOKUP.in(parentClass).defineClass(classData);
    } catch (IllegalAccessException exception) {
      // this is never possible unless someone changes the TRUSTED_LOOKUP field
      throw new AssertionError("trusted lookup had no permission to define class", exception);
    }
  }

  /**
   * Defines a hidden nest mate class in the given host class, which grants the defined class full access to everything
   * that is present in the class (for example private members). The returned class has not been initialized.
   *
   * @param hostClass the host class which should be used for the nested class.
   * @param classData the raw bytecode of the nest class to define.
   * @return a method handle with full access to the defined nest class.
   * @throws NullPointerException if the given host class or class data is null.
   */
  public static @NonNull MethodHandles.Lookup defineNestedClass(@NonNull Class<?> hostClass, byte[] classData) {
    try {
      // use .in() here to switch to the host class in which the nested class should be defined
      return TRUSTED_LOOKUP.in(hostClass).defineHiddenClass(classData, false, NEST_MATE_DEFINE_OPTIONS);
    } catch (IllegalAccessException exception) {
      // this is never possible unless someone changes the TRUSTED_LOOKUP field
      throw new AssertionError("trusted lookup had no permission to define class", exception);
    }
  }

  /**
   * Holds all information required to generate code to box or unbox a primitive type.
   *
   * @param wrapperType       the descriptor of the wrapper type.
   * @param primitiveType     the descriptor of the primitive type.
   * @param toWrapperMethod   the method to call to convert from primitive to wrapper on the wrapper type.
   * @param fromWrapperMethod the method to call to convert from the wrapper to the primitive type.
   * @param toWrapperDesc     the descriptor of the method used to box the primitive.
   * @param fromWrapperDesc   the descriptor of the method used to unbox the primitive.
   * @since 4.0
   */
  private record PrimitiveWrapperConverter(
    @NonNull ClassDesc wrapperType,
    @NonNull ClassDesc primitiveType,
    @NonNull String toWrapperMethod,
    @NonNull String fromWrapperMethod,
    @NonNull MethodTypeDesc toWrapperDesc,
    @NonNull MethodTypeDesc fromWrapperDesc
  ) {

    /**
     * Constructs a new converter instance for the given wrapper and primitive type.
     *
     * @param wrapperTypeDesc   the type descriptor of the wrapper type.
     * @param primitiveTypeDesc the type descriptor of the primitive type.
     * @throws NullPointerException if the given wrapper or primitive type is null.
     */
    private PrimitiveWrapperConverter(
      @NonNull ClassDesc wrapperTypeDesc,
      @NonNull ClassDesc primitiveTypeDesc
    ) {
      this(
        wrapperTypeDesc,
        primitiveTypeDesc,
        "valueOf", // e.g. Double.valueOf(d)
        String.format("%sValue", primitiveTypeDesc.displayName()), // e.g. doubleWrapper.doubleValue()
        MethodTypeDesc.of(wrapperTypeDesc, primitiveTypeDesc), // e.g. Double.valueOf(d): Double
        MethodTypeDesc.of(primitiveTypeDesc) // e.g. doubleValue(): double
      );
    }
  }
}
