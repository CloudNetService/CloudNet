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

package eu.cloudnetservice.driver.impl.network.rpc.generation;

import com.google.common.hash.Hashing;
import eu.cloudnetservice.driver.impl.network.rpc.introspec.DefaultRPCMethodMetadata;
import eu.cloudnetservice.driver.impl.network.rpc.introspec.RPCClassMetadata;
import java.lang.classfile.ClassBuilder;
import java.lang.classfile.CodeBuilder;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Contextual information that are available during the complete generation process. For example used to collect fields
 * that will be generated into the class after all other generation finishes.
 *
 * @since 4.0
 */
final class RPCGenerationContext {

  private final RPCGenerationCache generationCache;

  private final Set<TypeDescriptorField> typeDescriptorFields = new LinkedHashSet<>();
  private final Set<AdditionalInstanceFactoryField> additionalInstanceFactoriesFields = new LinkedHashSet<>();

  MethodTypeDesc superclassConstructorDesc;

  /**
   * Constructs a new rpc generation context.
   *
   * @param generationCache the cache for generated class implementation.
   * @throws NullPointerException if the given cache is null.
   */
  public RPCGenerationContext(@NonNull RPCGenerationCache generationCache) {
    this.generationCache = generationCache;
  }

  /**
   * Get the count of additional instance factories that were registered.
   *
   * @return the count of additional instance factories that were registered.
   */
  public int additionalInstanceFactoryCount() {
    return this.additionalInstanceFactoriesFields.size();
  }

  /**
   * Get the additional instance factories that were registered during the implementation build process.
   *
   * @return the additional instance factories registered during the implementation build.
   */
  public @NonNull List<RPCInternalInstanceFactory> additionalInstanceFactories() {
    return this.additionalInstanceFactoriesFields.stream()
      .map(AdditionalInstanceFactoryField::instanceFactory)
      .toList();
  }

  /**
   * Registers an additional instance factory that will be injected into the generated class during instantiation.
   *
   * @param generationFlags    the options to apply during class generation.
   * @param superType          the target type that is constructed by the instance factory.
   * @param baseImplementation the base implementation class for the super type, can be the same as the super type.
   * @return the name of the final factory field to access it.
   * @throws NullPointerException if the given super type or base implementation is null.
   */
  public @NonNull String registerAdditionalInstanceFactory(
    int generationFlags,
    @NonNull Class<?> superType,
    @NonNull Class<?> baseImplementation
  ) {
    var targetClassMeta = RPCClassMetadata.introspect(baseImplementation);
    var generatedInstanceFactory = this.generationCache.getOrGenerateImplementation(
      generationFlags,
      superType,
      targetClassMeta);
    return this.registerAdditionalInstanceFactory(superType, generatedInstanceFactory);
  }

  /**
   * Registers an additional instance factory that will be injected into the generated class during instantiation.
   *
   * @param targetType the target type that is constructed by the instance factory.
   * @param factory    the instance factory that should be assigned to the field.
   * @return the name of the final factory field to access it.
   * @throws NullPointerException if the given target type or instance factory is null.
   */
  public @NonNull String registerAdditionalInstanceFactory(
    @NonNull Class<?> targetType,
    @NonNull RPCInternalInstanceFactory factory
  ) {
    var factoryField = AdditionalInstanceFactoryField.forFactory(targetType, factory);
    this.additionalInstanceFactoriesFields.add(factoryField);
    return factoryField.name();
  }

  /**
   * Registers the need for a type descriptor field for the given method metadata and returns the field name of the
   * final field that will be added and initialized to the class.
   *
   * @param methodMetadata the method metadata for which the descriptor field should be registered.
   * @return the name of the final descriptor field to access it.
   * @throws NullPointerException if the given method metadata is null.
   */
  public @NonNull String registerTypeDescriptorField(@NonNull DefaultRPCMethodMetadata methodMetadata) {
    var descriptorField = TypeDescriptorField.forMethod(methodMetadata);
    this.typeDescriptorFields.add(descriptorField);
    return descriptorField.name();
  }

  /**
   * Registers everything in this context to the class and constructor of the class.
   *
   * @param generatingClass the descriptor of the class that is being generated.
   * @param classBuilder    the builder of the class being generated.
   * @param constructorCode the builder of the constructor code for the current class.
   * @throws NullPointerException if the current class desc, class builder or constructor builder is null.
   */
  public void applyToClassAndConstructor(
    @NonNull ClassDesc generatingClass,
    @NonNull ClassBuilder classBuilder,
    @NonNull CodeBuilder constructorCode
  ) {
    // add type descriptor fields
    for (var typeDescriptorField : this.typeDescriptorFields) {
      typeDescriptorField.applyToClassAndConstructor(generatingClass, classBuilder, constructorCode);
    }

    // add additional instance factory fields
    var currentFactoryIndex = 1;
    for (var factoryField : this.additionalInstanceFactoriesFields) {
      factoryField.applyToClassAndConstructor(currentFactoryIndex++, generatingClass, classBuilder, constructorCode);
    }
  }

