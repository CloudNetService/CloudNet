/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.ext.platforminject.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import eu.cloudnetservice.ext.platforminject.provider.PlatformInfoManager;
import eu.cloudnetservice.ext.platforminject.stereotype.ConstructionListener;
import eu.cloudnetservice.ext.platforminject.stereotype.PlatformPlugin;
import eu.cloudnetservice.ext.platforminject.util.PluginUtil;
import java.io.IOException;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.tools.Diagnostic;
import lombok.NonNull;

public final class PlatformPluginProcessor extends AbstractProcessor {

  private ProcessingEnvironment environment;

  @Override
  public synchronized void init(@NonNull ProcessingEnvironment processingEnv) {
    this.environment = processingEnv;
  }

  @Override
  public @NonNull SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public @NonNull Set<String> getSupportedAnnotationTypes() {
    return Set.of(
      "eu.cloudnetservice.ext.platforminject.stereotype.PlatformPlugin",
      "eu.cloudnetservice.ext.platforminject.stereotype.ConstructionListener");
  }

  @Override
  public boolean process(@NonNull Set<? extends TypeElement> annotations, @NonNull RoundEnvironment roundEnv) {
    // skip processing if processing is over
    if (roundEnv.processingOver()) {
      return false;
    }

    // find all platform plugin classes
    for (var element : roundEnv.getElementsAnnotatedWith(PlatformPlugin.class)) {
      // ensure that only classes are annotated
      if (element.getKind() != ElementKind.CLASS) {
        this.environment.getMessager().printMessage(
          Diagnostic.Kind.ERROR,
          "Only classes can be annotated with @PlatformPlugin");
        continue;
      }

      // extract the plugin info & get the corresponding platform info
      var platformAnno = element.getAnnotation(PlatformPlugin.class);
      var platformInfo = PlatformInfoManager.manager().provider(platformAnno.platform());

      // check if the platform is registered
      if (platformInfo == null) {
        this.environment.getMessager().printMessage(
          Diagnostic.Kind.ERROR,
          "Invalid platform provided to @PlatformPlugin: " + platformAnno.platform());
        continue;
      }

      // check if the type has a construct listener
      var constructionListener = element.getAnnotation(ConstructionListener.class);
      var listenerClass = constructionListener == null ? null : this.getListenerClass(constructionListener);

      // construct information about the plugin & the new main class name
      var pluginData = platformInfo.dataParser().parseAndValidateData(platformAnno, listenerClass);
      var platformMainClass = PluginUtil.buildMainClassName(pluginData.name(), platformAnno.platform());

      try {
        // generate the plugin info file first
        platformInfo.infoGenerator().generatePluginInfo(pluginData, platformMainClass, this.environment.getFiler());

        // generate the main class
        var type = (TypeElement) element;
        var generated = platformInfo.mainClassGenerator().generatePluginMainClass(pluginData, type, platformMainClass);

        // convert the generated class for writing
        var packageName = this.environment.getElementUtils().getPackageOf(element).toString();
        var javaFile = JavaFile.builder(packageName, generated).build();

        // write the file to the environment
        javaFile.writeTo(this.environment.getFiler());
      } catch (IOException exception) {
        this.environment.getMessager().printMessage(
          Diagnostic.Kind.ERROR,
          "Exception generating plugin info or main class: " + exception.getMessage(),
          element);
      }
    }

    // don't claim annotations
    return false;
  }

  private @NonNull String getListenerClass(@NonNull ConstructionListener listener) {
    ClassName listenerClassName;
    try {
      // try to get the class name by invoking the value method directly
      var listenerClass = listener.value();
      listenerClassName = ClassName.get(listenerClass);
    } catch (MirroredTypeException exception) {
      // try to get the class name from the type mirror
      var candidateName = TypeName.get(exception.getTypeMirror());
      if (!(candidateName instanceof ClassName)) {
        throw new IllegalStateException("@ConstructionListener uses non-class, an array or primitive as value");
      }

      listenerClassName = (ClassName) candidateName;
    }

    // some basic validation
    var pkg = listenerClassName.packageName();
    if (pkg.startsWith("java.") || pkg.startsWith("javax.")) {
      throw new IllegalArgumentException("@ConstructionListener might not use a java type as value");
    }

    // convert to fully qualified name
    return listenerClassName.canonicalName();
  }
}
