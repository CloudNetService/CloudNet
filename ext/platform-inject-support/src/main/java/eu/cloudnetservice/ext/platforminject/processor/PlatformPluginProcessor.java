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
import eu.cloudnetservice.ext.platforminject.provider.PlatformInfoManager;
import eu.cloudnetservice.ext.platforminject.stereotype.ConstructionListener;
import eu.cloudnetservice.ext.platforminject.stereotype.PlatformPlugin;
import eu.cloudnetservice.ext.platforminject.stereotype.ProvidesFor;
import eu.cloudnetservice.ext.platforminject.util.PatternUtil;
import eu.cloudnetservice.ext.platforminject.util.PluginUtil;
import eu.cloudnetservice.ext.platforminject.util.TypeUtil;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import lombok.NonNull;

public final class PlatformPluginProcessor extends AbstractProcessor {

  private final List<ScanPackageEntry> scanEntries = new LinkedList<>();
  private final List<BindingClassGenerator.ParsedBindingData> bindingData = new LinkedList<>();

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
      "eu.cloudnetservice.ext.platforminject.stereotype.ProvidesFor",
      "eu.cloudnetservice.ext.platforminject.stereotype.PlatformPlugin",
      "eu.cloudnetservice.ext.platforminject.stereotype.ConstructionListener");
  }

  @Override
  public boolean process(@NonNull Set<? extends TypeElement> annotations, @NonNull RoundEnvironment roundEnv) {
    if (roundEnv.processingOver()) {
      // group the found bindings by plugin
      Map<SimplifiedPluginInfo, Collection<BindingClassGenerator.ParsedBindingData>> bindingsPerPlugin = new HashMap<>();
      for (var scanEntry : this.scanEntries) {
        // find all data entries which are matching the plugin entry
        var matchingBindings = this.bindingData.stream()
          .filter(data -> data.platform().equalsIgnoreCase(scanEntry.platform()))
          .filter(data -> scanEntry.packageNameMatchers().stream().anyMatch(pattern -> {
            var matcher = pattern.matcher(data.packageName());
            return matcher.matches();
          }))
          .toList();

        // register the entry if any binding is matching
        if (!matchingBindings.isEmpty()) {
          var info = new SimplifiedPluginInfo(scanEntry.plugin(), scanEntry.platform(), scanEntry.mainClassPackage());
          var knownBindings = bindingsPerPlugin.computeIfAbsent(info, $ -> new LinkedList<>());
          knownBindings.addAll(matchingBindings);
        }
      }

      // generate a class with all bindings for each plugin we know
      for (var entry : bindingsPerPlugin.entrySet()) {
        var info = entry.getKey();
        var bindings = entry.getValue();

        // generate a class for the bindings
        var bindingClassName = PluginUtil.buildClassName(info.plugin(), info.platform(), "Bindings");
        var bindingClass = BindingClassGenerator.buildBindingClass(bindingClassName, bindings);

        try {
          // convert the generated class & write it
          var javaFile = JavaFile.builder(info.mainClassPackage(), bindingClass).build();
          javaFile.writeTo(this.environment.getFiler());
        } catch (IOException exception) {
          throw new IllegalStateException("Unable to write binding class file", exception);
        }
      }

      // don't claim annotations
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
      var listenerClass = constructionListener == null
        ? null
        : TypeUtil.lookupClassName(constructionListener::value).canonicalName();

      // construct information about the plugin & the new main class name
      var pluginData = platformInfo.dataParser().parseAndValidateData(platformAnno, listenerClass);

      // generate the plugin main name
      var packageName = this.environment.getElementUtils().getPackageOf(element).toString();
      var platformMainClass = PluginUtil.buildClassName(pluginData.name(), platformAnno.platform(), "Entrypoint");

      // register provide scan entries
      var providesScanPackages = platformAnno.providesScan();
      if (providesScanPackages.length == 0) {
        // only register the package of the plugin main class
        var packagePattern = PatternUtil.literalPatternWithWildcardEnding(packageName);
        this.scanEntries.add(new ScanPackageEntry(
          pluginData.name(),
          platformAnno.platform(),
          packageName,
          Set.of(packagePattern)));
      } else {
        // register the provided package names
        var packagePatterns = PatternUtil.parsePattern(providesScanPackages);
        this.scanEntries.add(new ScanPackageEntry(
          pluginData.name(),
          platformAnno.platform(),
          packageName,
          packagePatterns));
      }

      try {
        // generate the plugin info file first
        var fullMainName = String.format("%s.%s", packageName, platformMainClass);
        platformInfo.infoGenerator().generatePluginInfo(pluginData, fullMainName, this.environment.getFiler());

        // generate the main class
        var type = (TypeElement) element;
        var generated = platformInfo.mainClassGenerator().generatePluginMainClass(pluginData, type, platformMainClass);

        // convert the generated class & write it
        var javaFile = JavaFile.builder(packageName, generated).build();
        javaFile.writeTo(this.environment.getFiler());
      } catch (IOException exception) {
        throw new IllegalStateException("Exception generating plugin info or main class", exception);
      }
    }

    // find all @ProvidesFor annotated classes
    for (var element : roundEnv.getElementsAnnotatedWith(ProvidesFor.class)) {
      // ensure that only classes are annotated
      if (element.getKind() != ElementKind.CLASS) {
        this.environment.getMessager().printMessage(
          Diagnostic.Kind.ERROR,
          "Only classes can be annotated with @PlatformPlugin");
        continue;
      }

      // extract the info from the element
      var providedForAnnotation = element.getAnnotation(ProvidesFor.class);
      var boundElement = ClassName.get((TypeElement) element);
      var providedClassNames = TypeUtil.lookupClassNames(providedForAnnotation::types);

      // build the data from the given information
      var platform = providedForAnnotation.platform();
      var data = new BindingClassGenerator.ParsedBindingData(
        boundElement.packageName(),
        platform,
        boundElement,
        providedClassNames);

      // register the data
      this.bindingData.add(data);
    }

    // don't claim annotations
    return false;
  }

  private record ScanPackageEntry(
    @NonNull String plugin,
    @NonNull String platform,
    @NonNull String mainClassPackage,
    @NonNull Set<Pattern> packageNameMatchers
  ) {

  }


  private record SimplifiedPluginInfo(
    @NonNull String plugin,
    @NonNull String platform,
    @NonNull String mainClassPackage
  ) {

  }
}
