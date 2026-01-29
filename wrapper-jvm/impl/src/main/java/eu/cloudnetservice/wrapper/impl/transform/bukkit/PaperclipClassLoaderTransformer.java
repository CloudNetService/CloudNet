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
import org.jetbrains.annotations.Nullable;

/**
 * A transformer implementation that removes the paperclip call to get the parent class loader of the paperclip class
 * which results in the bootstrap class loader which does not have the files from the class path.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class PaperclipClassLoaderTransformer implements ClassTransformer {

  private static final String MAIN_METHOD_NAME = "main";
  private static final String CLASS_LOADER_GET_PARENT_METHOD = "getParent";
  private static final String CLASS_LOADER_INTERNAL_NAME = "java/lang/ClassLoader";
  private static final String PAPERCLIP_INDICATOR_METHOD = "extractAndApplyPatches";

  private final String mainClassPackage;

  /**
   * Constructs a new instance of this transformer, usually done via SPI.
   */
  public PaperclipClassLoaderTransformer() {
    var versionListPresent = ClassLoader.getSystemClassLoader().getResource("META-INF/versions.list") != null;
    this.mainClassPackage = versionListPresent ? findMainClassPackage() : null;
  }

  private static @Nullable String findMainClassPackage() {
    var command = System.getProperty("sun.java.command");
    if (command != null) {
      var mainClassName = command.split("\\s+", 2)[0].replace('.', '/');
      var lastSlashIndex = mainClassName.lastIndexOf('/');
      if (lastSlashIndex != -1) {
        return mainClassName.substring(0, lastSlashIndex);
      }
    }

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull TransformWillingness classTransformWillingness(@NonNull String internalClassName) {
    if (this.mainClassPackage == null) {
      return TransformWillingness.REJECT;
    }

    var lastSlashIndex = internalClassName.lastIndexOf('/');
    if (lastSlashIndex == this.mainClassPackage.length() && internalClassName.startsWith(this.mainClassPackage)) {
      return TransformWillingness.ACCEPT;
    }

    return TransformWillingness.REJECT;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ClassTransform provideClassTransform(@NonNull ClassModel original) {
    var indicatorMethod = original.methods()
      .stream()
      .anyMatch(method -> method.methodName().equalsString(PAPERCLIP_INDICATOR_METHOD));
    if (!indicatorMethod) {
      return ClassTransform.ACCEPT_ALL;
    }

    return ClassTransform.transformingMethodBodies(
      methodModel -> methodModel.methodName().equalsString(MAIN_METHOD_NAME)
        && methodModel.flags().has(AccessFlag.STATIC),
      (builder, element) -> {
        if (element instanceof InvokeInstruction invoke) {
          if (invoke.method().name().equalsString(CLASS_LOADER_GET_PARENT_METHOD)
            && invoke.owner().asInternalName().equals(CLASS_LOADER_INTERNAL_NAME)) {
            return;
          }
        }

        builder.with(element);
      }
    );
  }
}
