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

package eu.cloudnetservice.driver.impl.network.object.serializers;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.object.ObjectMapper;
import eu.cloudnetservice.driver.network.object.ObjectSerializer;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import java.lang.reflect.Type;
import lombok.NonNull;

/**
 * An object serializer which can write and read a json document to/from the buffer.
 *
 * @since 4.0
 */
public final class DocumentObjectSerializer implements ObjectSerializer<Document> {

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Object read(
    @NonNull DataBuf source,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    var documentFactoryName = source.readString();
    var documentFactory = ServiceRegistry.registry().instance(DocumentFactory.class, documentFactoryName);
    return documentFactory.parse(source);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(
    @NonNull DataBuf.Mutable dataBuf,
    @NonNull Document object,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    dataBuf.writeString(object.factoryName());
    object.writeTo(dataBuf);
  }
}
