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

package eu.cloudnetservice.driver.network.buffer;

import eu.cloudnetservice.driver.network.object.ObjectMapper;
import java.lang.reflect.Type;
import lombok.NonNull;

/**
 * Represents an object which can be written into a DataBuf but not using the default way the object mapper takes.
 * Therefore, this class provides the traditional read and write methods to (de-) serialize an object from a DataBuf.
 * <p>
 * A class which implements this interface is required to contain a no-args constructor which contains no special logic
 * for the data reader to create an empty instance of it. The data of the buffer is then filled in using the read
 * method.
 *
 * @see ObjectMapper#writeObject(DataBuf.Mutable, Object)
 * @see ObjectMapper#readObject(DataBuf, Type)
 * @since 4.0
 */
public interface DataBufable {

  /**
   * Writes all needed data from this object into the given data buf.
   *
   * @param dataBuf the buffer to write the data to.
   * @throws NullPointerException if the given buffer is null.
   */
  void writeData(@NonNull DataBuf.Mutable dataBuf);

  /**
   * Reads all data of this object from the given buffer. The deserializer uses the no-args constructor which must be
   * available in this class and creates an empty instance of this class. This method is then responsible to fill the
   * object data.
   * <p>
   * The given buffer should NEVER be read from more than written into the buffer in the write method.
   *
   * @param dataBuf the data buf to read the class data from.
   * @throws NullPointerException if the given buffer is null.
   */
  void readData(@NonNull DataBuf dataBuf);
}
