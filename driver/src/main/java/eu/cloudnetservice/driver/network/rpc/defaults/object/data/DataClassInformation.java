/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.network.rpc.defaults.object.data;

import eu.cloudnetservice.driver.network.rpc.annotation.RPCIgnore;
import eu.cloudnetservice.driver.network.rpc.exception.MissingAllArgsConstructorException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.NonNull;

/**
 * Holds all information about a data class and is able to create an instance from a network buffer and serializing an
 * instance to a network buffer.
 *
 * @since 4.0
 */
public class DataClassInformation {

  private final DataClassInvokerGenerator.DataClassInstanceCreator instanceCreator;
  private final DataClassInvokerGenerator.DataClassInformationWriter informationWriter;

  /**
   * Constructs a new data class information instance.
   *
   * @param creator the instance creator for the class.
   * @param writer  the instance write for the class.
   * @throws NullPointerException if either the writer or creator is null.
   */
  protected DataClassInformation(
    @NonNull DataClassInvokerGenerator.DataClassInstanceCreator creator,
    @NonNull DataClassInvokerGenerator.DataClassInformationWriter writer
  ) {
    this.instanceCreator = creator;
    this.informationWriter = writer;
  }

  /**
   * Constructs a data class information based on the given class using the given generator. This class pre evaluates if
   * the data class creation is possible and collects all fields of the data class, which will be interpreted by the
   * generator in the required way. This method ensures that only fields which should get included are passed to the
   * data class generator.
   *
   * @param clazz the class to build the information for.
   * @return the created data class information for the given class.
   * @throws NullPointerException               if either the given class or generator is null.
   * @throws MissingAllArgsConstructorException if no constructor with all field types exists in the given data class.
   */
  public static @NonNull DataClassInformation createClassInformation(@NonNull Class<?> clazz) {
    // get all types of the fields we want to include into the constructor lookup
    var includedFields = collectFields(clazz);
    // transform the included fields to the required type arrays
    var types = transformToArray(Type.class, includedFields, Field::getGenericType);
    var arguments = transformToArray(Class.class, includedFields, Field::getType);
    // try to find a constructor with all arguments
    try {
      clazz.getDeclaredConstructor(arguments);
    } catch (NoSuchMethodException exception) {
      throw new MissingAllArgsConstructorException(clazz, arguments);
    }
    // generate the constructor invoker for the argument types
    var instanceCreator = DataClassInvokerGenerator.createInstanceCreator(clazz, types);
    var informationWriter = DataClassInvokerGenerator.createWriter(clazz, includedFields);
    // done
    return new DataClassInformation(instanceCreator, informationWriter);
  }

  /**
   * Collects all fields which are included for rpc transmitting.
   *
   * @param clazz the data class to collect the fields of.
   * @return all fields which are included for rpc transmitting.
   * @throws NullPointerException if the given data class is null.
   */
  protected static @NonNull List<Field> collectFields(@NonNull Class<?> clazz) {
    List<Field> result = new ArrayList<>();

    var processing = clazz;
    do {
      for (var field : processing.getDeclaredFields()) {
        if (!Modifier.isTransient(field.getModifiers())
          && !Modifier.isStatic(field.getModifiers())
          && !field.isAnnotationPresent(RPCIgnore.class)
        ) {
          result.add(field);
        }
      }
    } while ((processing = processing.getSuperclass()) != Object.class);

    return result;
  }

  /**
   * Transforms the given list into an array using the provided mapping function.
   *
   * @param outType     the type of the array after transforming.
   * @param in          the list of objects to transform to an array.
   * @param transformer the transformer to transform between the list entry and the array entry.
   * @param <T>         the generic input type.
   * @param <O>         the generic output type.
   * @return a new array containing all elements from the input array after being applied to the given transformer.
   * @throws NullPointerException if one of the given arguments is null.
   */
  @SuppressWarnings("unchecked")
  protected static @NonNull <T, O> O[] transformToArray(
    @NonNull Class<O> outType,
    @NonNull List<T> in,
    @NonNull Function<T, O> transformer
  ) {
    var resultArray = (O[]) Array.newInstance(outType, in.size());
    for (var i = 0; i < in.size(); i++) {
      resultArray[i] = transformer.apply(in.get(i));
    }
    return resultArray;
  }

  /**
   * Get the instance creator for the underlying data class.
   *
   * @return the instance creator.
   */
  public @NonNull DataClassInvokerGenerator.DataClassInstanceCreator instanceCreator() {
    return this.instanceCreator;
  }

  /**
   * Get the instance writer for the underlying data class.
   *
   * @return the instance writer.
   */
  public @NonNull DataClassInvokerGenerator.DataClassInformationWriter informationWriter() {
    return this.informationWriter;
  }
}
