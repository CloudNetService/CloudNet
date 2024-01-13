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

package eu.cloudnetservice.driver.util.asm;

import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * A small helper class that helps keeping track of bytecode stack indices while pushing/loading from it.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class StackIndexHelper {

  private int nextVarIndex;

  /**
   * Constructs a new stack index helper instance.
   *
   * @param initialVarIndex the initial var index for the first element to load/store from/to the stack.
   */
  private StackIndexHelper(int initialVarIndex) {
    this.nextVarIndex = initialVarIndex;
  }

  /**
   * Creates a new stack index helper which uses 0 as the initial index.
   *
   * @return a stack index helper that uses 0 as the initial index.
   */
  public static @NonNull StackIndexHelper create() {
    return create(0);
  }

  /**
   * Constructs a new stack index helper with the given initial var index.
   *
   * @param initialVarIndex the initial var index for the first element to load/store from/to the stack.
   * @return a stack index helper that uses the given initial var index.
   */
  public static @NonNull StackIndexHelper create(int initialVarIndex) {
    return new StackIndexHelper(initialVarIndex);
  }

  /**
   * Skips the next (one) element of the current stack. This method implies that the next element has a size of one.
   *
   * @return this index helper, for chaining.
   */
  public @NonNull StackIndexHelper skipNext() {
    return this.skip(1);
  }

  /**
   * Skips the given amount of indices and uses the resulting value as the next var index to read/write from/to.
   *
   * @param depth the stack depth to skip.
   * @return this index helper, for chaining.
   */
  public @NonNull StackIndexHelper skip(int depth) {
    this.nextVarIndex += depth;
    return this;
  }

  /**
   * Stores an element of the given type on the stack using the appropriate opcode based on the given type and skips to
   * the next var index based on the size of the type.
   *
   * @param methodVisitor the method visitor to notify about the store operation.
   * @param type          the type to store on the stack.
   * @return this index helper, for chaining.
   * @throws NullPointerException if the given visitor or type is null.
   */
  public @NonNull StackIndexHelper push(@NonNull MethodVisitor methodVisitor, @NonNull Class<?> type) {
    var internalType = Type.getType(type);
    return this.push(methodVisitor, internalType);
  }

  /**
   * Stores an element of the given type on the stack using the appropriate opcode based on the given type and skips to
   * the next var index based on the size of the type.
   *
   * @param methodVisitor the method visitor to notify about the store operation.
   * @param type          the type to store on the stack.
   * @return this index helper, for chaining.
   * @throws NullPointerException if the given visitor or type is null.
   */
  public @NonNull StackIndexHelper push(@NonNull MethodVisitor methodVisitor, @NonNull Type type) {
    // store the element on the stack using the associated opcode
    var opcode = type.getOpcode(Opcodes.ISTORE);
    methodVisitor.visitVarInsn(opcode, this.nextVarIndex);

    // skip to the next var index based on the size of the type
    return this.skip(type.getSize());
  }

  /**
   * Loads an element of the given type onto the stack using the appropriate opcode based on the given type and skips to
   * the next var index based on the size of the type.
   *
   * @param methodVisitor the method visitor to notify about the load operation.
   * @param type          the type to load from the stack.
   * @return this index helper, for chaining.
   * @throws NullPointerException if the given visitor or type is null.
   */
  public @NonNull StackIndexHelper load(@NonNull MethodVisitor methodVisitor, @NonNull Class<?> type) {
    var internalType = Type.getType(type);
    return this.load(methodVisitor, internalType);
  }

  /**
   * Loads an element of the given type onto the stack using the appropriate opcode based on the given type and skips to
   * the next var index based on the size of the type.
   *
   * @param methodVisitor the method visitor to notify about the load operation.
   * @param type          the type to load from the stack.
   * @return this index helper, for chaining.
   * @throws NullPointerException if the given visitor or type is null.
   */
  public @NonNull StackIndexHelper load(@NonNull MethodVisitor methodVisitor, @NonNull Type type) {
    // load the element from the stack using the associated opcode
    var opcode = type.getOpcode(Opcodes.ILOAD);
    methodVisitor.visitVarInsn(opcode, this.nextVarIndex);

    // skip to the next var index based on the size of the type
    return this.skip(type.getSize());
  }
}
