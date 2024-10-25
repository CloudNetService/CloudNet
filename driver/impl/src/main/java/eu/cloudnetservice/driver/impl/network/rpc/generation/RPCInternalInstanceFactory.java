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

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.rpc.ChainableRPC;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The internal instance factory for generated rpc api implementations. This class is only used within this package to
 * hide the actual work from the public-facing api.
 *
 * @since 4.0
 */
public sealed class RPCInternalInstanceFactory {

  // ImplementationClass(Supplier, RPCSender, ChainableRPC, Object[])
  static final MethodType MT_BASIC_IMPLEMENTATION_CONSTRUCTOR = MethodType.methodType(
    void.class,
    Supplier.class,
    RPCSender.class,
    ChainableRPC.class,
    Object[].class);
  static final MethodTypeDesc MTD_BASIC_IMPLEMENTATION_CONSTRUCTOR =
    MethodTypeDesc.ofDescriptor(MT_BASIC_IMPLEMENTATION_CONSTRUCTOR.descriptorString());

  private final Runnable allocationNotifier;

  private final int userArgCount;
  private final RPCSender fallbackClassRPCSender;
  private final RPCInternalInstanceFactory[] additionalInstanceFactories;

  private final MethodHandle constructorMethodHandle;

  /**
   * Internal constructor for TempRPCInternalInstanceFactory, do not use outside that implementation.
   */
  private RPCInternalInstanceFactory() {
    this.allocationNotifier = null;
    this.userArgCount = 0;
    this.fallbackClassRPCSender = null;
    this.additionalInstanceFactories = null;
    this.constructorMethodHandle = null;
  }

  /**
   * Constructs a new internal instance factory.
   *
   * @param userArgCount                the count of arguments that are additionally supplied by the user.
   * @param allocationNotifier          a runnable to run every time an instance of the class is allocated.
   * @param fallbackClassRPCSender      the rpc sender instance to use for rpc lookups in the target class.
   * @param additionalInstanceFactories additional instance factories required for chained rpc constructions.
   * @param constructorMethodHandle     the method handle for the constructor to invoke for construction requests.
   * @throws NullPointerException if one of the given parameters is null.
   */
  private RPCInternalInstanceFactory(
    int userArgCount,
    @NonNull Runnable allocationNotifier,
    @NonNull RPCSender fallbackClassRPCSender,
    @NonNull RPCInternalInstanceFactory[] additionalInstanceFactories,
    @NonNull MethodHandle constructorMethodHandle
  ) {
    this.allocationNotifier = allocationNotifier;
    this.userArgCount = userArgCount;
    this.fallbackClassRPCSender = fallbackClassRPCSender;
    this.additionalInstanceFactories = additionalInstanceFactories;
    this.constructorMethodHandle = constructorMethodHandle;
  }

  /**
   * Constructs a new internal instance factory for the generated rpc api implementation class.
   *
   * @param userArgCount                the count of arguments that are additionally supplied by the user.
   * @param lookup                      a lookup method handle targeting the generated implementation class.
   * @param allocationNotifier          a runnable to run every time an instance of the class is allocated.
   * @param classRPCSender              the rpc sender instance to use for rpc lookups in the target class.
   * @param additionalInstanceFactories additional instance factories required for chained rpc constructions.
   * @return a new internal instance factory to construct a generated api implementation.
   * @throws NullPointerException  if one of the given parameters is null.
   * @throws IllegalStateException if the target constructor is missing in the generated class.
   */
  public static @NonNull RPCInternalInstanceFactory make(
    int userArgCount,
    @NonNull MethodHandles.Lookup lookup,
    @NonNull Runnable allocationNotifier,
    @NonNull RPCSender classRPCSender,
    @NonNull RPCInternalInstanceFactory[] additionalInstanceFactories
  ) {
    try {
      if (additionalInstanceFactories.length == 0) {
        // no additional instance factories required
        var constructorHandle = lookup.findConstructor(lookup.lookupClass(), MT_BASIC_IMPLEMENTATION_CONSTRUCTOR);
        return new RPCInternalInstanceFactory(
          userArgCount,
          allocationNotifier,
          classRPCSender,
          additionalInstanceFactories,
          constructorHandle);
      } else {
        // construct an array with the amount of instance factories that is required by the constructor
        var instanceFactoryParamTypes = new Class<?>[additionalInstanceFactories.length];
        Arrays.fill(instanceFactoryParamTypes, RPCInternalInstanceFactory.class);

        // resolve the constructor handle for the
        var methodType = MT_BASIC_IMPLEMENTATION_CONSTRUCTOR.appendParameterTypes(instanceFactoryParamTypes);
        var directConstructorHandle = lookup.findConstructor(lookup.lookupClass(), methodType);
        var spreadingConstructorHandle = directConstructorHandle.asSpreader(
          MT_BASIC_IMPLEMENTATION_CONSTRUCTOR.parameterCount(),
          RPCInternalInstanceFactory[].class,
          additionalInstanceFactories.length);
        return new RPCInternalInstanceFactory(
          userArgCount,
          allocationNotifier,
          classRPCSender,
          additionalInstanceFactories,
          spreadingConstructorHandle);
      }
    } catch (NoSuchMethodException | IllegalAccessException exception) {
      throw new IllegalStateException("unable to resolve constructor in generated rpc class", exception);
    }
  }

