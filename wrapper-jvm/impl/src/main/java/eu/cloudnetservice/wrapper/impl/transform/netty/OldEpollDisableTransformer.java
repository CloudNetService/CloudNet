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

package eu.cloudnetservice.wrapper.impl.transform.netty;

import eu.cloudnetservice.wrapper.transform.ClassTransformer;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * A transformer that explicitly disables netty epoll on old versions bundled with Minecraft.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class OldEpollDisableTransformer implements ClassTransformer {

  private static final String MN_OLD_EPOLL_CREATE = "epollCreate";
  private static final String CNI_EPOLL = "io/netty/channel/epoll/Epoll";
  private static final String FN_UNAVAILABILITY_CAUSE = "UNAVAILABILITY_CAUSE";
  private static final ClassDesc CD_EPOLL = ClassDesc.ofInternalName(CNI_EPOLL);
  private static final ClassDesc CD_UNSUPPORTED_OP_EX = ClassDesc.of(UnsupportedOperationException.class.getName());
  private static final MethodTypeDesc MTD_UNSUPPORTED_OP_EX_NEW =
    MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_String);

  /**
   * Constructs a new instance of this transformer, usually done via SPI.
   */
  public OldEpollDisableTransformer() {
    // used by SPI
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ClassTransform provideClassTransform() {
    CodeTransform codeTransform = (codeBuilder, codeElement) -> {
      if (codeElement instanceof InvokeInstruction inst && inst.name().equalsString(MN_OLD_EPOLL_CREATE)) {
        // old versions of netty are invoking a method called "epollCreate", new ones are invoking "newEpollCreate"
        // so if we encounter the old method call, we just set the unavailability cause field & insert a return to
        // skip the remaining method body. This will mark Epoll as unavailable, and it will not be used
        codeBuilder
          .new_(CD_UNSUPPORTED_OP_EX)
          .dup()
          .ldc("Netty 4.0.X is incompatible with Java 9+")
          .invokespecial(CD_UNSUPPORTED_OP_EX, ConstantDescs.INIT_NAME, MTD_UNSUPPORTED_OP_EX_NEW)
          .putstatic(CD_EPOLL, FN_UNAVAILABILITY_CAUSE, CD_UNSUPPORTED_OP_EX)
          .return_();
      }
      codeBuilder.accept(codeElement);
    };
    return ClassTransform.transformingMethodBodies(
      mm -> mm.methodName().equalsString(ConstantDescs.CLASS_INIT_NAME),
      codeTransform);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull TransformWillingness classTransformWillingness(@NonNull String internalClassName) {
    var isEpollClass = internalClassName.equals(CNI_EPOLL);
    return isEpollClass ? TransformWillingness.ACCEPT_ONCE : TransformWillingness.REJECT;
  }
}
