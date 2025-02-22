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

package eu.cloudnetservice.driver.ap.registry;

import eu.cloudnetservice.driver.ap.util.ProcessingUtil;
import eu.cloudnetservice.driver.registry.AutoService;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.tools.StandardLocation;
import lombok.NonNull;

public final class AutoServiceProcessor extends AbstractProcessor {

  private static final String OUT_DIR_NAME_FORMAT = "autoservices/%s";

  private Filer filer;
  private Messager messager;
  private Elements elementUtil;
  private Set<AutoServiceMapping> serviceMappings;

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void init(@NonNull ProcessingEnvironment processingEnv) {
    super.init(processingEnv);

    this.filer = processingEnv.getFiler();
    this.messager = processingEnv.getMessager();
    this.elementUtil = processingEnv.getElementUtils();
    this.serviceMappings = new HashSet<>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean process(@NonNull Set<? extends TypeElement> annotations, @NonNull RoundEnvironment roundEnv) {
    if (roundEnv.processingOver()) {
      // emit all service mappings into a file if any were found
      if (!this.serviceMappings.isEmpty()) {
        try {
          var outputFileName = String.format(OUT_DIR_NAME_FORMAT, UUID.randomUUID());
          var outputFile = this.filer.createResource(StandardLocation.CLASS_OUTPUT, "", outputFileName);
          try (var outputFileStream = outputFile.openOutputStream(); var out = new DataOutputStream(outputFileStream)) {
            for (var serviceMapping : this.serviceMappings) {
              serviceMapping.serialize(out);
            }
          }
        } catch (IOException exception) {
          this.messager.printError("Unable to create auto services output file: " + exception.getMessage());
        }
      }

      return false;
    }

    // process all elements that are annotated with @AutoService in the current round environment
    var annotatedElements = roundEnv.getElementsAnnotatedWith(AutoService.class);
    for (var element : annotatedElements) {
      // ensure that the annotated element is concrete
      var typeElement = (TypeElement) element;
      if (typeElement.getModifiers().contains(Modifier.ABSTRACT) || !typeElement.getKind().isClass()) {
        this.messager.printError("type annotated with @AutoService must be concrete", element);
        continue;
      }

      // get the annotation & validate the provided service name
      var annotation = element.getAnnotation(AutoService.class);
      var serviceName = annotation.name();
      if (serviceName.isBlank()) {
        this.messager.printError("service name must not be blank", element);
        continue;
      }

      // register a service mapping for each provided type in the annotation
      var annotatedBinaryName = this.elementUtil.getBinaryName(typeElement);
      @SuppressWarnings("ResultOfMethodCallIgnored") // we ignore annotation::services which is expected to happen
      var providedServiceTypes = ProcessingUtil.getTypesFromAnnotationProperty(annotation::services);
      for (var providedServiceType : providedServiceTypes) {
        if (providedServiceType instanceof DeclaredType declaredType) {
          // actual type (class or interface), validate that it is an interface
          var providedElement = declaredType.asElement();
          if (providedElement.getKind() != ElementKind.INTERFACE) {
            this.messager.printError("provided service implementation must be an interface", element);
            continue;
          }

          var providedBinaryName = this.elementUtil.getBinaryName((TypeElement) providedElement);
          var serviceMapping = new AutoServiceMapping(
            providedBinaryName,
            annotatedBinaryName,
            serviceName,
            annotation.singleton(),
            annotation.markAsDefault());
          if (!this.serviceMappings.add(serviceMapping)) {
            this.messager.printError("detected duplicate service registration for " + serviceMapping, element);
          }
        } else {
          // provided service type is not a class/interface (for example a primitive)
          this.messager.printError("provided service implementation must be an interface", element);
        }
      }
    }

    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Set<String> getSupportedAnnotationTypes() {
    return Set.of(AutoService.class.getCanonicalName());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Set<String> getSupportedOptions() {
    return Set.of();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }
}
