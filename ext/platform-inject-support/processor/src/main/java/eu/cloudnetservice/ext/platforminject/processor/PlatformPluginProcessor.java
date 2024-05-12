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

package eu.cloudnetservice.ext.platforminject.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import eu.cloudnetservice.ext.platforminject.api.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.api.spi.PlatformDataGeneratorProvider;
import eu.cloudnetservice.ext.platforminject.api.stereotype.ConstructionListener;
import eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin;
import eu.cloudnetservice.ext.platforminject.api.stereotype.ProvidesFor;
import eu.cloudnetservice.ext.platforminject.api.util.PluginUtil;
import eu.cloudnetservice.ext.platforminject.processor.util.PatternUtil;
import eu.cloudnetservice.ext.platforminject.processor.util.TypeUtil;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import lombok.NonNull;

public final class PlatformPluginProcessor extends AbstractProcessor {

  private static final String PLATFORM_ENTRYPOINT_CLASS_NAME = PlatformEntrypoint.class.getName();

  private final Map<String, PlatformDataGeneratorProvider> dataGeneratorProviders = new HashMap<>();

  private ProcessingEnvironment environment;

  @Override
  public synchronized void init(@NonNull ProcessingEnvironment processingEnv) {
    this.environment = processingEnv;

    // load the data providers
    var serviceLoader = ServiceLoader.load(PlatformDataGeneratorProvider.class, this.getClass().getClassLoader());
    serviceLoader.forEach(provider -> this.dataGeneratorProviders.put(provider.name(), provider));
  }

  @Override
  public @NonNull SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public @NonNull Set<String> getSupportedAnnotationTypes() {
    return Set.of(
      "eu.cloudnetservice.ext.platforminject.api.stereotype.ProvidesFor",
      "eu.cloudnetservice.ext.platforminject.api.stereotype.PlatformPlugin",
      "eu.cloudnetservice.ext.platforminject.api.stereotype.ConstructionListener");
  }

