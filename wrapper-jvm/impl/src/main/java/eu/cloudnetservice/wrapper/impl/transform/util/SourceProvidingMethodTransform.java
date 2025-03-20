/*
 * Copyright 2019-2025 CloudNetService team & contributors
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

package eu.cloudnetservice.wrapper.impl.transform.util;

import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.MethodModel;
import java.lang.classfile.MethodTransform;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * A utility class transform that transform methods that are passing the provided filter function.
 *
 * @param filter           the filter that a method must pass to be transformed.
 * @param transformFactory the factory providing the method transform to apply to a filtered method.
 */
@ApiStatus.Internal
public record SourceProvidingMethodTransform(
  @NonNull Predicate<MethodModel> filter,
  @NonNull Function<MethodModel, MethodTransform> transformFactory
) implements ClassTransform {

  /**
   * {@inheritDoc}
   */
  @Override
  public void accept(@NonNull ClassBuilder builder, @NonNull ClassElement element) {
    if (element instanceof MethodModel methodModel && this.filter.test(methodModel)) {
      var transform = this.transformFactory.apply(methodModel);
      builder.transformMethod(methodModel, transform);
    } else {
      builder.with(element);
    }
  }
}
