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

package eu.cloudnetservice.driver.inject;

import com.google.common.base.Preconditions;
import dev.derklaro.aerogel.binding.key.BindingKey;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * Some general utility that comes handy for some internal tasks.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class InjectUtil {

  private static final Object[] EMPTY_INSTANCE_ARRAY = new Object[0];
  private static final BindingKey<?>[] EMPTY_ELEMENT_ARRAY = new BindingKey[0];

  private InjectUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Builds an element from the generic type and annotations of each given parameter.
   *
   * @param parameters the parameters to build the element for.
   * @return an array of element each representing a given parameter, in order.
   * @throws NullPointerException if the given parameter array is null.
   */
  public static @NonNull BindingKey<?>[] buildElementsForParameters(@NonNull Parameter[] parameters) {
    return buildElementsForParameters(parameters, 0);
  }

  /**
   * Builds an element from the generic type and annotations of each given parameter.
   *
   * @param parameters the parameters to build the element for.
   * @param offset     the offset to begin construction from.
   * @return an array of element each representing a given parameter, in order.
   * @throws NullPointerException if the given parameter array is null.
   */
  public static @NonNull BindingKey<?>[] buildElementsForParameters(@NonNull Parameter[] parameters, int offset) {
    // return an empty element array if the given parameters are empty
    if (parameters.length <= offset) {
      return EMPTY_ELEMENT_ARRAY;
    }

    // construct the element array
    var params = Arrays.copyOfRange(parameters, offset, parameters.length);
    var elements = new BindingKey[params.length];
    for (int i = 0; i < params.length; i++) {
      var parameter = params[i];
      elements[i] = BindingKey.of(parameter.getParameterizedType()).selectQualifier(parameter.getAnnotations());
    }
    return elements;
  }

  /**
   * Finds all instances that are requested by the given element array in the given injection layer.
   *
   * @param layer the layer to retrieve the needed instances from.
   * @param keys  the keys to get the instances of.
   * @return the instances represented by the given elements, in order.
   * @throws NullPointerException if the given injection layer or elements array is null.
   */
  public static @NonNull Object[] findAllInstances(
    @NonNull InjectionLayer<?> layer,
    @NonNull BindingKey<?>[] keys
  ) {
    return findAllInstances(layer, keys, 0);
  }

  /**
   * Finds all instances that are requested by the given element array in the given injection layer.
   *
   * @param layer  the layer to retrieve the needed instances from.
   * @param keys   the keys to get the instances of.
   * @param offset the offset to start writing the elements from in the resulting array, must be bigger than zero.
   * @return the instances represented by the given elements, in order.
   * @throws NullPointerException     if the given injection layer or elements array is null.
   * @throws IllegalArgumentException if the given offset is smaller than zero.
   */
  public static @NonNull Object[] findAllInstances(
    @NonNull InjectionLayer<?> layer,
    @NonNull BindingKey<?>[] keys,
    int offset
  ) {
    Preconditions.checkArgument(offset >= 0, "offset must be >= 0");

    // return an empty array if the offset is 0 and no elements are present
    if (keys.length == 0 && offset == 0) {
      return EMPTY_INSTANCE_ARRAY;
    }

    // return an array of the expected size if an offset is present but no elements are present
    if (keys.length == 0) {
      return new Object[offset];
    }

    // find the instances
    var instances = new Object[keys.length + offset];
    for (int i = 0; i < keys.length; i++) {
      instances[i + offset] = layer.instance(keys[i]);
    }
    return instances;
  }
}