  @Override
  public boolean process(@NonNull Set<? extends TypeElement> annotations, @NonNull RoundEnvironment roundEnv) {
    // get a type mirror for the platform entrypoint class here, once
    // as this depends on the element utils which are only available via
    // the environment we cannot initialize that field statically
    var platformEntryPointClass = this.environment
      .getElementUtils()
      .getTypeElement(PLATFORM_ENTRYPOINT_CLASS_NAME)
      .asType();

    // find all platform plugin classes
    List<ScanPackageEntry> scanEntries = new LinkedList<>();
    for (var element : roundEnv.getElementsAnnotatedWith(PlatformPlugin.class)) {
      // ensure that only classes are annotated
      if (element.getKind() != ElementKind.CLASS) {
        this.environment.getMessager().printMessage(
          Diagnostic.Kind.ERROR,
          "Only classes can be annotated with @PlatformPlugin");
        continue;
      }

      // ensure that the class implements PlatformEntrypoint
      var typeElement = (TypeElement) element;
      if (!this.environment.getTypeUtils().isAssignable(typeElement.asType(), platformEntryPointClass)) {
        this.environment.getMessager().printMessage(
          Diagnostic.Kind.ERROR,
          "Classes annotated as @PlatformPlugin must implement PlatformEntrypoint");
        continue;
      }

      // extract the plugin info & get the corresponding platform info
      var platformAnno = element.getAnnotation(PlatformPlugin.class);
      var dataGenerator = this.dataGeneratorProviders.get(platformAnno.platform());

      // check if the platform is registered
      if (dataGenerator == null) {
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
      var pluginData = dataGenerator.dataParser().parseAndValidateData(platformAnno, listenerClass);

      // generate the plugin main name
      var packageName = this.environment.getElementUtils().getPackageOf(element).toString();
      var platformMainClass = PluginUtil.buildClassName(pluginData.name(), platformAnno.platform(), "Entrypoint");

      // register provide scan entries
      var providesScanPackages = platformAnno.providesScan();
      if (providesScanPackages.length == 0) {
        // only register the package of the plugin main class
        var packagePattern = PatternUtil.literalPatternWithWildcardEnding(packageName);
        scanEntries.add(new ScanPackageEntry(
          pluginData.name(),
          platformAnno.platform(),
          packageName,
          Set.of(packagePattern)));
      } else {
        // register the provided package names
        var packagePatterns = PatternUtil.parsePattern(providesScanPackages);
        scanEntries.add(new ScanPackageEntry(
          pluginData.name(),
          platformAnno.platform(),
          packageName,
          packagePatterns));
      }

      try {
        // generate the plugin info file first
        var fullMainName = String.format("%s.%s", packageName, platformMainClass);
        dataGenerator.infoGenerator().generatePluginInfo(pluginData, fullMainName, this.environment.getFiler());

        // generate the main class
        dataGenerator.mainClassGenerator().generatePluginMainClass(
          this.environment.getFiler(),
          packageName,
          pluginData,
          typeElement,
          platformMainClass);
      } catch (IOException exception) {
        throw new IllegalStateException("Exception generating plugin info or main class", exception);
      }
    }

    // find all @ProvidesFor annotated classes
    List<BindingClassGenerator.ParsedBindingData> bindingData = new LinkedList<>();
    for (var element : roundEnv.getElementsAnnotatedWith(ProvidesFor.class)) {
      // ensure that only classes are annotated
      if (element.getKind() != ElementKind.CLASS) {
        this.environment.getMessager().printMessage(
          Diagnostic.Kind.ERROR,
          "Only classes can be annotated with @PlatformPlugin");
        continue;
      }

      // we can safely cast to a type element now
      var typeElement = (TypeElement) element;

      // extract the info from the element
      var boundElement = ClassName.get(typeElement);
      var providedForAnnotation = element.getAnnotation(ProvidesFor.class);
      var providedClassNames = TypeUtil.lookupClassNames(providedForAnnotation::types);

      // filter out the interfaces & superclass which are directly given through the provided annotation
      // to allow better generic type detection
      // we do nothing if the annotated class is parameterized, as this is a hint that the extended classes
      // might take type parameters which are not available during compile time
      Set<TypeName> genericTypes = new LinkedHashSet<>();
      if (typeElement.getTypeParameters().isEmpty() && providedForAnnotation.bindGenericSupertypes()) {
        // check the direct super class
        var supertype = typeElement.getSuperclass();
        this.findAndRegisterGenericType(providedClassNames, genericTypes, supertype);

        // check all interfaces
        var interfaces = typeElement.getInterfaces();
        for (var implementedInterface : interfaces) {
          this.findAndRegisterGenericType(providedClassNames, genericTypes, implementedInterface);
        }
      }

      // build the data from the given information
      var platform = providedForAnnotation.platform();
      var data = new BindingClassGenerator.ParsedBindingData(
        providedForAnnotation.bindWildcardTypeOfParameterizedTypes(),
        boundElement.packageName(),
        platform,
        boundElement,
        providedClassNames,
        genericTypes);

      // register the data
      bindingData.add(data);
    }

    // generate the binding classes for the plugins
    this.generateBindingClass(scanEntries, bindingData);

    // don't claim annotations
    return false;
  }

  private void generateBindingClass(
    @NonNull Collection<ScanPackageEntry> scanEntries,
    @NonNull List<BindingClassGenerator.ParsedBindingData> bindingData
  ) {
    // group the found bindings by plugin
    Map<SimplifiedPluginInfo, Collection<BindingClassGenerator.ParsedBindingData>> bindingsPerPlugin = new HashMap<>();
    for (var scanEntry : scanEntries) {
      // find all data entries which are matching the plugin entry
      var matchingBindings = bindingData.stream()
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
  }

  private void findAndRegisterGenericType(
    @NonNull Collection<ClassName> providedClasses,
    @NonNull Collection<TypeName> target,
    @NonNull TypeMirror currentType
  ) {
    if (!(currentType instanceof NoType)) {
      // get the full type name and erase the type
      var genericType = TypeName.get(currentType);
      var erasedType = TypeUtil.erasure(genericType);

      // check if there is a direct bound present in the provided classes
      for (var providedClass : providedClasses) {
        if (providedClass.equals(erasedType)) {
          target.add(genericType);
          break;
        }
      }
    }
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
