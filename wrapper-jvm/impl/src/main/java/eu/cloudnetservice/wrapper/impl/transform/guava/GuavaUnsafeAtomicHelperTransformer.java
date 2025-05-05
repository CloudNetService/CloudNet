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
import java.lang.constant.ConstantDescs;
import lombok.NonNull;

/**
 * A transformer for the {@code ServerProcessImpl} class in Minestom which inserts a call to {@code System.exit} before
 * the last return statement in the {@code stop} method to ensure a clean shutdown of the wrapper process.
 *
 * @since 4.0
 */
public final class GuavaUnsafeAtomicHelperTransformer implements ClassTransformer {

  private static final String CNI_UNSAFE_ATOMIC_HELPER =
    GuavaTransformUtil.buildFullClassName("util/concurrent/AbstractFuture$UnsafeAtomicHelper");
  private static final CodeTransform CT_REPLACE_WITH_EXCEPTION =
    GuavaTransformUtil.replaceWithExceptionTransform("uses unsafe methods which are discouraged starting from java 24");

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ClassTransform provideClassTransform(@NonNull ClassModel original) {
    return ClassTransform.transformingMethodBodies(
      methodModel -> methodModel.methodName().equalsString(ConstantDescs.CLASS_INIT_NAME),
      CT_REPLACE_WITH_EXCEPTION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull TransformWillingness classTransformWillingness(@NonNull String internalClassName) {
    var isUnsafeAtomicHelper = internalClassName.endsWith(CNI_UNSAFE_ATOMIC_HELPER);
    return isUnsafeAtomicHelper ? TransformWillingness.ACCEPT : TransformWillingness.REJECT;
  }
}
