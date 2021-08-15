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

package de.dytanic.cloudnet.driver.network.rpc.defaults.object.data;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import de.dytanic.cloudnet.common.StringUtil;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.rpc.annotation.RPCFieldGetter;
import de.dytanic.cloudnet.driver.network.rpc.annotation.RPCIgnore;
import de.dytanic.cloudnet.driver.network.rpc.exception.ClassCreationException;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectMapper;
import de.dytanic.cloudnet.driver.util.DefiningClassLoader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.LoaderClassPath;
import org.jetbrains.annotations.NotNull;

public class DataClassInvokerGenerator {

  private static final String DATA_BUF_CLASS_NAME = DataBuf.Mutable.class.getCanonicalName();
  private static final String OBJECT_MAPPER_CLASS_NAME = ObjectMapper.class.getCanonicalName();

  private static final String INSTANCE_CREATOR_NAME_FORMAT = "%sInstanceCreator";
  private static final String INFORMATION_WRITE_NAME_FORMAT = "%sInformationWriter";

  private static final String INSTANCE_MAKER_OVERRIDDEN_FORMAT =
    "public Object makeInstance(" + DATA_BUF_CLASS_NAME + " b, " + OBJECT_MAPPER_CLASS_NAME
      + " m) { return new %s(%s); }";
  private static final String WRITE_INFO_OVERRIDDEN_FORMAT =
    "public void writeInformation(" + DATA_BUF_CLASS_NAME + " b, Object obj," + OBJECT_MAPPER_CLASS_NAME + " m) { %s }";

  private final ClassPool classPool;
  private final Map<ClassLoader, DefiningClassLoader> classLoaders;

  public DataClassInvokerGenerator() {
    this.classPool = new ClassPool(ClassPool.getDefault());
    this.classLoaders = new ConcurrentHashMap<>();
  }

  public @NotNull DataClassInstanceCreator createInstanceCreator(@NotNull Class<?> clazz, @NotNull Type[] types) {
    try {
      // get the class loader which is responsible for the class we want to invoke
      DefiningClassLoader loader = this.getDefiningClassLoader(clazz.getClassLoader());
      // create the class
      CtClass ctClass = this.classPool.makeClass(String.format(INSTANCE_CREATOR_NAME_FORMAT, clazz.getCanonicalName()));
      ctClass.addInterface(this.classPool.getCtClass(DataClassInstanceCreator.class.getName()));
      // add the types as a field to the class
      ctClass.addField(CtField.make("private final Type[] types;", ctClass));
      // add a constructor to initialize the field
      ctClass.addConstructor(CtNewConstructor.make(String.format(
        "public %s(Type[] types) { this.types = types; }",
        ctClass.getSimpleName()
      ), ctClass));
      // create the method body
      StringBuilder stringBuilder = new StringBuilder();
      for (int i = 0; i < types.length; i++) {
        stringBuilder.append("m.readObject(b, this.types[").append(i).append("],");
      }
      // override the method
      ctClass.addMethod(CtMethod.make(String.format(
        INSTANCE_MAKER_OVERRIDDEN_FORMAT,
        clazz.getName(),
        stringBuilder.length() == 0 ? "" : stringBuilder.substring(0, stringBuilder.length() - 1)
      ), ctClass));
      // now we can define the class and make an instance of the created invoker
      return (DataClassInstanceCreator) loader.defineClass(ctClass.getName(), ctClass.toBytecode())
        .getDeclaredConstructor(Type[].class)
        .newInstance((Object) types);
    } catch (Exception exception) {
      throw new ClassCreationException(String.format(
        "Unable to generate DataClassInstanceCreator for class %s",
        clazz.getName()), exception);
    }
  }

  public @NotNull DataClassInformationWriter createWriter(@NotNull Class<?> clazz, @NotNull Collection<Field> fields) {
    try {
      // get the class loader which is responsible for the class we want to invoke
      DefiningClassLoader loader = this.getDefiningClassLoader(clazz.getClassLoader());
      // create the class
      CtClass ctClass = this.classPool.makeClass(
        String.format(INFORMATION_WRITE_NAME_FORMAT, clazz.getCanonicalName()));
      ctClass.addInterface(this.classPool.getCtClass(DataClassInformationWriter.class.getName()));
      if (fields.size() > 0) {
        // get the methods of the class
        Set<Method> includedMethods = new HashSet<>();
        for (Method method : clazz.getDeclaredMethods()) {
          if (!method.isAnnotationPresent(RPCIgnore.class)) {
            includedMethods.add(method);
          }
        }
        // associate each field to a method getter
        Map<Field, Method> fieldGetters = new HashMap<>();
        for (Field field : fields) {
          RPCFieldGetter overriddenGetter = field.getAnnotation(RPCFieldGetter.class);
          if (overriddenGetter != null) {
            fieldGetters.put(field, Iterables.find(
              includedMethods,
              Predicates.and(this.commonFilter(field), method -> method.getName().equals(overriddenGetter.value()))));
          } else {
            fieldGetters.put(field, Iterables.find(
              includedMethods,
              Predicates.and(this.commonFilter(field),
                m -> StringUtil.endsWithIgnoreCase(m.getName(), field.getName()))));
          }
        }
        // create the method body builder
        StringBuilder stringBuilder = new StringBuilder();
        for (Field field : fields) {
          Method associatedMethod = fieldGetters.get(field);
          stringBuilder
            .append("m.writeObject(b, obj.")
            .append(associatedMethod.getName())
            .append(");");
        }
        // override the method
        ctClass.addMethod(CtMethod.make(
          String.format(WRITE_INFO_OVERRIDDEN_FORMAT, clazz.getName(), stringBuilder),
          ctClass));
      } else {
        // override the method
        ctClass.addMethod(CtMethod.make(String.format(WRITE_INFO_OVERRIDDEN_FORMAT, ""), ctClass));
      }

      // now we can define the class and make an instance of the created writer
      return (DataClassInformationWriter) loader.defineClass(ctClass.getName(), ctClass.toBytecode())
        .getDeclaredConstructor()
        .newInstance();
    } catch (Exception exception) {
      throw new ClassCreationException(String.format(
        "Unable to generate DataClassInformationWriter for class %s and fields",
        clazz.getName(), fields.stream().map(Field::getName).collect(Collectors.joining(", "))), exception);
    }
  }

  protected @NotNull DefiningClassLoader getDefiningClassLoader(@NotNull ClassLoader parent) {
    return this.classLoaders.computeIfAbsent(
      parent,
      classLoader -> {
        // append the loader to the class pool after creation
        DefiningClassLoader definingClassLoader = new DefiningClassLoader(classLoader);
        this.classPool.appendClassPath(new LoaderClassPath(definingClassLoader));
        return definingClassLoader;
      });
  }

  protected @NotNull Predicate<Method> commonFilter(@NotNull Field field) {
    return method -> method.getParameterCount() == 0 && method.getReturnType().equals(field.getType());
  }

  public interface DataClassInstanceCreator {

    @NotNull Object makeInstance(@NotNull DataBuf buf, @NotNull ObjectMapper context);
  }

  public interface DataClassInformationWriter {

    void writeInformation(@NotNull DataBuf.Mutable target, @NotNull Object obj, @NotNull ObjectMapper context);
  }
}
