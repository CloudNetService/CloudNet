/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.driver.network.rpc.defaults.handler.invoker;

import de.dytanic.cloudnet.driver.network.rpc.defaults.handler.MethodInformation;
import de.dytanic.cloudnet.driver.network.rpc.exception.ClassCreationException;
import de.dytanic.cloudnet.driver.util.DefiningClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.LoaderClassPath;
import org.jetbrains.annotations.NotNull;

public class MethodInvokerGenerator {

  private static final String CLASS_NAME_FORMAT = "%s.GeneratedInvoker%s_%s";
  private static final String INSTANCE_FIELD_FORMAT = "private final %s instance;";
  private static final String CONSTRUCTOR_FORMAT = "public %s(%s instance) { this.instance = instance; }";
  private static final String OVERRIDDEN_METHOD_FORMAT = "public Object callMethod(Object... args) { return this.instance.%s(%s); }";
  private static final String METHOD_INVOKER_CLASS_NAME = MethodInvoker.class.getName();

  private final ClassPool classPool;
  private final Map<ClassLoader, DefiningClassLoader> classLoaders;

  public MethodInvokerGenerator() {
    this.classPool = new ClassPool(ClassPool.getDefault());
    this.classLoaders = new ConcurrentHashMap<>();
  }

  public @NotNull MethodInvoker makeInvoker(@NotNull MethodInformation methodInfo) {
    try {
      // get the class loader which is responsible for the class we want to invoke
      DefiningClassLoader loader = this.classLoaders.computeIfAbsent(
        methodInfo.getDefiningClass().getClassLoader(),
        classLoader -> {
          // append the loader to the class pool after creation
          DefiningClassLoader definingClassLoader = new DefiningClassLoader(classLoader);
          this.classPool.appendClassPath(new LoaderClassPath(definingClassLoader));
          return definingClassLoader;
        });
      // now make the class
      CtClass ctClass = this.classPool.makeClass(String.format(
        CLASS_NAME_FORMAT,
        methodInfo.getDefiningClass().getPackage().getName(),
        methodInfo.getDefiningClass().getSimpleName(),
        methodInfo.getName()));
      // append the super interface
      ctClass.addInterface(this.classPool.get(METHOD_INVOKER_CLASS_NAME));
      // append the field to the class
      ctClass.addField(CtField.make(
        String.format(INSTANCE_FIELD_FORMAT, methodInfo.getSourceInstance().getClass().getCanonicalName()),
        ctClass));
      // add a constructor which initialized the field
      ctClass.addConstructor(CtNewConstructor.make(String.format(
        CONSTRUCTOR_FORMAT,
        ctClass.getName(),
        methodInfo.getSourceInstance().getClass().getCanonicalName()), ctClass));
      // override the invoke method
      if (methodInfo.getArguments().length > 0) {
        // the real magic happens here
        StringBuilder arguments = new StringBuilder();
        for (int i = 0; i < methodInfo.getArguments().length; i++) {
          arguments
            // cast the expected argument
            .append('(').append(methodInfo.getArguments()[i].getTypeName()).append(')')
            // read from the provided argument array
            .append("args[").append(i).append("],");
        }
        // add the method
        ctClass.addMethod(CtMethod.make(String.format(
          OVERRIDDEN_METHOD_FORMAT,
          methodInfo.getName(),
          arguments.substring(0, arguments.length() - 1)), ctClass));
      } else {
        // just invoke the method without arguments as we don't one
        ctClass.addMethod(CtMethod.make(String.format(OVERRIDDEN_METHOD_FORMAT, methodInfo.getName(), ""), ctClass));
      }

      // now we can define the class and make an instance of the created invoker
      return (MethodInvoker) loader.defineClass(ctClass.getName(), ctClass.toBytecode())
        .getDeclaredConstructor(methodInfo.getSourceInstance().getClass())
        .newInstance(methodInfo.getSourceInstance());
    } catch (Exception exception) {
      throw new ClassCreationException(String.format(
        "Cannot generate rpc handler for method %s defined in class %s",
        methodInfo.getName(),
        methodInfo.getDefiningClass().getCanonicalName()
      ), exception);
    }
  }
}