  /**
   * Constructs a new instance of the generated rpc class.
   *
   * @param baseRPC                   the base RPC to use for chained api calls.
   * @param channelSupplier           the channel supplier to which all network requests should be sent.
   * @param additionalConstructorArgs the array of additional constructor args to supply to the generated class.
   * @return a constructed instance of the generated rpc class implementation.
   * @throws NullPointerException     if the given channel supplier or additional args is null.
   * @throws IllegalArgumentException if the given additional constructor args mismatch the expected count.
   * @throws IllegalStateException    if the construction of the generated class is not possible.
   */
  public @NonNull Object constructInstance(
    @Nullable ChainableRPC baseRPC,
    @NonNull Supplier<NetworkChannel> channelSupplier,
    @NonNull Object[] additionalConstructorArgs
  ) {
    return this.constructInstance(baseRPC, null, channelSupplier, additionalConstructorArgs);
  }

  /**
   * Constructs a new instance of the generated rpc class.
   *
   * @param baseRPC                   the base RPC to use for chained api calls.
   * @param classRPCSender            the rpc sender to use instead of the fallback one given to this factory.
   * @param channelSupplier           the channel supplier to which all network requests should be sent.
   * @param additionalConstructorArgs the array of additional constructor args to supply to the generated class.
   * @return a constructed instance of the generated rpc class implementation.
   * @throws NullPointerException     if the given channel supplier or additional args is null.
   * @throws IllegalArgumentException if the given additional constructor args mismatch the expected count.
   * @throws IllegalStateException    if the construction of the generated class is not possible.
   */
  public @NonNull Object constructInstance(
    @Nullable ChainableRPC baseRPC,
    @Nullable RPCSender classRPCSender,
    @NonNull Supplier<NetworkChannel> channelSupplier,
    @NonNull Object[] additionalConstructorArgs
  ) {
    // sanity check to suppress ide warnings, but this can only happen it called from
    // TempRPCInternalInstanceFactory which should never be the case
    Objects.requireNonNull(this.allocationNotifier, "call to super from TempRPCInternalInstanceFactory");
    Objects.requireNonNull(this.constructorMethodHandle, "call to super from TempRPCInternalInstanceFactory");
    Objects.requireNonNull(this.additionalInstanceFactories, "call to super from TempRPCInternalInstanceFactory");

    if (additionalConstructorArgs.length != this.userArgCount) {
      // sanity check that the additional constructor args are valid
      throw new IllegalArgumentException(String.format(
        "illegal additional constructor parameter count, expected %d, got %d",
        this.userArgCount, additionalConstructorArgs.length));
    }

    // notify about instance allocation
    this.allocationNotifier.run();

    try {
      this.replaceSpecialArgs(additionalConstructorArgs, baseRPC, channelSupplier);
      var rpcSender = Objects.requireNonNullElse(classRPCSender, this.fallbackClassRPCSender);
      if (this.additionalInstanceFactories.length == 0) {
        // no additional instance factories present, skip that parameter completely
        return this.constructorMethodHandle.invoke(
          channelSupplier,
          rpcSender,
          baseRPC,
          additionalConstructorArgs);
      } else {
        // additional instance factories present, insert the parameter
        return this.constructorMethodHandle.invoke(
          channelSupplier,
          rpcSender,
          baseRPC,
          additionalConstructorArgs,
          this.additionalInstanceFactories);
      }
    } catch (Throwable throwable) {
      throw new IllegalStateException("unable to construct instance of generated rpc class", throwable);
    }
  }

  /**
   * Replaces all requests for special arguments in the given target array.
   *
   * @param args            the argument to array to replace the special values in.
   * @param baseRPC         the base RPC to use for chained api calls.
   * @param channelSupplier the channel supplier to which all network requests should be sent.
   * @throws NullPointerException if the given args array or base rpc is null.
   */
  private void replaceSpecialArgs(
    Object[] args,
    @Nullable ChainableRPC baseRPC,
    @NonNull Supplier<NetworkChannel> channelSupplier
  ) {
    for (var index = 0; index < args.length; index++) {
      var element = args[index];
      if (element instanceof SpecialArg specialArg) {
        switch (specialArg) {
          case RPC_SENDER -> args[index] = this.fallbackClassRPCSender;
          case RPC_CHAIN_BASE -> args[index] = baseRPC;
          case CHANNEL_SUPPLIER -> args[index] = channelSupplier;
        }
      }
    }
  }

