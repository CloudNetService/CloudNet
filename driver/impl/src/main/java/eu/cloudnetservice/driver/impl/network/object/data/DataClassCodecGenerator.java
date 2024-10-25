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

package eu.cloudnetservice.driver.impl.network.object.data;

import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.object.ObjectMapper;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCFieldGetter;
import eu.cloudnetservice.utils.base.CodeGenerationUtil;
import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Internal generator for data class codec implementations in runtime.
 *
 * @since 4.0
 */
final class DataClassCodecGenerator {

  // field / method names in generated class
  private static final String FIELDS_FIELD_NAME = "fields";
  private static final String SUPER_CODEC_NAME = "nextCodec";
  private static final String SERIALIZE_METHOD_NAME = "serialize";
  private static final String DESERIALIZE_METHOD_NAME = "deserialize";

  // type descriptors from java that are needed
  private static final ClassDesc CD_TYPE = ClassDesc.of(Type.class.getName());
  private static final ClassDesc CD_FIELD = ClassDesc.of(Field.class.getName());
  private static final ClassDesc CD_FIELD_ARRAY = CD_FIELD.arrayType();
  private static final String FIELD_GET_TYPE_NAME = "getGenericType";
  private static final MethodTypeDesc MT_FIELD_GET_TYPE = MethodTypeDesc.of(CD_TYPE);

  // data codec descriptors
  private static final ClassDesc CD_DATA_BUF = ClassDesc.of(DataBuf.class.getName());
  private static final ClassDesc CD_DATA_BUF_MUT = ClassDesc.of(DataBuf.Mutable.class.getName());
  private static final ClassDesc CD_OBJECT_MAPPER = ClassDesc.of(ObjectMapper.class.getName());
  private static final ClassDesc CD_DATA_CLASS_CODEC = ClassDesc.of(DataClassCodec.class.getName());

  // constants for invoking methods in object mapper
  private static final String OBJECT_MAPPER_READ_NAME = "readObject";
  private static final String OBJECT_MAPPER_WRITE_NAME = "writeObject";
  private static final MethodTypeDesc MT_OBJECT_MAPPER_READ = MethodTypeDesc.of(
    ConstantDescs.CD_Object,
    CD_DATA_BUF,
    CD_TYPE);
  private static final MethodTypeDesc MT_OBJECT_MAPPER_WRITE = MethodTypeDesc.of(
    CD_DATA_BUF_MUT,
    CD_DATA_BUF_MUT,
    ConstantDescs.CD_Object);

  // method descriptors in generated class
  private static final MethodTypeDesc MT_CONSTRUCTOR = MethodTypeDesc.of(
    ConstantDescs.CD_void,
    CD_FIELD_ARRAY,
    CD_DATA_CLASS_CODEC);
  private static final MethodTypeDesc MT_DESERIALIZE = MethodTypeDesc.of(
    ConstantDescs.CD_Object,
    CD_DATA_BUF,
    CD_OBJECT_MAPPER);
  private static final MethodTypeDesc MT_SERIALIZE = MethodTypeDesc.of(
    ConstantDescs.CD_void,
    CD_DATA_BUF_MUT,
    CD_OBJECT_MAPPER,
    ConstantDescs.CD_Object);

  // constants for runtime invocation
  private static final Field[] EMPTY_FIELD_ARRAY = new Field[0];
  private static final MethodType MTR_CONSTRUCTOR = MethodType.methodType(
    void.class,
    Field[].class,
    DataClassCodec.class);

