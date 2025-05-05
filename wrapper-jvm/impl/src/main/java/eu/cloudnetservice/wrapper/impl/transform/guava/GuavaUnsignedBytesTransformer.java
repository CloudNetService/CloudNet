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
 *
 */
public final class GuavaUnsignedBytesTransformer implements ClassTransformer {

  private static final String MN_GET_UNSAFE = "getUnsafe";
  private static final CodeTransform CT_REPLACE_WITH_EXCEPTION =
    GuavaTransformUtil.replaceWithExceptionTransform("uses unsafe methods which are discouraged starting from java 24");
  private static final String CNI_UNSIGNED_BYTES_UNSAFE_COMPARATOR =
    GuavaTransformUtil.buildFullClassName("primitives/UnsignedBytes$LexicographicalComparatorHolder$UnsafeComparator");

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
    var isUnsafeAtomicHelper = internalClassName.endsWith(CNI_UNSIGNED_BYTES_UNSAFE_COMPARATOR);
    return isUnsafeAtomicHelper ? TransformWillingness.ACCEPT : TransformWillingness.REJECT;
  }
}
