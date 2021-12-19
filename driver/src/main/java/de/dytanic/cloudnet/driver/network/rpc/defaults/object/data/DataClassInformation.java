/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.driver.network.rpc.defaults.object.data;

import de.dytanic.cloudnet.driver.network.rpc.annotation.RPCIgnore;
import de.dytanic.cloudnet.driver.network.rpc.defaults.object.data.DataClassInvokerGenerator.DataClassInformationWriter;
import de.dytanic.cloudnet.driver.network.rpc.defaults.object.data.DataClassInvokerGenerator.DataClassInstanceCreator;
import de.dytanic.cloudnet.driver.network.rpc.exception.MissingAllArgsConstructorException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.NonNull;

public class DataClassInformation {

  private final DataClassInstanceCreator instanceCreator;
  private final DataClassInformationWriter informationWriter;

  public DataClassInformation(DataClassInstanceCreator creator, DataClassInformationWriter writer) {
    this.instanceCreator = creator;
    this.informationWriter = writer;
  }

  public static @NonNull DataClassInformation createClassInformation(
    @NonNull Class<?> clazz,
    @NonNull DataClassInvokerGenerator generator
  ) {
    // get all types of the fields we want to include into the constructor lookup
    var includedFields = collectFields(clazz);
    // transform the included fields to the required type arrays
    var types = transformToArray(Type.class, includedFields, Field::getGenericType);
    Class<?>[] arguments = transformToArray(Class.class, includedFields, Field::getType);
    // try to find a constructor with all arguments
    try {
      clazz.getDeclaredConstructor(arguments);
    } catch (NoSuchMethodException exception) {
      throw new MissingAllArgsConstructorException(clazz, arguments);
    }
    // generate the constructor invoker for the argument types
    var instanceCreator = generator.createInstanceCreator(clazz, types);
    var informationWriter = generator.createWriter(clazz, includedFields);
    // done
    return new DataClassInformation(instanceCreator, informationWriter);
  }

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

  public @NonNull DataClassInstanceCreator instanceCreator() {
    return this.instanceCreator;
  }

  public @NonNull DataClassInformationWriter informationWriter() {
    return this.informationWriter;
  }
}
