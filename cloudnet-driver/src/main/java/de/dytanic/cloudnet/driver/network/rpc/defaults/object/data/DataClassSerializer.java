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

import com.google.common.base.Verify;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectMapper;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectSerializer;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataClassSerializer implements ObjectSerializer<Object> {

  private final DataClassInvokerGenerator generator = new DataClassInvokerGenerator();
  private final Map<Type, DataClassInformation> cachedClassInformation = new ConcurrentHashMap<>();

  @Override
  public @Nullable Object read(
    @NotNull DataBuf source,
    @NotNull Type type,
    @NotNull ObjectMapper caller
  ) {
    // ensure that the given type is a class
    Verify.verify(type instanceof Class<?>, "Cannot call data class serializer on non-class");
    // get the class information
    DataClassInformation information = this.cachedClassInformation.computeIfAbsent(
      type,
      $ -> DataClassInformation.createClassInformation((Class<?>) type, this.generator));
    // let the instance creator do the stuff
    return information.getInstanceCreator().makeInstance(source, caller);
  }

  @Override
  public void write(
    @NotNull DataBuf.Mutable dataBuf,
    @NotNull Object object,
    @NotNull Type type,
    @NotNull ObjectMapper caller
  ) {
    // ensure that the given type is a class
    Verify.verify(type instanceof Class<?>, "Cannot call data class serializer on non-class");
    // get the class information
    DataClassInformation information = this.cachedClassInformation.computeIfAbsent(
      type,
      $ -> DataClassInformation.createClassInformation((Class<?>) type, this.generator));
    // let the information writer do the stuff
    information.getInformationWriter().writeInformation(dataBuf, object, caller);
  }
}
