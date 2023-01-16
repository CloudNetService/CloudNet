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

package eu.cloudnetservice.ext.platforminject.processor.platform.velocity;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import eu.cloudnetservice.ext.platforminject.api.data.ParsedPluginData;
import eu.cloudnetservice.ext.platforminject.api.mapping.PlatformedContainer;
import eu.cloudnetservice.ext.platforminject.processor.classgen.BaseMainClassGenerator;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import lombok.NonNull;

final class VelocityMainClassGenerator extends BaseMainClassGenerator {

  private static final ClassName INJECT_ANNOTATION = ClassName.get("com.google.inject", "Inject");
  private static final ClassName SUBSCRIBE_ANNOTATION = ClassName.get("com.velocitypowered.api.event", "Subscribe");

  private static final ClassName PROXY_SERVER = ClassName.get("com.velocitypowered.api.proxy", "ProxyServer");
  private static final ClassName PLUGIN_CONTAINER = ClassName.get("com.velocitypowered.api.plugin", "PluginContainer");

  private static final ClassName PROXY_INIT_EVENT = ClassName.get(
    "com.velocitypowered.api.event.proxy",
    "ProxyInitializeEvent");
  private static final ClassName PROXY_SHUTDOWN_EVENT = ClassName.get(
    "com.velocitypowered.api.event.proxy",
    "ProxyShutdownEvent");

  public VelocityMainClassGenerator() {
    super("velocity");
  }

  @Override
  public void generatePluginMainClass(
    @NonNull ParsedPluginData pluginData,
    @NonNull TypeElement internalMainClass,
    @NonNull TypeSpec.Builder typeBuilder
  ) {
    // add a field for the platform info
    var platformInfoField = FieldSpec
      .builder(PlatformedContainer.class, "data", Modifier.PRIVATE, Modifier.FINAL)
      .build();
    typeBuilder.addField(platformInfoField);

    // add the class constructor
    var constructor = this.appendConstructListenerConstruction(
      MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(INJECT_ANNOTATION)
        .addParameter(PLUGIN_CONTAINER, "container")
        .addParameter(PROXY_SERVER, "proxy")
        .addCode(CodeBlock.of("this.data = new $T(this, container, proxy);", PlatformedContainer.class)),
      CodeBlock.of("this.data"),
      pluginData.constructionListenerClass()
    ).build();
    typeBuilder.addMethod(constructor);

    // add the proxy shutdown listener (to enable the plugin)
    var proxyInitListener = MethodSpec.methodBuilder("handleProxyInit")
      .addModifiers(Modifier.PUBLIC)
      .addAnnotation(SUBSCRIBE_ANNOTATION)
      .addParameter(PROXY_INIT_EVENT, "event")
      .addCode(this.visitPluginLoad(
        internalMainClass,
        CodeBlock.of("this.data"),
        CodeBlock.of("this.getClass().getClassLoader().getParent()")))
      .build();
    typeBuilder.addMethod(proxyInitListener);

    // add the stopping engine listener (to disable the plugin)
    var proxyShutdownListener = MethodSpec.methodBuilder("handleProxyShutdown")
      .addModifiers(Modifier.PUBLIC)
      .addAnnotation(SUBSCRIBE_ANNOTATION)
      .addParameter(PROXY_SHUTDOWN_EVENT, "event")
      .addCode(this.visitPluginDisableByData(CodeBlock.of("this.data")))
      .build();
    typeBuilder.addMethod(proxyShutdownListener);
  }
}
