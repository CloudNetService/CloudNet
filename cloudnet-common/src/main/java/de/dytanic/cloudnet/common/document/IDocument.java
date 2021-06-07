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

package de.dytanic.cloudnet.common.document;

import de.dytanic.cloudnet.common.document.gson.IJsonDocPropertyable;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

/**
 * A document is a wrapper to persistence data or read data in the heap or easy into the following implementation format
 * of this interface.
 */
public interface IDocument<R extends IDocument<?>> extends IJsonDocPropertyable, Serializable, IPersistable,
  IReadable, Iterable<String> {

  Collection<String> keys();

  int size();

  R clear();

  R remove(String key);

  boolean contains(String key);

  <T> T toInstanceOf(Class<T> clazz);

  <T> T toInstanceOf(Type clazz);


  R append(String key, Object value);

  R append(String key, Number value);

  R append(String key, Boolean value);

  R append(String key, String value);

  R append(String key, Character value);

  R append(String key, R value);

  R append(Properties properties);

  R append(Map<String, Object> map);

  R append(String key, Properties properties);

  R append(String key, byte[] bytes);

  R append(R t);

  R appendNull(String key);

  R getDocument(String key);

  int getInt(String key);

  double getDouble(String key);

  float getFloat(String key);

  byte getByte(String key);

  short getShort(String key);

  long getLong(String key);

  boolean getBoolean(String key);

  String getString(String key);

  char getChar(String key);

  BigDecimal getBigDecimal(String key);

  BigInteger getBigInteger(String key);

  Properties getProperties(String key);

  byte[] getBinary(String key);

  <T> T get(String key, Class<T> clazz);

  <T> T get(String key, Type type);


  default boolean isEmpty() {
    return this.size() == 0;
  }
}
