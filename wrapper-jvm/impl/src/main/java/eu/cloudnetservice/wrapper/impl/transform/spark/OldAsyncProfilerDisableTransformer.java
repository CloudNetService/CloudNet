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

package eu.cloudnetservice.wrapper.impl.transform.spark;

import eu.cloudnetservice.utils.base.StringUtil;
import eu.cloudnetservice.wrapper.transform.ClassTransformer;
import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.MethodModel;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * A transformer that explicitly disables the spark async profiler integration on old spark versions.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class OldAsyncProfilerDisableTransformer implements ClassTransformer {

  private static final String MN_LOAD = "load";
  private static final String MN_IS_LINUX_MUSL = "isLinuxMusl";
  private static final String CNI_ASYNC_PROFILER_ACC_PREFIX = "me/lucko/spark/";
  private static final String CNI_ASYNC_PROFILER_ACC_SUFFIX = "/common/sampler/async/AsyncProfilerAccess";
  private static final ClassDesc CD_UNSUPPORTED_OP_EX = ClassDesc.of(UnsupportedOperationException.class.getName());
  private static final MethodTypeDesc MTD_UNSUPPORTED_OP_EX_NEW =
    MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_String);

  /**
   * Constructs a new instance of this transformer, usually done via SPI.
   */
  public OldAsyncProfilerDisableTransformer() {
    // used by SPI
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ClassTransform provideClassTransform() {
    return new AsyncProfilerAccessClassTransform();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull TransformWillingness classTransformWillingness(@NonNull String internalClassName) {
    var isAsyncProfilerAccessClass = internalClassName.startsWith(CNI_ASYNC_PROFILER_ACC_PREFIX)
      && internalClassName.endsWith(CNI_ASYNC_PROFILER_ACC_SUFFIX);
    return isAsyncProfilerAccessClass ? TransformWillingness.ACCEPT_ONCE : TransformWillingness.REJECT;
  }

  /**
   * A transformer which replaces the {@code load} method to always throw an exception on {@code AsyncProfilerAccess} in
   * case the async profiler is not supported.
   *
   * @since 4.0
   */
  private static final class AsyncProfilerAccessClassTransform implements ClassTransform {

    // holds if the "isLinuxMusl" method exists in AsyncProfilerAccess - the method was removed
    // alongside the async profiler 3 support which added the required java 23 support
    private boolean isLinuxMuslExists = false;
    // the bug only happens on amd64 systems, so on aarch system we can leave the profiler
    // enabled even when running on the old version of it
    private boolean isLinuxAarch64 = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void atStart(@NonNull ClassBuilder builder) {
      var classModel = builder.original().orElseThrow(() -> new IllegalStateException("original not preset on remap"));
      this.isLinuxMuslExists = classModel.methods().stream().anyMatch(methodModel -> {
        var isStatic = methodModel.flags().has(AccessFlag.STATIC);
        return isStatic && methodModel.methodName().equalsString(MN_IS_LINUX_MUSL);
      });

      var arch = StringUtil.toLower(System.getProperty("os.arch"));
      var osName = StringUtil.toLower(System.getProperty("os.name"));
      this.isLinuxAarch64 = osName.equals("linux") && arch.equals("aarch64");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(@NonNull ClassBuilder builder, @NonNull ClassElement element) {
      if (element instanceof MethodModel mm
        && !this.isLinuxAarch64
        && this.isLinuxMuslExists
        && mm.flags().has(AccessFlag.STATIC)
        && mm.methodName().equalsString(MN_LOAD)) {
        builder.withMethodBody(mm.methodName(), mm.methodType(), mm.flags().flagsMask(), code -> code
          .new_(CD_UNSUPPORTED_OP_EX)
          .dup()
          .ldc("this version of spark uses a version of async-profiler which does not support java 23+")
          .invokespecial(CD_UNSUPPORTED_OP_EX, ConstantDescs.INIT_NAME, MTD_UNSUPPORTED_OP_EX_NEW)
          .athrow());
      } else {
        builder.with(element);
      }
    }
  }
}
