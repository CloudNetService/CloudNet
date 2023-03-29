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

package eu.cloudnetservice.driver.document.gson.send;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import eu.cloudnetservice.driver.document.send.ElementVisitor;
import eu.cloudnetservice.driver.document.send.element.ArrayElement;
import eu.cloudnetservice.driver.document.send.element.NullElement;
import eu.cloudnetservice.driver.document.send.element.ObjectElement;
import eu.cloudnetservice.driver.document.send.element.PrimitiveElement;
import lombok.NonNull;

record GsonArrayVisitor(@NonNull JsonArray targetArray) implements ElementVisitor {

  @Override
  public void visitEnd() {
  }

  @Override
  public void visitNull(@NonNull NullElement entry) {
    this.targetArray.add(JsonNull.INSTANCE);
  }

  @Override
  public void visitPrimitive(@NonNull PrimitiveElement entry) {
    var primitive = GsonPrimitiveConverter.wrapAsPrimitive(entry.innerValue());
    this.targetArray.add(primitive);
  }

  @Override
  public @NonNull ElementVisitor visitArray(@NonNull ArrayElement entry) {
    var array = new JsonArray();
    this.targetArray.add(array);
    return new GsonArrayVisitor(array);
  }

  @Override
  public @NonNull ElementVisitor visitObject(@NonNull ObjectElement entry) {
    var object = new JsonObject();
    this.targetArray.add(object);
    return new GsonObjectVisitor(object);
  }
}
