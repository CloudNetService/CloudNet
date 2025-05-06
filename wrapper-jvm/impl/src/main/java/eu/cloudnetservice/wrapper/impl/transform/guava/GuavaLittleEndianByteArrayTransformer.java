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

package eu.cloudnetservice.wrapper.impl.transform.guava;

import eu.cloudnetservice.wrapper.transform.ClassTransformer;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeTransform;
import java.lang.reflect.AccessFlag;
import lombok.NonNull;

/**
 * Transformer that disables the usage of sun.misc.Unsafe in guava.LittleEndianByteArray.
 *
 * @since 4.0
 */
public final class GuavaLittleEndianByteArrayTransformer implements ClassTransformer {

  private static final String MN_GET_UNSAFE = "getUnsafe";
  private static final String CNI_LITTLE_ENDIAN_UNSAFE_BYTE_ARRAY =
    GuavaTransformUtil.buildFullClassName("hash/LittleEndianByteArray$UnsafeByteArray");
  private static final CodeTransform CT_REPLACE_WITH_EXCEPTION = GuavaTransformUtil.replaceWithExceptionTransform();

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ClassTransform provideClassTransform(@NonNull ClassModel original) {
    return ClassTransform.transformingMethodBodies(
      mm -> mm.flags().has(AccessFlag.STATIC) && mm.methodName().equalsString(MN_GET_UNSAFE),
      CT_REPLACE_WITH_EXCEPTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull TransformWillingness classTransformWillingness(@NonNull String internalClassName) {
    var isUnsafeAtomicHelper = internalClassName.endsWith(CNI_LITTLE_ENDIAN_UNSAFE_BYTE_ARRAY);
    return isUnsafeAtomicHelper ? TransformWillingness.ACCEPT : TransformWillingness.REJECT;
  }
}
