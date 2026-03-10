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

import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.registry.ServiceRegistryRegistration;
import eu.cloudnetservice.node.module.condition.ConditionContext;
import eu.cloudnetservice.node.module.condition.ConditionProcessor;
import eu.cloudnetservice.node.module.condition.KeepOnConditionFailure;
import eu.cloudnetservice.node.module.metadata.ModuleMetadata;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.classfile.Annotation;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassHierarchyResolver;
import java.lang.classfile.MethodModel;
import java.lang.classfile.MethodTransform;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.util.Collection;
import java.util.List;
import lombok.NonNull;

/**
 * Transformation processor for classes loaded by a module class loader.
 *
 * @since 4.0
 */
@Singleton
public final class ModuleClassTransformer {

  private static final ClassDesc CD_KEEP_ON_CONDITION_FAILURE = ClassDesc.of(KeepOnConditionFailure.class.getName());

  private final ServiceRegistry serviceRegistry;

  @Inject
  public ModuleClassTransformer(@NonNull ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * Transforms the given class, returning the updated class file bytes. Might return the exact input bytes if no
   * changes were applied to the class.
   *
   * @param classFileBytes    the raw bytes of the class to transform.
   * @param moduleClassLoader the class loader currently loading the class file.
   * @param moduleMetadata    the metadata of the module which owns the class that is being transformed.
   * @return the transformed class file bytes to load.
   * @throws NullPointerException if one of the given parameters is null.
   */
  public byte[] transformClass(
    byte[] classFileBytes,
    @NonNull ClassLoader moduleClassLoader,
    @NonNull ModuleMetadata moduleMetadata
  ) {
    // resolve all registered condition processors first. no need to do anything if no processors are registered
    var conditionProcessors = this.serviceRegistry.registrations(ConditionProcessor.class);
    if (conditionProcessors.isEmpty()) {
      return classFileBytes;
    }

    var classHierarchyResolver = ClassHierarchyResolver.ofClassLoading(moduleClassLoader);
    var classHierarchyResolverOption = ClassFile.ClassHierarchyResolverOption.of(classHierarchyResolver);
    var classFile = ClassFile.of(classHierarchyResolverOption);
    var classModel = classFile.parse(classFileBytes);
    return classFile.transformClass(classModel, (classBuilder, classElement) -> {
      if (!(classElement instanceof MethodModel methodModel)) {
        classBuilder.with(classElement);
        return;
      }

      // skip constructors and clinit method, cannot have annotations anyway
      if (methodModel.methodName().equalsString(ConstantDescs.INIT_NAME)
        || methodModel.methodName().equalsString(ConstantDescs.CLASS_INIT_NAME)) {
        classBuilder.with(classElement);
        return;
      }

      // check if the method defines any annotations
      var annotations = methodModel.findAttribute(Attributes.runtimeVisibleAnnotations())
        .map(RuntimeVisibleAnnotationsAttribute::annotations)
        .orElse(List.of());
      if (annotations.isEmpty()) {
        classBuilder.with(classElement);
        return;
      }

      // check for @KeepOnConditionFailure early, so that we can report if it's being used on a non-void method
      var keepOnFailure = annotations.stream()
        .anyMatch(annotation -> annotation.classSymbol().equals(CD_KEEP_ON_CONDITION_FAILURE));
      var returnsVoid = methodModel.methodTypeSymbol().returnType().equals(ConstantDescs.CD_void);
      if (keepOnFailure && !returnsVoid) {
        var errorMessage = String.format(
          "@KeepOnConditionFailure is only supported on methods returning void, but found on: %s.%s%s",
          classModel.thisClass().name().stringValue(),
          methodModel.methodName().stringValue(),
          methodModel.methodTypeSymbol().displayDescriptor());
        throw new UnsupportedOperationException(errorMessage);
      }

      // check if the method passes all condition checks
      var conditionContext = new DefaultConditionContext(
        classModel.thisClass().asSymbol(),
        methodModel.methodName().stringValue(),
        methodModel.methodTypeSymbol(),
        moduleMetadata,
        moduleClassLoader);
      var methodPassesConditions = this.checkMethodPassesConditions(conditionContext, annotations, conditionProcessors);
      if (methodPassesConditions) {
        classBuilder.with(classElement);
        return;
      }

      // drop the whole method if we shouldn't keep it on a condition failure
      if (!keepOnFailure) {
        return;
      }

      // drop the body of the method, but keep the method itself
      var methodTransform = MethodTransform.transformingCode(MethodeCodeDropTransform.INSTANCE);
      classBuilder.transformMethod(methodModel, methodTransform);
    });
  }

  /**
   * Checks if all the given condition processor match all the given annotation. A processor matches an annotation if it
   * either doesn't support it or the {@code matches()} method returns {@code true}.
   *
   * @param conditionContext    the condition process context for the current method.
   * @param methodAnnotations   the annotations on the method.
   * @param conditionProcessors the registered condition processors to check against.
   * @return true if all the processors match the given annotations, false otherwise.
   * @throws NullPointerException if one of the given arguments is null.
   */
  private boolean checkMethodPassesConditions(
    @NonNull ConditionContext conditionContext,
    @NonNull Collection<Annotation> methodAnnotations,
    @NonNull Collection<ServiceRegistryRegistration<ConditionProcessor>> conditionProcessors
  ) {
    for (var annotation : methodAnnotations) {
      for (var processorRegistration : conditionProcessors) {
        var conditionProcessor = processorRegistration.serviceInstance();
        var annotationClassDesc = conditionProcessor.annotation().describeConstable().orElse(null);
        if (annotationClassDesc == null || !annotation.classSymbol().equals(annotationClassDesc)) {
          // processor is not for the annotation, skip
          continue;
        }

        var passesCondition = conditionProcessor.matches(conditionContext, annotation);
        if (!passesCondition) {
          return false;
        }
      }
    }

    return true;
  }
}
