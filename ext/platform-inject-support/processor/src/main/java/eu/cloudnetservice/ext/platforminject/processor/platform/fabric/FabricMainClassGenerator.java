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

package eu.cloudnetservice.ext.platforminject.processor.platform.fabric;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import eu.cloudnetservice.ext.platforminject.api.data.ParsedPluginData;
import eu.cloudnetservice.ext.platforminject.processor.classgen.BaseMainClassGenerator;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import lombok.NonNull;

final class FabricMainClassGenerator extends BaseMainClassGenerator {

  private static final ClassName ENV_TYPE_CLASS = ClassName.get("net.fabricmc.api", "EnvType");
  private static final ClassName ENVIRONMENT_ANNOTATION_CLASS = ClassName.get("net.fabricmc.api", "Environment");

  private static final ClassName SERVER_MOD_INIT_CLASS = ClassName.get(
    "net.fabricmc.api",
    "DedicatedServerModInitializer");

  public FabricMainClassGenerator() {
    super("fabric");
  }

  @Override
  protected void generatePluginMainClass(
    @NonNull ParsedPluginData pluginData,
    @NonNull TypeElement internalMainClass,
    @NonNull TypeSpec.Builder typeBuilder
  ) {
    // generate our shutdown hook class
    var shutdownHookClass = this.generateShutdownHookClass();
    typeBuilder.addType(shutdownHookClass);

    // implement the mod initializer class
    typeBuilder.addSuperinterface(SERVER_MOD_INIT_CLASS);

    // add the environment annotation to the class
    var environmentAnno = AnnotationSpec.builder(ENVIRONMENT_ANNOTATION_CLASS)
      .addMember("value", "$T.SERVER", ENV_TYPE_CLASS)
      .build();
    typeBuilder.addAnnotation(environmentAnno);

    // build the constructor and add the construction listener call
    var constructor = this.appendConstructListenerConstruction(
      MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC),
      CodeBlock.of("this"),
      pluginData.constructionListenerClass()
    ).build();
    typeBuilder.addMethod(constructor);

    // override the onInitializeServer method & add it to the type
    var loadCode = CodeBlock.builder()
      .add("Runtime.getRuntime().addShutdownHook(new Thread(new $N(this)));", shutdownHookClass)
      .add(this.visitPluginLoad(
        internalMainClass,
        CodeBlock.of("this"),
        CodeBlock.of("this.getClass().getClassLoader()")))
      .build();
    var loadMethod = this.beginMethod("onInitializeServer").addAnnotation(Override.class).addCode(loadCode).build();
    typeBuilder.addMethod(loadMethod);
  }

  private @NonNull TypeSpec generateShutdownHookClass() {
    // begin the class build
    var shutdownHookClass = TypeSpec.classBuilder("ShutdownHook")
      .addSuperinterface(Runnable.class)
      .addModifiers(Modifier.PRIVATE, Modifier.STATIC);

    // add the platform data field
    var dataField = FieldSpec.builder(ClassName.OBJECT, "data", Modifier.PRIVATE, Modifier.FINAL).build();
    shutdownHookClass.addField(dataField);

    // add the constructor
    var constructor = MethodSpec.constructorBuilder()
      .addModifiers(Modifier.PUBLIC)
      .addParameter(ClassName.OBJECT, "platformData")
      .addCode(CodeBlock.of("this.data = platformData;"))
      .build();
    shutdownHookClass.addMethod(constructor);

    // override the run method
    var runMethod = this.beginMethod("run")
      .addAnnotation(Override.class)
      .addCode(this.visitPluginDisableByData(CodeBlock.of("this.data")))
      .build();
    shutdownHookClass.addMethod(runMethod);

    // finish construction
    return shutdownHookClass.build();
  }
}
