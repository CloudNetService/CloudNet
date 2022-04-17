/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.network.rpc.generation;

import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.NetworkComponent;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.rpc.object.ObjectMapper;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A context for the generation of a class which implements some kind of api which only calls methods using rpc.
 *
 * @param component           the network component to use when firing the rpc.
 * @param channelSupplier     the supplier for the network component invoked when firing the rpc.
 * @param objectMapper        the object mapper mapping the databuf back to an object.
 * @param dataBufFactory      the databuf factory to use when creating databufs for the generation factory.
 * @param extendingClass      the class the generated class should extend, null for none.
 * @param interfaces          the interfaces the generated class should implement.
 * @param implementAllMethods if all methods or only abstract methods should get implemented.
 * @since 4.0
 */
public record GenerationContext(
  @Nullable NetworkComponent component,
  @Nullable Supplier<NetworkChannel> channelSupplier,
  @Nullable ObjectMapper objectMapper,
  @Nullable DataBufFactory dataBufFactory,
  @Nullable Class<?> extendingClass,
  @NonNull Set<Class<?>> interfaces,
  boolean implementAllMethods
) {

  /**
   * Creates a new builder instance for a generation context.
   *
   * @return a new generation context builder.
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Creates a new generation context based on the given input class.
   *
   * @param clazz the class to either extend or implement.
   * @return a new generation context builder, trying to extend/implement the given class
   * @throws NullPointerException if the given class is null.
   */
  public static @NonNull Builder forClass(@NonNull Class<?> clazz) {
    if (clazz.isInterface()) {
      return builder().addInterfaces(clazz);
    } else {
      return builder().extendingClass(clazz);
    }
  }

  /**
   * Represents a builder for a generation context.
   *
   * @since 4.0
   */
  public static final class Builder {

    private NetworkComponent component;
    private Supplier<NetworkChannel> channelSupplier;
    private ObjectMapper objectMapper;
    private DataBufFactory dataBufFactory;
    private Class<?> extendingClass;
    private boolean implementAllMethods = false;
    private Set<Class<?>> interfaces = new HashSet<>();

    /**
     * Sets the network component to use when firing rpc.
     *
     * @param component the component used to fire.
     * @return the same instance of the builder, for chaining.
     */
    public @NonNull Builder component(@Nullable NetworkComponent component) {
      this.component = component;
      return this;
    }

    /**
     * Sets the supplier for the network component that is used when firing rpc.
     *
     * @param channelSupplier the supplier used to fire.
     * @return the same instance of the builder, for chaining.
     */
    public @NonNull Builder channelSupplier(@Nullable Supplier<NetworkChannel> channelSupplier) {
      this.channelSupplier = channelSupplier;
      return this;
    }

    /**
     * Sets the object mapper responsible for mapping a databuf back to an object.
     *
     * @param objectMapper the object mapper for mapping.
     * @return the same instance of the builder, for chaining.
     */
    public @NonNull Builder objectMapper(@Nullable ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
      return this;
    }

    /**
     * Sets the databuf factory to use when creating databufs for the generation.
     *
     * @param dataBufFactory the databuf factory for the generation.
     * @return the same instance of the builder, for chaining.
     */
    public @NonNull Builder dataBufFactory(@Nullable DataBufFactory dataBufFactory) {
      this.dataBufFactory = dataBufFactory;
      return this;
    }

    /**
     * Sets the class the generated classes based on the build context should extend.
     *
     * @param extendingClass the class to extend.
     * @return the same instance of the builder, for chaining.
     * @throws NullPointerException if the given class is null.
     */
    public @NonNull Builder extendingClass(@Nullable Class<?> extendingClass) {
      this.extendingClass = extendingClass;
      return this;
    }

    /**
     * Sets if all methods of the target class should get implemented or only abstract methods.
     *
     * @param implementAllMethods if all methods should get implemented.
     * @return the same instance of the builder, for chaining.
     */
    public @NonNull Builder implementAllMethods(boolean implementAllMethods) {
      this.implementAllMethods = implementAllMethods;
      return this;
    }

    /**
     * Sets all interfaces the generated class based on the build context should implement.
     *
     * @param interfaces the classes to implement.
     * @return the same instance of the builder, for chaining.
     * @throws NullPointerException if the given interface array is null.
     */
    public @NonNull Builder interfaces(@NonNull Set<Class<?>> interfaces) {
      this.interfaces = new HashSet<>(interfaces);
      return this;
    }

    /**
     * Adds the given interfaces to the set of interfaces which should get implemented by the generated class.
     *
     * @param interfaces the interfaces to add.
     * @return the same instance of the builder, for chaining.
     * @throws NullPointerException if the given interface array is null.
     */
    public @NonNull Builder addInterfaces(@NonNull Class<?>... interfaces) {
      this.interfaces.addAll(Arrays.asList(interfaces));
      return this;
    }

    /**
     * Builds a new generation context based on this builder.
     *
     * @return a new generation context based on this builder.
     */
    public @NonNull GenerationContext build() {
      return new GenerationContext(
        this.component,
        this.channelSupplier,
        this.objectMapper,
        this.dataBufFactory,
        this.extendingClass,
        this.interfaces,
        this.implementAllMethods);
    }
  }
}