  /**
   * Information about the request of adding a type descriptor field into the final class. It holds the field name that
   * should be assigned and the method descriptor string the field should be initialized with.
   *
   * @param name       the name to use for the generated field.
   * @param descriptor the method descriptor to use for field initialization.
   * @since 4.0
   */
  private record TypeDescriptorField(
    @NonNull String name,
    @NonNull String descriptor
  ) {

    /**
     * Generates a type descriptor field instance for the given method metadata. This method uses a deterministic way to
     * generate the field name, so each invocation leads to the same field name result.
     *
     * @param methodMetadata the method metadata to generate the descriptor info for.
     * @return the generated descriptor field information.
     * @throws NullPointerException if the given method metadata is null.
     */
    public static @NonNull TypeDescriptorField forMethod(@NonNull DefaultRPCMethodMetadata methodMetadata) {
      var descriptor = methodMetadata.methodType().descriptorString();
      var hashedDescriptor = Hashing.murmur3_128().hashString(descriptor, StandardCharsets.UTF_8).toString();
      var fieldName = String.format("rpc_td_%s_%s", methodMetadata.name(), hashedDescriptor);
      return new TypeDescriptorField(fieldName, descriptor);
    }

    /**
     * Adds this field into the target class builder and adds code to the constructor to initialize the field value.
     *
     * @param generatingClass the descriptor of the class that is being generated.
     * @param classBuilder    the builder of the class being generated.
     * @param constructorCode the builder of the constructor code for the current class.
     * @throws NullPointerException if the current class desc, class builder or constructor builder is null.
     */
    public void applyToClassAndConstructor(
      @NonNull ClassDesc generatingClass,
      @NonNull ClassBuilder classBuilder,
      @NonNull CodeBuilder constructorCode
    ) {
      // add the field to the class
      classBuilder.withField(this.name, RPCGenerationConstants.CD_TYPE_DESC, RPCGenerationConstants.AFM_FIELD_PF);

      // add the init code to the constructor
      constructorCode
        .aload(0)
        .ldc(this.descriptor)
        .invokestatic(
          ConstantDescs.CD_MethodTypeDesc,
          "ofDescriptor",
          RPCGenerationConstants.MTD_MTD_OF_DESCRIPTOR,
          true)
        .putfield(generatingClass, this.name, RPCGenerationConstants.CD_TYPE_DESC);
    }
  }

  /**
   * A field that holds an additional instance factory that is accessed to allocate instances of chain classes.
   *
   * @param name            the name of the field.
   * @param targetType      the target type that is being constructed by the factory.
   * @param instanceFactory the instance factory.
   * @since 4.0
   */
  private record AdditionalInstanceFactoryField(
    @NonNull String name,
    @NonNull Class<?> targetType,
    @NonNull RPCInternalInstanceFactory instanceFactory
  ) {

    /**
     * Constructs a field that holds an additional instance factory for the given target type.
     *
     * @param targetType      the target type which is constructed by the instance factory.
     * @param instanceFactory the instance factory that should be used for initialisation.
     * @return the generated factory field information.
     * @throws NullPointerException if the given target type or instance factory is null.
     */
    public static @NonNull AdditionalInstanceFactoryField forFactory(
      @NonNull Class<?> targetType,
      @NonNull RPCInternalInstanceFactory instanceFactory
    ) {
      var hashedName = Hashing.murmur3_128().hashString(targetType.getName(), StandardCharsets.UTF_8).toString();
      var fieldName = String.format("rpc_if_%s", hashedName);
      return new AdditionalInstanceFactoryField(fieldName, targetType, instanceFactory);
    }

    /**
     * Adds this field into the target class builder and adds code to the constructor to initialize the field value.
     * This method uses a deterministic way to generate the field name, so each invocation leads to the same field name
     * result.
     *
     * @param fieldIndex      the index of the current field that is being added to the class (1-based).
     * @param generatingClass the descriptor of the class that is being generated.
     * @param classBuilder    the builder of the class being generated.
     * @param constructorCode the builder of the constructor code for the current class.
     * @throws NullPointerException if the current class desc, class builder or constructor builder is null.
     */
    public void applyToClassAndConstructor(
      int fieldIndex,
      @NonNull ClassDesc generatingClass,
      @NonNull ClassBuilder classBuilder,
      @NonNull CodeBuilder constructorCode
    ) {
      // add the field to the class
      classBuilder.withField(
        this.name,
        RPCGenerationConstants.CD_INT_INSTANCE_FACTORY,
        RPCGenerationConstants.AFM_FIELD_PF);

      // add code to the constructor that assigns the parameter to the field
      var otherParamsCount = RPCInternalInstanceFactory.MTD_BASIC_IMPLEMENTATION_CONSTRUCTOR.parameterCount();
      constructorCode
        .aload(0)
        .aload(otherParamsCount + fieldIndex)
        .putfield(generatingClass, this.name, RPCGenerationConstants.CD_INT_INSTANCE_FACTORY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || this.getClass() != o.getClass()) {
        return false;
      }
      var that = (AdditionalInstanceFactoryField) o;
      return Objects.equals(this.targetType, that.targetType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return Objects.hashCode(this.targetType);
    }
  }
}
