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

package eu.cloudnetservice.wrapper.impl.transform.bukkit;

import eu.cloudnetservice.wrapper.transform.ClassTransformer;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeTransform;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Field;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * A transformer implementation that rewrites the {@code setFailsafeFieldValue} method in the {@code ReflectionUtils}
 * class on old FAWE versions. This is due to the fact that the method uses illegal reflection in the attempt to set a final field which is
 * not possible anymore on newer java versions.
 */
@ApiStatus.Internal
public final class FAWEReflectionUtilsTransformer implements ClassTransformer {

  private static final String CNI_REFLECTION_UTILS = "com/boydti/fawe/util/ReflectionUtils";
  private static final String MN_SET_FAILSAFE_FIELD_VALUE = "setFailsafeFieldValue";
  private static final ClassDesc CD_FIELD = ClassDesc.of(Field.class.getName());
  private static final String MN_FIELD_SET_ACCESSIBLE = "setAccessible";
  private static final MethodTypeDesc MTD_SET_ACCESSIBLE = MethodTypeDesc.of(ConstantDescs.CD_void,
    ConstantDescs.CD_boolean);
  private static final String MN_FIELD_SET = "set";
  private static final MethodTypeDesc MTD_SET = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_Object,
    ConstantDescs.CD_Object);

  /**
   * Constructs a new instance of this transformer, usually done via SPI.
   */
  public FAWEReflectionUtilsTransformer() {
    // used by SPI
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ClassTransform provideClassTransform() {
    CodeTransform codeTransform = (builder, _) -> {
      // field.setAccessible(true);
      builder.aload(0).iconst_1();
      builder.invokevirtual(CD_FIELD, MN_FIELD_SET_ACCESSIBLE, MTD_SET_ACCESSIBLE);

      // field.set(instance, value);
      builder.aload(0).aload(1).aload(2);
      builder.invokevirtual(CD_FIELD, MN_FIELD_SET, MTD_SET);

      // return;
      builder.return_();
    };
    return ClassTransform.transformingMethodBodies(
      mm -> mm.methodName().equalsString(MN_SET_FAILSAFE_FIELD_VALUE),
      codeTransform);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull TransformWillingness classTransformWillingness(@NonNull String internalClassName) {
    var isFaweReflectionUtils = internalClassName.equals(CNI_REFLECTION_UTILS);
    return isFaweReflectionUtils ? TransformWillingness.ACCEPT_ONCE : TransformWillingness.REJECT;
  }
}
