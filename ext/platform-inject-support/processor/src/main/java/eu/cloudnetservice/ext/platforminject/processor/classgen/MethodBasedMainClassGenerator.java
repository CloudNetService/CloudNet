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

package eu.cloudnetservice.ext.platforminject.processor.classgen;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import eu.cloudnetservice.ext.platforminject.api.data.ParsedPluginData;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

public abstract class MethodBasedMainClassGenerator extends BaseMainClassGenerator {

  protected final String loadMethodName;
  protected final String disableMethodName;

  protected MethodBasedMainClassGenerator(
    @NonNull String platformName,
    @NonNull String loadMethodName,
    @NonNull String disableMethodName
  ) {
    super(platformName);
    this.loadMethodName = loadMethodName;
    this.disableMethodName = disableMethodName;
  }

  @Override
  public void generatePluginMainClass(
    @NonNull ParsedPluginData pluginData,
    @NonNull TypeElement internalMainClass,
    @NonNull TypeSpec.Builder typeBuilder
  ) {
    // build the accessor to get the platform data from the class
    var platformAccessBlock = this.providePlatformAccess(internalMainClass, pluginData);
    var classLoaderAccessBlock = this.provideClassLoaderAccess(internalMainClass, pluginData);

    // build the constructor and add the construction listener call
    var constructor = this.appendConstructListenerConstruction(
      MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC),
      platformAccessBlock,
      pluginData.constructionListenerClass()
    ).build();
    typeBuilder.addMethod(constructor);

    // build the load method
    var loadCode = this.visitPluginLoad(internalMainClass, platformAccessBlock, classLoaderAccessBlock);
    var loadMethod = this.beginMethod(this.loadMethodName).addAnnotation(Override.class).addCode(loadCode).build();

    // build the disable method
    var disableCode = this.visitPluginDisableByData(platformAccessBlock);
    var disableMethod = this.beginMethod(this.disableMethodName)
      .addAnnotation(Override.class)
      .addCode(disableCode)
      .build();

    // add the methods to the type & apply other customizations which are required for the platform
    typeBuilder.addMethod(loadMethod).addMethod(disableMethod);
    this.customizeType(pluginData, typeBuilder);
  }

  @ApiStatus.OverrideOnly
  protected void customizeType(@NonNull ParsedPluginData pluginData, @NonNull TypeSpec.Builder typeBuilder) {
  }

  protected abstract @NonNull CodeBlock providePlatformAccess(
    @NonNull TypeElement mainClass,
    @NonNull ParsedPluginData pluginData);

  protected abstract @NonNull CodeBlock provideClassLoaderAccess(
    @NonNull TypeElement mainClass,
    @NonNull ParsedPluginData pluginData);
}
