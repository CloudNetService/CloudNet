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

package de.dytanic.cloudnet.common.document.gson;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class BasicJsonDocPropertyable implements IJsonDocPropertyable {

  protected JsonDocument properties = new JsonDocument();

  @Override
  public <E> IJsonDocPropertyable setProperty(JsonDocProperty<E> docProperty, E val) {
    this.properties.setProperty(docProperty, val);
    return this;
  }

  @Override
  public <E> E getProperty(JsonDocProperty<E> docProperty) {
    return this.properties.getProperty(docProperty);
  }

  @Override
  public <E> IJsonDocPropertyable removeProperty(JsonDocProperty<E> docProperty) {
    this.properties.removeProperty(docProperty);
    return this;
  }

  @Override
  public <E> boolean hasProperty(JsonDocProperty<E> docProperty) {
    return docProperty.tester.test(this.properties);
  }

  @Override
  public JsonDocument getProperties() {
    return this.properties;
  }
}
