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

package eu.cloudnetservice.node.impl.module.classloader.transformer;

import eu.cloudnetservice.node.module.condition.ConditionContext;
import eu.cloudnetservice.node.module.metadata.ModuleMetadata;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import lombok.NonNull;

/**
 * Default implementation of a condition processing context.
 *
 * @param targetClass            the class of the method that is being processed.
 * @param targetMethodName       the name of the method that is being processed.
 * @param targetMethodDescriptor the descriptor of the method being processed.
 * @param moduleMetadata         the metadata of the module that defines the method.
 * @param moduleClassLoader      the class loader that loaded the class which is being processed.
 * @since 4.0
 */
record DefaultConditionContext(
  @NonNull ClassDesc targetClass,
  @NonNull String targetMethodName,
  @NonNull MethodTypeDesc targetMethodDescriptor,
  @NonNull ModuleMetadata moduleMetadata,
  @NonNull ClassLoader moduleClassLoader
) implements ConditionContext {

}
