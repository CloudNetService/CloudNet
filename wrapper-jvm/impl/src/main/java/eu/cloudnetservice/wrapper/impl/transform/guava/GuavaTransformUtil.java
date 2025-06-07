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
   * Builds a full internal class name of a guava class based on the given class name suffix.
   *
   * @param internalClassNameSuffix the suffix of the class name to build.
   * @return a full guava class name based on the given class name suffix.
   * @throws NullPointerException if the given class name suffix is null.
   */
  public static @NonNull String buildFullClassName(@NonNull String internalClassNameSuffix) {
    return GUAVA_BASE_PACKAGE_NAME + "/" + internalClassNameSuffix;
  }

  /**
   * Constructs a new code transform instance that replaces the full target method code with the throw of an exception.
   *
   * @return a new code transform instance that replaces the full target method code with the throw of an exception.
   */
  public static @NonNull CodeTransform replaceWithExceptionTransform() {
    return (builder, _) -> builder
      .new_(CD_UNSUPPORTED_OP_EX)
      .dup()
      .ldc("uses unsafe methods which are discouraged starting from java 24")
      .invokespecial(CD_UNSUPPORTED_OP_EX, ConstantDescs.INIT_NAME, MTD_UNSUPPORTED_OP_EX_NEW)
      .athrow();
  }
}
