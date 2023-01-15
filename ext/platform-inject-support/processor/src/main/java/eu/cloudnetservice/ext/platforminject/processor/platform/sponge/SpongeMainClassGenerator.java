/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.ext.platforminject.processor.platform.sponge;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import eu.cloudnetservice.ext.platforminject.api.data.ParsedPluginData;
import eu.cloudnetservice.ext.platforminject.api.mapping.Container;
import eu.cloudnetservice.ext.platforminject.processor.classgen.BaseMainClassGenerator;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import lombok.NonNull;

final class SpongeMainClassGenerator extends BaseMainClassGenerator {

  private static final ClassName INJECT_ANNOTATION = ClassName.get("com.google.inject", "Inject");
  private static final ClassName LISTENER_ANNOTATION = ClassName.get("org.spongepowered.api.event", "Listener");
  private static final ClassName PLUGIN_ANNOTATION = ClassName.get("org.spongepowered.plugin.builtin.jvm", "Plugin");

  private static final ClassName SERVER_CLASS = ClassName.get("org.spongepowered.api", "Server");
  private static final ClassName PLUGIN_CONTAINER_CLASS = ClassName.get("org.spongepowered.plugin", "PluginContainer");

  private static final TypeName STARTED_ENGINE_EVENT = ParameterizedTypeName.get(
    ClassName.get("org.spongepowered.api.event.lifecycle", "StartedEngineEvent"),
    SERVER_CLASS);
  private static final TypeName STOPPING_ENGINE_EVENT = ParameterizedTypeName.get(
    ClassName.get("org.spongepowered.api.event.lifecycle", "StoppingEngineEvent"),
    SERVER_CLASS);

  public SpongeMainClassGenerator() {
    super("sponge");
  }

  @Override
  public void generatePluginMainClass(
    @NonNull ParsedPluginData pluginData,
    @NonNull TypeElement internalMainClass,
    @NonNull TypeSpec.Builder typeBuilder
  ) {
    // add the @Plugin annotation to the class
    var id = SpongePluginInfoGenerator.PLUGIN_ID_GENERATOR.convert(pluginData.name());
    var pluginAnnotation = AnnotationSpec.builder(PLUGIN_ANNOTATION).addMember("value", "$S", id).build();
    typeBuilder.addAnnotation(pluginAnnotation);

    // add a field for the platform info
    var platformInfoField = FieldSpec.builder(Container.class, "data", Modifier.PRIVATE, Modifier.FINAL).build();
    typeBuilder.addField(platformInfoField);

    // add the class constructor
    var constructor = this.appendConstructListenerConstruction(
      MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(INJECT_ANNOTATION)
        .addParameter(PLUGIN_CONTAINER_CLASS, "container")
        .addCode(CodeBlock.of("this.data = new $T(this, container);", Container.class)),
      CodeBlock.of("this.data"),
      pluginData.constructionListenerClass()
    ).build();
    typeBuilder.addMethod(constructor);

    // add the engine start event (to enable the plugin)
    var engineStartListener = MethodSpec.methodBuilder("handleEngineStart")
      .addModifiers(Modifier.PUBLIC)
      .addAnnotation(LISTENER_ANNOTATION)
      .addParameter(STARTED_ENGINE_EVENT, "event")
      .addCode(this.visitPluginLoad(
        internalMainClass,
        CodeBlock.of("this.data"),
        CodeBlock.of("this.getClass().getClassLoader()")))
      .build();
    typeBuilder.addMethod(engineStartListener);

    // add the stopping engine listener (to disable the plugin)
    var engineStoppingListener = MethodSpec.methodBuilder("handleEngineStopping")
      .addModifiers(Modifier.PUBLIC)
      .addAnnotation(LISTENER_ANNOTATION)
      .addParameter(STOPPING_ENGINE_EVENT, "event")
      .addCode(this.visitPluginDisableByData(CodeBlock.of("this.data")))
      .build();
    typeBuilder.addMethod(engineStoppingListener);
  }
}
