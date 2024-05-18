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

package eu.cloudnetservice.driver.ap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

@SupportedAnnotationTypes("eu.cloudnetservice.driver.network.rpc.annotation.RPCValidation")
public class RPCValidationProcessor extends AbstractProcessor {

  private volatile Elements eu;
  private volatile Map<Element, List<Map.Entry<String, Integer>>> elements;

  @Override
  public void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.eu = processingEnv.getElementUtils();
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    // get the annotation type element
    var annotationType = annotations.stream().findFirst().orElse(null);
    if (annotationType != null) {
      // get all types which are annotated as @RPCValidation
      for (Element element : roundEnv.getElementsAnnotatedWith(annotationType)) {
        // find the annotation
        var annotation = this.findAnnotation(element, annotationType);
        // read the values from the annotation
        var values = this.eu.getElementValuesWithDefaults(annotation).values()
          .iterator();
        // excludePattern (1), includeStaticMethods (2)
        var excludePattern = (String) values.next().getValue();
        var includeStaticMethods = (boolean) values.next().getValue();
        // read all elements from the class
        for (Element enclosedElement : element.getEnclosedElements()) {
          // skip all elements which are not a method
          if (enclosedElement.getKind() != ElementKind.METHOD
            || !(enclosedElement instanceof ExecutableElement method)) {
            continue;
          }
          // skip static method if requested
          if (!includeStaticMethods && element.getModifiers().contains(Modifier.STATIC)) {
            continue;
          }
          // check if the method name is excluded
          if (!excludePattern.isEmpty() && enclosedElement.getSimpleName().toString().matches(excludePattern)) {
            continue;
          }
          // add to the processed elements
          if (this.elements == null) {
            this.elements = new HashMap<>();
          }

          this.elements.computeIfAbsent(element, $ -> new ArrayList<>())
            .add(Map.entry(method.getSimpleName().toString(), method.getParameters().size()));
        }
      }
    }

    // check if the processing run is over
    if (roundEnv.processingOver() && this.elements != null) {
      // process the result
      for (var entry : this.elements.entrySet()) {
        // filter out all duplicates of a type
        var duplicates = entry.getValue().stream()
          .filter(e -> Collections.frequency(entry.getValue(), e) > 1)
          .collect(Collectors.toSet());
        // check if there are any duplicates
        if (!duplicates.isEmpty()) {
          throw new IllegalStateException(String.format(
            "Duplicate method names in %s: %s",
            entry.getKey().getSimpleName().toString(),
            duplicates.stream().map(Map.Entry::getKey).collect(Collectors.joining(", "))));
        }
      }
      // reset the values
      this.elements = null;
    }

    return false;
  }

  private AnnotationMirror findAnnotation(Element element, TypeElement type) {
    return element.getAnnotationMirrors().stream()
      .filter(mirror -> mirror.getAnnotationType().asElement().equals(type))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Element " + element + " has no annotation of type " + type));
  }
}
