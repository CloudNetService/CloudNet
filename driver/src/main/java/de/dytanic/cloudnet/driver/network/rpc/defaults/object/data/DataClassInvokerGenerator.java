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

import static de.dytanic.cloudnet.driver.util.asm.AsmUtils.primitiveToWrapper;
import static de.dytanic.cloudnet.driver.util.asm.AsmUtils.pushInt;
import static de.dytanic.cloudnet.driver.util.asm.AsmUtils.wrapperToPrimitive;
import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

import com.google.common.reflect.TypeToken;
import de.dytanic.cloudnet.common.StringUtil;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.rpc.annotation.RPCFieldGetter;
import de.dytanic.cloudnet.driver.network.rpc.annotation.RPCIgnore;
import de.dytanic.cloudnet.driver.network.rpc.exception.ClassCreationException;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectMapper;
import de.dytanic.cloudnet.driver.util.define.ClassDefiners;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class DataClassInvokerGenerator {

  private static final String SUPER = "java/lang/Object";

  // DataClassInstanceCreator related stuff
  private static final String TYPES_DESC = Type.getDescriptor(java.lang.reflect.Type[].class);
  private static final String[] INSTANCE_CREATOR = new String[]{Type.getInternalName(DataClassInstanceCreator.class)};
  private static final String MAKE_INSTANCE_DESCRIPTOR = Type.getMethodDescriptor(
    Type.getType(Object.class),
    Type.getType(DataBuf.class),
    Type.getType(ObjectMapper.class));
  // DataClassInformationWriter related stuff
  private static final String[] INFO_WRITER = new String[]{Type.getInternalName(DataClassInformationWriter.class)};
  private static final String WRITE_INFORMATION_DESCRIPTOR = Type.getMethodDescriptor(
    Type.VOID_TYPE,
    Type.getType(DataBuf.Mutable.class),
    Type.getType(Object.class),
    Type.getType(ObjectMapper.class));
  // ObjectMapper related stuff
  private static final String DATA_BUF_NAME = Type.getInternalName(ObjectMapper.class);
  private static final String READ_OBJECT_DESC = Type.getMethodDescriptor(
    Type.getType(Object.class),
    Type.getType(DataBuf.class),
    Type.getType(java.lang.reflect.Type.class));
  private static final String WRITE_OBJECT_DESC = Type.getMethodDescriptor(
    Type.getType(DataBuf.Mutable.class),
    Type.getType(DataBuf.Mutable.class),
    Type.getType(Object.class));
  // Related stuff to generated classes
  private static final String INSTANCE_CREATOR_NAME_FORMAT = "%s$InstanceCreator";
  private static final String INFORMATION_WRITE_NAME_FORMAT = "%s$InformationWriter";

  public @NotNull DataClassInstanceCreator createInstanceCreator(
    @NotNull Class<?> clazz,
    @NotNull java.lang.reflect.Type[] types
  ) {
    try {
      var className = String.format(INSTANCE_CREATOR_NAME_FORMAT, Type.getInternalName(clazz));
      // init the class writer for a public final class implementing the InstanceCreator
      var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
      cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL, className, null, SUPER, INSTANCE_CREATOR);
      // add the Type[] field which holds the information about the target constructor
      cw
        .visitField(ACC_PRIVATE | ACC_FINAL, "types", TYPES_DESC, null, null)
        .visitEnd();
      // generate a constructor and the method
      MethodVisitor mv;
      {
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(" + TYPES_DESC + ")V", null, null);
        mv.visitCode();
        // visit super
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, SUPER, "<init>", "()V", false);
        // write the field value
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, className, "types", TYPES_DESC);
        // finish
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
      }
      {
        mv = cw.visitMethod(ACC_PUBLIC | ACC_FINAL, "makeInstance", MAKE_INSTANCE_DESCRIPTOR, null, null);
        mv.visitCode();
        // visit the new call
        mv.visitTypeInsn(NEW, Type.getInternalName(clazz));
        mv.visitInsn(DUP);
        // visit all types - store each parameter type for later use
        var parameters = new Type[types.length];
        for (var i = 0; i < types.length; i++) {
          // extract the raw type of the given type
          var rawType = TypeToken.of(types[i]).getRawType();
          // load the mapper, the data buf and the current class to the stack
          mv.visitVarInsn(ALOAD, 2);
          mv.visitVarInsn(ALOAD, 1);
          mv.visitVarInsn(ALOAD, 0);
          // get the types field
          mv.visitFieldInsn(GETFIELD, className, "types", TYPES_DESC);
          // push the index of the array access of types to the stack
          pushInt(mv, i);
          mv.visitInsn(AALOAD);
          // invoke the read method of the ObjectMapper
          mv.visitMethodInsn(INVOKEINTERFACE, DATA_BUF_NAME, "readObject", READ_OBJECT_DESC, true);
          // check if the raw type is primitive
          if (rawType.isPrimitive()) {
            wrapperToPrimitive(mv, rawType);
          } else {
            // cast to the type
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(rawType));
          }
          // store the type of the parameter
          parameters[i] = Type.getType(rawType);
        }
        // invoke the init (constructor) method
        mv.visitMethodInsn(
          INVOKESPECIAL,
          Type.getInternalName(clazz),
          "<init>",
          Type.getMethodDescriptor(Type.VOID_TYPE, parameters),
          false);
        // return the constructed value
        mv.visitInsn(ARETURN);
        // finish the method
        mv.visitMaxs(0, 0);
        mv.visitEnd();
      }
      // finish the class
      cw.visitEnd();
      // define & select the correct constructor for the class
      var constructor = ClassDefiners.current()
        .defineClass(className, clazz, cw.toByteArray())
        .getDeclaredConstructor(java.lang.reflect.Type[].class);
      constructor.setAccessible(true);
      // instantiate the new class
      return (DataClassInstanceCreator) constructor.newInstance((Object) types);
    } catch (Exception exception) {
      throw new ClassCreationException(String.format(
        "Unable to generate DataClassInstanceCreator for class %s",
        clazz.getName()), exception);
    }
  }

  public @NotNull DataClassInformationWriter createWriter(@NotNull Class<?> clazz, @NotNull Collection<Field> fields) {
    try {
      var className = String.format(INFORMATION_WRITE_NAME_FORMAT, Type.getInternalName(clazz));
      // init the class writer for a public final class implementing the InstanceCreator
      var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
      cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL, className, null, SUPER, INFO_WRITER);
      // visit an empty constructor
      MethodVisitor mv;
      {
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, SUPER, "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
      }
      // begin the method visit of the writeInformation method
      mv = cw.visitMethod(ACC_PUBLIC, "writeInformation", WRITE_INFORMATION_DESCRIPTOR, null, null);
      mv.visitCode();
      // check if there are fields we need to include - skip that step if not
      if (fields.size() > 0) {
        // get the methods of the class
        Set<Method> includedMethods = new HashSet<>();
        var processing = clazz;
        do {
          // only use the methods of the current class, not of the subclasses to prevent deep methods.
          for (var method : processing.getDeclaredMethods()) {
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
        for (var field : fields) {
          var overriddenGetter = field.getAnnotation(RPCFieldGetter.class);
          if (overriddenGetter != null) {
            // in this case the associated getter method is given, try to find it in the methods list
            fieldGetters.put(field, this.findGetterForField(
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
              includedMethods,
              field,
              m -> StringUtil.endsWithIgnoreCase(m.getName(), field.getName())));
          }
        }
        // create the method body
        for (var field : fields) {
          // initial work for the method instantiation
          // load the arguments of the method to the stack
          mv.visitVarInsn(ALOAD, 3);
          mv.visitVarInsn(ALOAD, 1);
          mv.visitVarInsn(ALOAD, 2);
          // get the raw type of the field
          Class<?> rawType;
          // get the associated getter method of the field
          var getter = fieldGetters.get(field);
          if (getter != null) {
            // extract all needed information from the method
            rawType = TypeToken.of(getter.getGenericReturnType()).getRawType();
            var declaring = Type.getInternalName(getter.getDeclaringClass());
            // cast the object argument to the declaring class of the method
            mv.visitTypeInsn(CHECKCAST, declaring);
            // get the value of the method
            mv.visitMethodInsn(
              INVOKEVIRTUAL,
              declaring,
              getter.getName(),
              Type.getMethodDescriptor(getter),
              getter.getDeclaringClass().isInterface());
          } else {
            // extract all needed information from the field
            rawType = TypeToken.of(field.getGenericType()).getRawType();
            var declaring = Type.getInternalName(field.getDeclaringClass());
            // cast the object argument to the declaring class of the field
            mv.visitTypeInsn(CHECKCAST, declaring);
            // get the value of the field
            mv.visitFieldInsn(GETFIELD, declaring, field.getName(), Type.getDescriptor(field.getType()));
          }
          // check if the type of the method or field is primitive
          if (rawType.isPrimitive()) {
            primitiveToWrapper(mv, rawType);
          }
          // invoke the write method in the object mapper
          mv.visitMethodInsn(INVOKEINTERFACE, DATA_BUF_NAME, "writeObject", WRITE_OBJECT_DESC, true);
        }
      }
      // finish the method generation
      mv.visitInsn(RETURN);
      mv.visitMaxs(0, 0);
      mv.visitEnd();
      // finish the class
      cw.visitEnd();

      // declare the class and get the constructor
      var constructor = ClassDefiners.current()
        .defineClass(className, clazz, cw.toByteArray())
        .getDeclaredConstructor();
      constructor.setAccessible(true);
      // create a new instance of the class
      return (DataClassInformationWriter) constructor.newInstance();
    } catch (Exception exception) {
      throw new ClassCreationException(String.format(
        "Unable to generate DataClassInformationWriter for class %s and fields %s",
        clazz.getName(), fields.stream().map(Field::getName).collect(Collectors.joining(", "))), exception);
    }
  }

  protected @Nullable Method findGetterForField(
    @NotNull Collection<Method> methods,
    @NotNull Field field,
    @NotNull Predicate<Method> extraFilter
  ) {
    Method choice = null;
    var fullFilter = this.commonFilter(field).and(extraFilter);
    // search for the best choice
    for (var method : methods) {
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
