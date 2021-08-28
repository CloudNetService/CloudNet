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

import com.google.common.primitives.Primitives;
import com.google.common.reflect.TypeToken;
import de.dytanic.cloudnet.common.StringUtil;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.rpc.annotation.RPCFieldGetter;
import de.dytanic.cloudnet.driver.network.rpc.annotation.RPCIgnore;
import de.dytanic.cloudnet.driver.network.rpc.exception.ClassCreationException;
import de.dytanic.cloudnet.driver.network.rpc.exception.MissingFieldGetterException;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectMapper;
import de.dytanic.cloudnet.driver.util.DefiningClassLoader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.LoaderClassPath;
import org.jetbrains.annotations.NotNull;

public class DataClassInvokerGenerator {

  private static final String DATA_BUF_CLASS_NAME = DataBuf.class.getCanonicalName();
  private static final String DATA_BUF_MUTABLE_CLASS_NAME = DataBuf.Mutable.class.getCanonicalName();
  private static final String OBJECT_MAPPER_CLASS_NAME = ObjectMapper.class.getCanonicalName();

  private static final String INSTANCE_CREATOR_NAME_FORMAT = "%sInstanceCreator";
  private static final String INFORMATION_WRITE_NAME_FORMAT = "%sInformationWriter";

  private static final String INSTANCE_MAKER_OVERRIDDEN_FORMAT =
    "public Object makeInstance(" + DATA_BUF_CLASS_NAME + " b, " + OBJECT_MAPPER_CLASS_NAME
      + " m) { return new %s(%s); }";
  private static final String WRITE_INFO_OVERRIDDEN_FORMAT =
    "public void writeInformation(" + DATA_BUF_MUTABLE_CLASS_NAME + " b, Object obj," + OBJECT_MAPPER_CLASS_NAME
      + " m) { %s }";

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
      ctClass.addField(CtField.make("private final java.lang.reflect.Type[] types;", ctClass));
      // add a constructor to initialize the field
      ctClass.addConstructor(CtNewConstructor.make(String.format(
        "public %s(java.lang.reflect.Type[] types) { this.types = types; }",
        ctClass.getSimpleName()
      ), ctClass));
      // create the method body
      StringBuilder stringBuilder = new StringBuilder();
      for (int i = 0; i < types.length; i++) {
        // more than the raw type is not supported anyways
        String wrapper;
        Class<?> rawType = TypeToken.of(types[i]).getRawType();
        if (rawType.isPrimitive()) {
          // unwrap all primitives classes as they are causing verify errors by the runtime
          Class<?> wrapped = Primitives.wrap(rawType);
          // this hacky trick allows us to convert a wrapped type to a primitive type by calling the <primitive>Value
          // method. For example: ((Boolean) <decoding code (added later)>).booleanValue()
          wrapper = String.format("((%s) %s).%sValue()", wrapped.getTypeName(), "%s", rawType.getTypeName());
        } else {
          // we can just cast non-primitive types
          wrapper = String.format("(%s) %s", rawType.getTypeName(), "%s");
        }
        // generate the method
        stringBuilder.append(String.format(wrapper, String.format("m.readObject(b, this.types[%d])", i))).append(',');
      }
      // override the method
      ctClass.addMethod(CtMethod.make(String.format(
        INSTANCE_MAKER_OVERRIDDEN_FORMAT,
        clazz.getCanonicalName(),
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
        Class<?> processing = clazz;
        do {
          // only use the methods of the current class, not of the subclasses to prevent deep methods.
          for (Method method : processing.getDeclaredMethods()) {
            // we search for getter methods which
            //  - have no parameters
            //  - are not annotated with @RPCIgnore
            //  - is public
            //  - is not static
            if (method.getParameterCount() == 0
              && !method.isAnnotationPresent(RPCIgnore.class)
              && Modifier.isPublic(method.getModifiers())
              && !Modifier.isStatic(method.getModifiers())) {
              includedMethods.add(method);
            }
          }
        } while ((processing = processing.getSuperclass()) != Object.class);
        // associate each field to a method getter
        Map<Field, Method> fieldGetters = new HashMap<>();
        for (Field field : fields) {
          RPCFieldGetter overriddenGetter = field.getAnnotation(RPCFieldGetter.class);
          if (overriddenGetter != null) {
            // in this case the associated getter method is given, try to find it in the methods list
            fieldGetters.put(field, this.findGetterForField(
              clazz,
              includedMethods,
              field,
              m -> m.getName().equals(overriddenGetter.value())));
          } else {
            // here we just search for a method which ends with the field name, covering any case.
            // Example: field name: cpuUsage will find methods like:
            //  - getCpuUsage()
            //  - cpuUsage()
            //  - getFullCpuUsage()
            fieldGetters.put(field, this.findGetterForField(
              clazz,
              includedMethods,
              field,
              m -> StringUtil.endsWithIgnoreCase(m.getName(), field.getName())));
          }
        }
        // create the method body builder
        StringBuilder stringBuilder = new StringBuilder();
        for (Field field : fields) {
          // more than the raw type is not supported anyways
          String wrapper;
          Class<?> rawType = TypeToken.of(field.getGenericType()).getRawType();
          if (rawType.isPrimitive()) {
            // unwrap all primitives classes as they are causing verify errors by the runtime
            Class<?> wrapped = Primitives.wrap(rawType);
            // convert the primitive type when the method is called to the object wrapper of the type by using the
            // valueOf method. For example: Integer.valueOf(<decoding code (added later)>)
            wrapper = String.format("%s.valueOf(%s)", wrapped.getCanonicalName(), "%s");
          } else {
            // just write, no wrapping required for objects
            wrapper = "%s";
          }
          // use the associated getter to access the field value
          Method associatedMethod = fieldGetters.get(field);
          stringBuilder
            .append("m.writeObject(b, ")
            .append(String.format(
              wrapper,
              String.format("((%s) obj).%s()", clazz.getCanonicalName(), associatedMethod.getName())))
            .append(");");
        }
        // override the method
        ctClass.addMethod(CtMethod.make(
          String.format(WRITE_INFO_OVERRIDDEN_FORMAT, stringBuilder),
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
        "Unable to generate DataClassInformationWriter for class %s and fields %s",
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

  protected @NotNull Method findGetterForField(
    @NotNull Class<?> clazz,
    @NotNull Collection<Method> methods,
    @NotNull Field field,
    @NotNull Predicate<Method> extraFilter
  ) {
    Method choice = null;
    Predicate<Method> fullFilter = this.commonFilter(field).and(extraFilter);
    // search for the best choice
    for (Method method : methods) {
      if (fullFilter.test(method)) {
        if (choice == null) {
          // if there is no choice yet the method is the choice
          choice = method;
        } else if (choice.getName().length() > method.getName().length()) {
          // if the method name is shorter than the current choice we assume that the method is better to use
          choice = method;
        }
      }
    }
    // check if we found a getter method
    if (choice == null) {
      throw new MissingFieldGetterException(clazz, field);
    }
    // success!
    return choice;
  }

  protected @NotNull Predicate<Method> commonFilter(@NotNull Field field) {
    return method -> method.getReturnType().equals(field.getType());
  }

  public interface DataClassInstanceCreator {

    @NotNull Object makeInstance(@NotNull DataBuf buf, @NotNull ObjectMapper context);
  }

  public interface DataClassInformationWriter {

    void writeInformation(@NotNull DataBuf.Mutable target, @NotNull Object obj, @NotNull ObjectMapper context);
  }
}
