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

import java.lang.classfile.CodeTransform;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

/**
 * Utility functions for use when transforming guava classes.
 */
final class GuavaTransformUtil {

  // join to prevent shadow from detecting this string and replacing it with the relocated package name
  private static final String GUAVA_BASE_PACKAGE_NAME = String.join("/", "com", "google", "common");

  private static final ClassDesc CD_UNSUPPORTED_OP_EX =
    ClassDesc.of(UnsupportedOperationException.class.getName());
  private static final MethodTypeDesc MTD_UNSUPPORTED_OP_EX_NEW =
    MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_String);

  private GuavaTransformUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * @param internalClassNameSuffix
   * @return
   */
  public static @NonNull String buildFullClassName(@NotNull String internalClassNameSuffix) {
    return GUAVA_BASE_PACKAGE_NAME + "/" + internalClassNameSuffix;
  }

  /**
   *
   * @param message
   * @return
   */
  public static @NonNull CodeTransform replaceWithExceptionTransform(@NonNull String message) {
    return (builder, _) -> builder
      .new_(CD_UNSUPPORTED_OP_EX)
      .dup()
      .ldc("uses unsafe methods which are discouraged starting from java 24")
      .invokespecial(CD_UNSUPPORTED_OP_EX, ConstantDescs.INIT_NAME, MTD_UNSUPPORTED_OP_EX_NEW)
      .athrow();
  }
}
