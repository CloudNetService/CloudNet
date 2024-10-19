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

package eu.cloudnetservice.driver.impl.document.gson.send;

import com.google.gson.JsonObject;
import eu.cloudnetservice.driver.document.send.ElementVisitor;
import eu.cloudnetservice.driver.document.send.element.ArrayElement;
import eu.cloudnetservice.driver.document.send.element.NullElement;
import eu.cloudnetservice.driver.document.send.element.ObjectElement;
import eu.cloudnetservice.driver.document.send.element.PrimitiveElement;
import lombok.NonNull;

/**
 * An element visitor to visit the root object element of a document send. This visitor only accepts calls to
 * {@link #visitObject(ObjectElement)}, all other calls with throw an {@link UnsupportedOperationException}.
 *
 * @param rootObject the root json object to put all child elements into.
 * @since 4.0
 */
public record GsonRootObjectVisitor(@NonNull JsonObject rootObject) implements ElementVisitor {

  /**
   * {@inheritDoc}
   */
  @Override
  public void visitEnd() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void visitNull(@NonNull NullElement entry) {
    throw new UnsupportedOperationException("not supported on root visitor");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void visitPrimitive(@NonNull PrimitiveElement entry) {
    throw new UnsupportedOperationException("not supported on root visitor");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ElementVisitor visitArray(@NonNull ArrayElement entry) {
    throw new UnsupportedOperationException("not supported on root visitor");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ElementVisitor visitObject(@NonNull ObjectElement entry) {
    return new GsonObjectVisitor(this.rootObject);
  }
}
