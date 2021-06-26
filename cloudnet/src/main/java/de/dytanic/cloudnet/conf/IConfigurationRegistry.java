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

package de.dytanic.cloudnet.conf;

import java.lang.reflect.Type;

public interface IConfigurationRegistry {

  IConfigurationRegistry put(String key, Object object);

  IConfigurationRegistry put(String key, String string);

  IConfigurationRegistry put(String key, Number number);

  IConfigurationRegistry put(String key, Boolean bool);

  IConfigurationRegistry put(String key, byte[] bytes);

  IConfigurationRegistry remove(String key);

  boolean contains(String key);

  <T> T getObject(String key, Class<T> clazz);

  <T> T getObject(String key, Type type);

  String getString(String key);

  String getString(String key, String def);

  Integer getInt(String key);

  Integer getInt(String key, Integer def);

  Double getDouble(String key);

  Double getDouble(String key, Double def);

  Short getShort(String key);

  Short getShort(String key, Short def);

  Long getLong(String key);

  Long getLong(String key, Long def);

  Boolean getBoolean(String key);

  Boolean getBoolean(String key, Boolean def);

  byte[] getBytes(String key);

  byte[] getBytes(String key, byte[] bytes);

  IConfigurationRegistry save();

  IConfigurationRegistry load();

}