  private DataClassCodecGenerator() {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates the codec implementation for the given class hierarchy, including all given fields. The root class is
   * assumed to be the first element in the given class hierarchy list.
   *
   * @param allFields      all fields in the class hierarchy that are subject to serialization.
   * @param classHierarchy the full class hierarchy that was inspected for fields to include.
   * @return an instance of the generated codec for the root target class.
   * @throws NullPointerException   if the given fields or class hierarchy is null.
   * @throws NoSuchElementException if the given hierarchy list has no elements.
   */
  public static @NonNull DataClassCodec generateClassCodec(
    @NonNull List<Field> allFields,
    @NonNull List<Class<?>> classHierarchy
  ) {
    // get the root type we're generating for & reverse the class hierarchy to generate backwards
    // this is needed as the serializer is basically a tree of serializers, where the root serializer calls
    // the sub serializers that have access to private fields in the target class
    var rootType = classHierarchy.getFirst();
    var reversedHierarchy = classHierarchy.reversed();

    // build a mapping for type -> fields
    Map<Class<?>, List<Field>> fieldsPerType = new HashMap<>();
    for (var field : allFields) {
      var fieldsOfType = fieldsPerType.computeIfAbsent(field.getDeclaringClass(), _ -> new ArrayList<>());
      fieldsOfType.add(field);
    }

    // keep track of the previously generated codec
    DataClassCodec previousCodec = null;
    for (var hierarchyEntry : reversedHierarchy) {
      var root = rootType == hierarchyEntry;
      var fieldsOfType = fieldsPerType.get(hierarchyEntry);
      if (fieldsOfType == null && !root) {
        // while the class is part of the hierarchy it does not define any fields, we can skip the codec class
        continue;
      }

      // only provide all fields to deserialize to the root type deserializer
      var fieldsToDeserialize = root ? allFields.toArray(EMPTY_FIELD_ARRAY) : EMPTY_FIELD_ARRAY;

      try {
        // generate & instantiate the class
        var lookup = generateClassCodecClass(hierarchyEntry, allFields, root);
        var constructor = lookup.findConstructor(lookup.lookupClass(), MTR_CONSTRUCTOR);
        previousCodec = (DataClassCodec) constructor.invoke(fieldsToDeserialize, previousCodec);
      } catch (Throwable throwable) {
        throw new IllegalStateException(
          String.format("unable to instantiate generated codec class for %s", hierarchyEntry.getName()),
          throwable);
      }
    }

    // this is impossible to happen under normal conditions
    Objects.requireNonNull(previousCodec, "at least one codec must have been generated at this point");
    return previousCodec;
  }

  /**
   * Generates and defines the codec for the given target class. The deserialized method is only properly implemented if
   * this is the root type, in all other cases the deserialize method will just return null.
   *
   * @param target    the target class to create the codec for.
   * @param allFields all fields that are subject to serialization, include those not defined in the given target.
   * @param root      if the given target class is the root class for which the codec is generated.
   * @return a lookup with full privileged access to the defined codec class.
   * @throws NullPointerException if the given target class or fields list is null.
   */
  private static @NonNull MethodHandles.Lookup generateClassCodecClass(
    @NonNull Class<?> target,
    @NonNull List<Field> allFields,
    boolean root
  ) {
    // generate the name of the generated class
    var targetClassDesc = ClassDesc.ofDescriptor(target.descriptorString());
    var classDesc = targetClassDesc.nested("RPC_DCC");

    var classFileBytes = ClassFile.of().build(classDesc, classBuilder -> {
      // implement DataClassCodec
      classBuilder.withInterfaceSymbols(CD_DATA_CLASS_CODEC);

      // insert the fields
      classBuilder.withField(FIELDS_FIELD_NAME, CD_FIELD_ARRAY, ClassFile.ACC_PRIVATE | ClassFile.ACC_FINAL);
      classBuilder.withField(SUPER_CODEC_NAME, CD_DATA_CLASS_CODEC, ClassFile.ACC_PRIVATE | ClassFile.ACC_FINAL);

      // insert the constructor
      classBuilder.withMethodBody(
        ConstantDescs.INIT_NAME,
        MT_CONSTRUCTOR,
        ClassFile.ACC_PUBLIC,
        code -> code
          // call super
          .aload(0)
          .invokespecial(ConstantDescs.CD_Object, ConstantDescs.INIT_NAME, ConstantDescs.MTD_void)
          // assign the fields field
          .aload(0)
          .aload(1)
          .putfield(classDesc, FIELDS_FIELD_NAME, CD_FIELD_ARRAY)
          // assign the next codec field
          .aload(0)
          .aload(2)
          .putfield(classDesc, SUPER_CODEC_NAME, CD_DATA_CLASS_CODEC)
          .return_());

      // insert the serialize method
      classBuilder.withMethodBody(
        SERIALIZE_METHOD_NAME,
        MT_SERIALIZE,
        ClassFile.ACC_PUBLIC,
        code -> generateSerializeMethod(code, target, allFields, targetClassDesc, classDesc));

      // insert the deserialize method
      classBuilder.withMethodBody(
        DESERIALIZE_METHOD_NAME,
        MT_DESERIALIZE,
        ClassFile.ACC_PUBLIC,
        code -> {
          if (root) {
            // root type known the target constructor to invoke (all args)
            generateDeserializeMethod(code, allFields, targetClassDesc, classDesc);
          } else {
            // not the root type, this method doesn't need to be implemented
            code.aconst_null().areturn();
          }
        });
    });

    // define the class as a nest mate in the target class
    return CodeGenerationUtil.defineNestedClass(target, classFileBytes);
  }

  /**
   * Generates the serialize method of the data class codec for the given target type.
   *
   * @param code                the code builder of the current codec class.
   * @param target              the target class for which the codec is getting implemented.
   * @param allFields           all fields, include those that are not defined in the given target class.
   * @param targetClassDesc     the class descriptor of the target class.
   * @param generatingClassDesc the class descriptor of the class that is being generated.
   * @throws NullPointerException if one of the parameters is null.
   */
  private static void generateSerializeMethod(
    @NonNull CodeBuilder code,
    @NonNull Class<?> target,
    @NonNull List<Field> allFields,
    @NonNull ClassDesc targetClassDesc,
    @NonNull ClassDesc generatingClassDesc
  ) {
    for (var field : allFields) {
      if (field.getDeclaringClass() != target) {
        // don't implement for fields that are not located in the current class we're working on
        continue;
      }

      var fieldTypeDesc = ClassDesc.ofDescriptor(field.getType().descriptorString());
      var getterMethod = getAndValidateGetterMethod(field);
      if (getterMethod != null) {
        // get the field value from the specified getter method
        var getterMethodType = MethodTypeDesc.of(fieldTypeDesc);
        var getterInInterface = getterMethod.getDeclaringClass().isInterface();
        var invocationOpcode = getterInInterface ? Opcode.INVOKEINTERFACE : Opcode.INVOKEVIRTUAL;
        code
          .aload(2)
          .aload(1)
          .aload(3)
          .checkcast(targetClassDesc)
          .invoke(
            invocationOpcode,
            targetClassDesc,
            getterMethod.getName(),
            getterMethodType,
            getterInInterface);
      } else {
        // get the field value directly
        code
          .aload(2)
          .aload(1)
          .aload(3)
          .checkcast(targetClassDesc)
          .getfield(targetClassDesc, field.getName(), fieldTypeDesc);
      }

      // check if primitive boxing is required
      if (fieldTypeDesc.isPrimitive()) {
        CodeGenerationUtil.boxPrimitive(code, fieldTypeDesc.descriptorString());
      }

      // call objectMapper.write for the field, drop the return value
      code
        .invokeinterface(CD_OBJECT_MAPPER, OBJECT_MAPPER_WRITE_NAME, MT_OBJECT_MAPPER_WRITE)
        .pop();
    }

    // call the super serializer, if present
    code
      .aload(0)
      .getfield(generatingClassDesc, SUPER_CODEC_NAME, CD_DATA_CLASS_CODEC)
      .ifThen(Opcode.IFNONNULL, ifGuardedCode -> ifGuardedCode
        .aload(0)
        .getfield(generatingClassDesc, SUPER_CODEC_NAME, CD_DATA_CLASS_CODEC)
        .aload(1)
        .aload(2)
        .aload(3)
        .invokeinterface(CD_DATA_CLASS_CODEC, SERIALIZE_METHOD_NAME, MT_SERIALIZE));

    // void method
    code.return_();
  }

  /**
   * Generates the deserialize method for the root type of the hierarchy.
   *
   * @param code                the code builder of the current codec class.
   * @param allFields           all fields in the class hierarchy.
   * @param targetClassDesc     the class descriptor of the target class.
   * @param generatingClassDesc the class descriptor of the class that is being generated.
   * @throws NullPointerException if one of the parameters is null.
   */
  private static void generateDeserializeMethod(
    @NonNull CodeBuilder code,
    @NonNull List<Field> allFields,
    @NonNull ClassDesc targetClassDesc,
    @NonNull ClassDesc generatingClassDesc
  ) {
    var fieldCount = allFields.size();
    var parameterTypesStoreSlots = new int[fieldCount];    // keeps track where the constructor params are stored
    var constructorParamTypes = new ClassDesc[fieldCount]; // keeps track of the parameter types of the constructor
    for (var index = 0; index < fieldCount; index++) {
      var field = allFields.get(index);
      var fieldType = ClassDesc.ofDescriptor(field.getType().descriptorString());

      code
        .aload(2)
        .aload(1)
        // load the target field type
        .aload(0)
        .getfield(generatingClassDesc, FIELDS_FIELD_NAME, CD_FIELD_ARRAY)
        .ldc(index)
        .aaload()
        .invokevirtual(CD_FIELD, FIELD_GET_TYPE_NAME, MT_FIELD_GET_TYPE)
        // invoke ObjectMapper.readObject
        .invokeinterface(CD_OBJECT_MAPPER, OBJECT_MAPPER_READ_NAME, MT_OBJECT_MAPPER_READ);

      if (fieldType.isPrimitive()) {
        // decoded value needs to be unboxed
        CodeGenerationUtil.unboxPrimitive(code, fieldType.descriptorString());
      } else {
        // cast the parameter to the desired type
        code.checkcast(fieldType);
      }

      // store the variable on the stack
      var typeKind = TypeKind.fromDescriptor(fieldType.descriptorString());
      var parameterSlot = code.allocateLocal(typeKind);
      code.storeLocal(typeKind, parameterSlot);

      // store the information about the parameter
      constructorParamTypes[index] = fieldType;
      parameterTypesStoreSlots[index] = parameterSlot;
    }

    // begin the target class construction
    code.new_(targetClassDesc).dup();

    // build the method type for the constructor to call & load all parameters
    var constructorMethodType = MethodTypeDesc.of(ConstantDescs.CD_void, constructorParamTypes);
    for (var index = 0; index < fieldCount; index++) {
      var type = constructorParamTypes[index];
      var storeSlot = parameterTypesStoreSlots[index];
      var typeKind = TypeKind.fromDescriptor(type.descriptorString());
      code.loadLocal(typeKind, storeSlot);
    }

    // invoke the constructor and return the constructed value
    code
      .invokespecial(targetClassDesc, ConstantDescs.INIT_NAME, constructorMethodType)
      .areturn();
  }

  /**
   * Finds and validates the getter method provided via an annotation for the given field. This method returns null in
   * case no specific getter is provided for the given field.
   *
   * @param field the field to find the getter method for.
   * @return the getter method for the given field, null if none is specifically provided.
   * @throws NullPointerException if the given field is null.
   */
  private static @Nullable Method getAndValidateGetterMethod(@NonNull Field field) {
    var getterAnnotation = field.getAnnotation(RPCFieldGetter.class);
    if (getterAnnotation == null) {
      // no getter annotation provided
      return null;
    }

    try {
      var getterMethod = field.getDeclaringClass().getDeclaredMethod(getterAnnotation.value());
      if (!field.getType().equals(getterMethod.getReturnType())) {
        // getter returns another type as field
        throw new IllegalStateException(String.format(
          "field %s in %s declared type %s but getter method %s returns %s",
          field.getName(),
          field.getDeclaringClass().getName(),
          field.getType().getName(),
          getterAnnotation.value(),
          getterMethod.getReturnType().getName()));
      }

      return getterMethod;
    } catch (NoSuchMethodException _) {
      // provided getter method does not exist in class
      throw new IllegalStateException(String.format(
        "field %s in %s declared non-existing getter method %s",
        field.getName(), field.getDeclaringClass().getName(), getterAnnotation.value()));
    }
  }
}
