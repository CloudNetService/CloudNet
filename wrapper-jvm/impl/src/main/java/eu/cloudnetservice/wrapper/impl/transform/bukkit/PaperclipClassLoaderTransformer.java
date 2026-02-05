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

package eu.cloudnetservice.wrapper.impl.transform.bukkit;

import eu.cloudnetservice.wrapper.impl.transform.ClassTransformer;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.reflect.AccessFlag;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * A transformer that removes the {@code ClassLoader.getParent()} call from the paperclip main method, as it results in
 * the paper main class being loaded by the bootstrap class loader. This then causes, for example, plugins to not have
 * access to the wrapper classes (which reside on the class path and therefore on the system class loader).
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class PaperclipClassLoaderTransformer implements ClassTransformer {

  private static final String MAIN_METHOD_NAME = "main";
  private static final String CLASS_LOADER_GET_PARENT_METHOD = "getParent";
  private static final String CLASS_LOADER_INTERNAL_NAME = "java/lang/ClassLoader";
  private static final String PAPERCLIP_INDICATOR_METHOD = "extractAndApplyPatches";

  // need to check by package and not by exact class name, as the real paperclip main class is just
  // a dummy implementation to check if the correct java version for the paper version is being used
  private final String mainClassPackage;

  /**
   * Constructs a new instance of this transformer, usually done via SPI.
   */
  public PaperclipClassLoaderTransformer() {
    this.mainClassPackage = findPaperclipMainClassPackage();
  }

  /**
   * Finds the package where the main class of paperclip might be located in. Throws an exception in case no paperclip
   * is on the classpath or the main class package cannot be resolved.
   *
   * @return the package name of the paperclip main class.
   * @throws RuntimeException if no paperclip main package can be resolved.
   */
  private static @NonNull String findPaperclipMainClassPackage() {
    // check for the 'patches.list' file included in modern paperclip (since 1.18)
    // previously paperclip used instrumentation to add the paper jar to the system cl, making this transform obsolete
    var hasPatchesList = ClassLoader.getSystemClassLoader().getResource("META-INF/patches.list") != null;
    if (hasPatchesList) {
      // 'sun.java.command' holds the main class and arguments to supply to the main class, we use this
      // property to resolve the main class that is being invoked by the jvm
      var mainClassAndArgs = System.getProperty("sun.java.command", "");
      var mainClass = mainClassAndArgs.split(" ")[0];
      if (!mainClass.isBlank()) {
        var internalMainClassName = mainClass.replace('.', '/');
        var lastPackageDelimiter = internalMainClassName.lastIndexOf('/');
        if (lastPackageDelimiter != -1) {
          return internalMainClassName.substring(0, lastPackageDelimiter);
        }
      }
    }

    throw new RuntimeException("not running paperclip or main class not found");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull TransformWillingness classTransformWillingness(@NonNull String internalClassName) {
    var lastPkgDelimIdx = internalClassName.lastIndexOf('/');
    if (lastPkgDelimIdx == this.mainClassPackage.length() && internalClassName.startsWith(this.mainClassPackage)) {
      return TransformWillingness.ACCEPT;
    }

    return TransformWillingness.REJECT;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ClassTransform provideClassTransform(@NonNull ClassModel original) {
    var hasPaperclipMainClassIndicatorMethod = original.methods()
      .stream()
      .anyMatch(method -> method.methodName().equalsString(PAPERCLIP_INDICATOR_METHOD));
    if (!hasPaperclipMainClassIndicatorMethod) {
      return ClassTransform.ACCEPT_ALL;
    }

    return ClassTransform.transformingMethodBodies(
      methodModel -> methodModel.flags().has(AccessFlag.STATIC)
        && methodModel.methodName().equalsString(MAIN_METHOD_NAME),
      (builder, element) -> {
        if (element instanceof InvokeInstruction invokeInst
          && invokeInst.owner().asInternalName().equals(CLASS_LOADER_INTERNAL_NAME)
          && invokeInst.method().name().equalsString(CLASS_LOADER_GET_PARENT_METHOD)) {
          return;
        }

        builder.with(element);
      }
    );
  }
}