  /**
   * A collection of special args that can be passed to a super constructor when invoking. If the extra argument array
   * on an instance construct call contains this enum, the value is replaced with the value passed to the instance
   * factory.
   *
   * @since 4.0
   */
  public enum SpecialArg {

    /**
     * Special pointer that will be replaced with the rpc sender responsible for the target class.
     */
    RPC_SENDER(RPCSender.class),
    /**
     * The base rpc used for the class invocation, can be null.
     */
    RPC_CHAIN_BASE(ChainableRPC.class),
    /**
     * The channel supplier to get the channel into which the rpc calls should be sent.
     */
    CHANNEL_SUPPLIER(Supplier.class);

    private static final SpecialArg[] VALUES = values();
    public static final int SPECIAL_ARG_MAX_INDEX = -(VALUES.length);

    private final Class<?> argType;

    /**
     * Constructs a new special arg.
     *
     * @param argType the type that will be injected in the final step.
     * @throws NullPointerException if the given arg type is null.
     */
    SpecialArg(@NonNull Class<?> argType) {
      this.argType = argType;
    }

    /**
     * Get the special argument represented by the given param mapping index or throws an exception if the param mapping
     * index is invalid.
     *
     * @param paramMappingIndex the param mapping index to get the special arg for.
     * @return the special arg mapped to the given param mapping index.
     * @throws IllegalStateException if the given param mapping value is invalid.
     */
    static @NonNull SpecialArg fromParamMappingIndex(int paramMappingIndex) {
      var index = -paramMappingIndex - 1;
      if (index >= 0 && index < VALUES.length) {
        return VALUES[index];
      } else {
        throw new IllegalStateException(String.format("illegal special param mapping index %d", paramMappingIndex));
      }
    }

    /**
     * Get the argument type that will be injected into the constructor.
     *
     * @return the argument type that will be injected into the constructor.
     */
    @NonNull
    Class<?> argType() {
      return this.argType;
    }
  }

  /**
   * An instance factory implementation that is temporarily not delegated (during class construction) to prevent issues
   * with circular class references.
   *
   * @since 4.0
   */
  static final class TempRPCInternalInstanceFactory extends RPCInternalInstanceFactory {

    private RPCInternalInstanceFactory delegate;

    /**
     * Sets the delegate instance factory if that didn't happen yet.
     *
     * @param delegate the delegate instance factory to set.
     * @throws NullPointerException if the given delegate is null.
     */
    public void setDelegate(@NonNull RPCInternalInstanceFactory delegate) {
      if (this.delegate == null) {
        this.delegate = delegate;
      }
    }

    /**
     * Uses the delegate instance of this temp factory to construct the target rpc implementation class.
     *
     * @param baseRPC                   the base RPC to use for chained api calls.
     * @param channelSupplier           the channel supplier to which all network requests should be sent.
     * @param additionalConstructorArgs the array of additional constructor args to supply to the generated class.
     * @return a constructed instance of the generated rpc class implementation.
     * @throws NullPointerException     if the given channel supplier or additional args is null.
     * @throws IllegalArgumentException if the given additional constructor args mismatch the expected count.
     * @throws IllegalStateException    if this factory is not yet delegated or the construction fails.
     */
    @Override
    public @NonNull Object constructInstance(
      @Nullable ChainableRPC baseRPC,
      @NonNull Supplier<NetworkChannel> channelSupplier,
      @NonNull Object[] additionalConstructorArgs
    ) {
      return this.constructInstance(baseRPC, null, channelSupplier, additionalConstructorArgs);
    }

    /**
     * Uses the delegate instance of this temp factory to construct the target rpc implementation class.
     *
     * @param baseRPC                   the base RPC to use for chained api calls.
     * @param classRPCSender            the rpc sender to use instead of the fallback one given to this factory.
     * @param channelSupplier           the channel supplier to which all network requests should be sent.
     * @param additionalConstructorArgs the array of additional constructor args to supply to the generated class.
     * @return a constructed instance of the generated rpc class implementation.
     * @throws NullPointerException     if the given channel supplier or additional args is null.
     * @throws IllegalArgumentException if the given additional constructor args mismatch the expected count.
     * @throws IllegalStateException    if this factory is not yet delegated or the construction fails.
     */
    @Override
    public @NonNull Object constructInstance(
      @Nullable ChainableRPC baseRPC,
      @Nullable RPCSender classRPCSender,
      @NonNull Supplier<NetworkChannel> channelSupplier,
      @NonNull Object[] additionalConstructorArgs
    ) {
      var delegate = this.delegate;
      Preconditions.checkState(delegate != null, "delegate not yet set");
      return delegate.constructInstance(baseRPC, classRPCSender, channelSupplier, additionalConstructorArgs);
    }
  }
}
