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

import eu.cloudnetservice.wrapper.impl.transform.ClassTransformer;
import java.lang.classfile.Annotation;
import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.MethodModel;
import java.lang.classfile.MethodTransform;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import java.lang.classfile.attribute.DeprecatedAttribute;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.AccessFlag;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * Transforms methods and fields in {@code sun.misc.Unsafe} to use save and supported methods instead.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class UnsafeTransformer implements ClassTransformer {

  // the current package name
  private static final String PKG = UnsafeTransformer.class.getPackageName();

  private static final String CNI_UNSAFE = "sun/misc/Unsafe";
  private static final ClassDesc CD_UNSAFE = ClassDesc.ofInternalName(CNI_UNSAFE);

  private static final ClassDesc CD_DEPRECATED = ClassDesc.of(Deprecated.class.getName());
  private static final ClassDesc CD_UNSAFE_DELEGATE = ClassDesc.of(PKG, "UnsafeReplacementDelegate");

  // descriptors for UnsafeUsageTraceLogger and UnsafeUsageTraceLogger.traceUnsafeUsage(String, String)
  private static final String MN_UNSAFE_TRACE = "traceUnsafeUsage";
  private static final ClassDesc CD_UNSAFE_TRACER = ClassDesc.of(PKG, "UnsafeUsageTraceLogger");
  private static final MethodTypeDesc MTD_UNSAFE_TRACE =
    MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_String, ConstantDescs.CD_String);

  /**
   * Constructs a new instance of this transformer, usually done via SPI.
   */
  public UnsafeTransformer() {
    var transformerDisabled = Boolean.getBoolean("cloudnet.wrapper.unsafe-transform-disabled");
    if (transformerDisabled) {
      throw new UnsupportedOperationException("transformer disabled via system property");
    }
  }

  /**
   * Inits the instrumentation to use for defining the classes in the bootstrap class loader.
   *
   * @param instrumentation the instrumentation to use.
   * @throws NullPointerException  if the given classloader is null.
   * @throws IllegalStateException if the instrumentation is already initialized.
   */
  public static void init(@NonNull Instrumentation instrumentation) {
    UnsafeReplacementDefiner.init(instrumentation);
  }

  /**
   * Generates the invocation and return instruction into the given code builder to call the given unsafe replacement
   * method in {@code UnsafeReplacementDelegate}.
   *
   * @param codeBuilder the code builder to generate the invocation and return instruction into.
   * @param methodName  the name of the replacement method to call.
   * @param desc        the descriptor of the replacement method to call.
   * @throws NullPointerException if the given code builder, method name or descriptor is null.
   */
  private static void callReplacementMethod(
    @NonNull CodeBuilder codeBuilder,
    @NonNull String methodName,
    @NonNull MethodTypeDesc desc
  ) {
    // load the method parameters onto the stack
    var paramCount = desc.parameterCount();
    for (var index = 0; index < paramCount; index++) {
      var paramDesc = desc.parameterType(index);
      var paramSlot = codeBuilder.parameterSlot(index);
      var paramTypeKind = TypeKind.fromDescriptor(paramDesc.descriptorString());
      codeBuilder.loadLocal(paramTypeKind, paramSlot);
    }

    // call the replacement method
    codeBuilder.invokestatic(CD_UNSAFE_DELEGATE, methodName, desc, false);

    // return the result of the replacement method invocation
    var returnDesc = desc.returnType();
    var returnTypeKind = TypeKind.fromDescriptor(returnDesc.descriptorString());
    codeBuilder.return_(returnTypeKind);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ClassTransform provideClassTransform(@NonNull ClassModel original) {
    // only append the classes if they are actually required
    UnsafeReplacementDefiner.appendClassesToBootstrapClassLoader();

    // find all fields that have a replacement but are no longer existing in sun.misc.Unsafe
    var mapping = UnsafeReplacementMapping.load();
    var existingFields = original.fields().stream().map(field -> {
      var name = field.fieldName().stringValue();
      var desc = field.fieldTypeSymbol();
      return new UnsafeReplacementMapping.Key(name, desc);
    }).collect(Collectors.toUnmodifiableSet());
    var nonExistingFields = mapping.fieldReplacements().stream()
      .filter(replacement -> !existingFields.contains(replacement.key()))
      .toList();

    return new UnsafeClassTransform(mapping, nonExistingFields);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull TransformWillingness classTransformWillingness(@NonNull String internalClassName) {
    var isUnsafe = internalClassName.equals(CNI_UNSAFE);
    return isUnsafe ? TransformWillingness.ACCEPT_ONCE : TransformWillingness.REJECT;
  }

  /**
   * Class transform implementation that inserts replacement calls into {@code sun.misc.Unsafe}.
   *
   * @param replacementMapping the unsafe replacement mapping to use for this transform.
   * @param additionalFields   the additional fields to add to the class.
   * @since 4.0
   */
  private record UnsafeClassTransform(
    @NonNull UnsafeReplacementMapping replacementMapping,
    @NonNull Collection<UnsafeReplacementMapping.FieldReplacement> additionalFields
  ) implements ClassTransform {

    /**
     * {@inheritDoc}
     */
    @Override
    public void atStart(@NonNull ClassBuilder builder) {
      // put all non-existing fields into the class, all public static final and marked as deprecated
      for (var additionalField : this.additionalFields) {
        var deprecatedAnnotation = Annotation.of(CD_DEPRECATED);
        builder.withField(additionalField.sourceName(), additionalField.type(), fieldBuilder -> fieldBuilder
          .with(DeprecatedAttribute.of())
          .with(RuntimeVisibleAnnotationsAttribute.of(deprecatedAnnotation))
          .withFlags(AccessFlag.PUBLIC, AccessFlag.STATIC, AccessFlag.FINAL));
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(@NonNull ClassBuilder builder, @NonNull ClassElement element) {
      switch (element) {
        case MethodModel mm when mm.methodName().equalsString(ConstantDescs.CLASS_INIT_NAME) -> {
          // transforms the field initializers of replaced fields in <cinit>
          var codeTransform = new ClassInitTransform(this.replacementMapping, this.additionalFields);
          builder.transformMethod(mm, MethodTransform.transformingCode(codeTransform));
        }
        case MethodModel mm -> {
          var methodType = mm.methodTypeSymbol();
          var methodName = mm.methodName().stringValue();
          var replacementMethod = this.replacementMapping.replacementMethodName(methodName, methodType);
          if (replacementMethod != null) {
            // replacement method exists for the current method, replace
            var codeTransform = new MethodReplacementTransform(methodName, replacementMethod, methodType);
            builder.transformMethod(mm, MethodTransform.transformingCode(codeTransform));
          } else {
            // method has no replacement, keep as-is
            builder.with(mm);
          }
        }
        default -> builder.with(element);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void atEnd(@NonNull ClassBuilder builder) {
      // put the remaining methods that no longer exist in sun.misc.Unsafe into the class
      var remainingMethods = this.replacementMapping.methodReplacements();
      for (var remainingMethod : remainingMethods) {
        var deprecatedAnnotation = Annotation.of(CD_DEPRECATED);
        builder.withMethod(
          remainingMethod.sourceName(),
          remainingMethod.type(),
          ClassFile.ACC_PUBLIC,
          methodBuilder -> methodBuilder
            .with(DeprecatedAttribute.of())
            .with(RuntimeVisibleAnnotationsAttribute.of(deprecatedAnnotation))
            .withCode(codeBuilder -> {
              // call the unsafe tracing method
              codeBuilder
                .ldc(remainingMethod.sourceName())
                .ldc(remainingMethod.type().descriptorString())
                .invokestatic(CD_UNSAFE_TRACER, MN_UNSAFE_TRACE, MTD_UNSAFE_TRACE, false);

              // call the replacement method for the inserted source method, this also inserts the return instruction
              callReplacementMethod(codeBuilder, remainingMethod.replacementName(), remainingMethod.type());
            }));
      }
    }
  }

  /**
   * Transforms the class init block of a class and replaces the field initializers with replaced ones.
   *
   * @param replacementMapping the unsafe replacement mapping to use for this transform.
   * @param additionalFields   the additional fields to add to the class.
   * @since 4.0
   */
  private record ClassInitTransform(
    @NonNull UnsafeReplacementMapping replacementMapping,
    @NonNull Collection<UnsafeReplacementMapping.FieldReplacement> additionalFields
  ) implements CodeTransform {

    /**
     * {@inheritDoc}
     */
    @Override
    public void atStart(@NonNull CodeBuilder builder) {
      // init all additionally added static final fields
      for (var additionalField : this.additionalFields) {
        builder
          .getstatic(CD_UNSAFE_DELEGATE, additionalField.replacementName(), additionalField.type())
          .putstatic(CD_UNSAFE, additionalField.sourceName(), additionalField.type());
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(@NonNull CodeBuilder builder, @NonNull CodeElement element) {
      if (element instanceof FieldInstruction inst && inst.opcode() == Opcode.PUTSTATIC) {
        var field = inst.field();
        var fieldType = field.typeSymbol();
        var fieldName = field.name().stringValue();
        var replacement = this.replacementMapping.replacementFieldName(fieldName, fieldType);
        if (replacement != null) {
          // pop the current resolved field value from the stack
          switch (field.width()) {
            case 1 -> builder.pop();
            case 2 -> builder.pop2();
          }

          // load the replacement field value onto the stack and put it into the field instead
          builder.getstatic(CD_UNSAFE_DELEGATE, replacement, fieldType).putstatic(CD_UNSAFE, fieldName, fieldType);
          return;
        }
      }

      builder.with(element);
    }
  }

  /**
   * Code transform to replace the body of an existing method with a call to our unsafe delegate replacement.
   *
   * @param originalName    the name of the original method that is being replaced.
   * @param replacementName the name of the replacement method to use.
   * @param desc            the descriptor of the method that is being replaced.
   * @since 4.0
   */
  private record MethodReplacementTransform(
    @NonNull String originalName,
    @NonNull String replacementName,
    @NonNull MethodTypeDesc desc
  ) implements CodeTransform {

    /**
     * {@inheritDoc}
     */
    @Override
    public void atStart(@NonNull CodeBuilder builder) {
      // call the unsafe tracing method
      builder
        .ldc(this.originalName)
        .ldc(this.desc.descriptorString())
        .invokestatic(CD_UNSAFE_TRACER, MN_UNSAFE_TRACE, MTD_UNSAFE_TRACE, false);

      // call the replacement method for the inserted source method, this also inserts the return instruction
      callReplacementMethod(builder, this.replacementName, this.desc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(@NonNull CodeBuilder builder, @NonNull CodeElement element) {
      // no-op to drop all existing code
    }
  }
}
