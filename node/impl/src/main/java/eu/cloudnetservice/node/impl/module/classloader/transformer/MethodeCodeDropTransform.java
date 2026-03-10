/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.module.classloader.transformer;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import lombok.NonNull;

/**
 * Code transform implementation which drops the whole code of a method and only inserts a final return statement. Can
 * only be used for void methods.
 */
final class MethodeCodeDropTransform implements CodeTransform {

  static final CodeTransform INSTANCE = new MethodeCodeDropTransform();

  private MethodeCodeDropTransform() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void accept(@NonNull CodeBuilder builder, @NonNull CodeElement element) {
    // no-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void atEnd(@NonNull CodeBuilder builder) {
    builder.return_();
  }
}
