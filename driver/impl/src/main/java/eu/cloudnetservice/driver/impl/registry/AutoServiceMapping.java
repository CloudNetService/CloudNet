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

package eu.cloudnetservice.driver.impl.registry;

import java.io.DataInput;
import java.io.IOException;
import lombok.NonNull;

/**
 * A mapping of all properties set in an {@link eu.cloudnetservice.driver.registry.AutoService} annotation on an
 * implementation class. This class is deserialized from information provided during compilation via an annotation
 * processor.
 *
 * @param serviceType          the service type that is implemented by the implementation type.
 * @param implementationType   the type that implements the given service type.
 * @param serviceName          the name that should be used for the service registration.
 * @param singletonService     if the service instance should be allocated once or every time it is retrieved.
 * @param markServiceAsDefault if the service should be marked as the default service after registration.
 * @since 4.0
 */
// counterpart of eu.cloudnetservice.driver.ap.registry.AutoServiceMapping
record AutoServiceMapping(
  @NonNull Class<?> serviceType,
  @NonNull Class<?> implementationType,
  @NonNull String serviceName,
  boolean singletonService,
  boolean markServiceAsDefault
) {

  /**
   * Deserializes an auto service mapping from the given data input. Class references are loaded using the class loader
   * of the given caller class. The data input must be prefixed with a single byte indicating the data version followed
   * by all the information needed to construct this mapping.
   *
   * @param caller    the caller that requested deserialization of the mappings.
   * @param dataInput the data input from which the mappings should be deserialized.
   * @return a deserialized auto service mapping based on the given data input.
   * @throws IOException            if an I/O error occurs while reading the mapping.
   * @throws ClassNotFoundException if a referenced class in the mapping cannot be resolved.
   * @throws NullPointerException   if the given caller class or data input is null.
   * @throws IllegalStateException  if an unknown auto service mapping data version is provided.
   */
  public static @NonNull AutoServiceMapping deserialize(
    @NonNull Class<?> caller,
    @NonNull DataInput dataInput
  ) throws IOException, ClassNotFoundException {
    var version = dataInput.readByte();
    if (version == 0x01) {
      var serviceType = Class.forName(dataInput.readUTF(), false, caller.getClassLoader());
      var implementationType = Class.forName(dataInput.readUTF(), false, caller.getClassLoader());
      return new AutoServiceMapping(
        serviceType,
        implementationType,
        dataInput.readUTF(),
        dataInput.readBoolean(),
        dataInput.readBoolean());
    } else {
      throw new IllegalStateException("Illegal auto service mapping version " + version);
    }
  }
}
