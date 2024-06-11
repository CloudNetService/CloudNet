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

package eu.cloudnetservice.driver.network.rpc.defaults.generation;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.rpc.ChainableRPC;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The internal instance factory for generated rpc api implementations. This class is only used within this package to
 * hide the actual work from the public-facing api.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public sealed class RPCInternalInstanceFactory {

  private final Runnable allocationNotifier;

  private final int userArgCount;
  private final RPCSender classRPCSender;
  private final RPCInternalInstanceFactory[] additionalInstanceFactories;

  private final MethodHandle constructorMethodHandle;

  /**
   * Internal constructor for TempRPCInternalInstanceFactory, do not use outside that implementation.
   */
  private RPCInternalInstanceFactory() {
    this.allocationNotifier = null;
    this.userArgCount = 0;
    this.classRPCSender = null;
    this.additionalInstanceFactories = null;
    this.constructorMethodHandle = null;
  }

  /**
   * Constructs a new internal instance factory.
   *
   * @param userArgCount                the count of arguments that are additionally supplied by the user.
   * @param allocationNotifier          a runnable to run every time an instance of the class is allocated.
   * @param classRPCSender              the rpc sender instance to use for rpc lookups in the target class.
   * @param additionalInstanceFactories additional instance factories required for chained rpc constructions.
   * @param constructorMethodHandle     the method handle for the constructor to invoke for construction requests.
   * @throws NullPointerException if one of the given parameters is null.
   */
  private RPCInternalInstanceFactory(
    int userArgCount,
    @NonNull Runnable allocationNotifier,
    @NonNull RPCSender classRPCSender,
    @NonNull RPCInternalInstanceFactory[] additionalInstanceFactories,
    @NonNull MethodHandle constructorMethodHandle
  ) {
    this.allocationNotifier = allocationNotifier;
    this.userArgCount = userArgCount;
    this.classRPCSender = classRPCSender;
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
        var methodType = MethodType.methodType(
          void.class,
          Supplier.class,
          RPCSender.class,
          ChainableRPC.class,
          Object[].class);
        var constructorHandle = lookup.findConstructor(lookup.lookupClass(), methodType);
        return new RPCInternalInstanceFactory(
          userArgCount,
          allocationNotifier,
          classRPCSender,
          additionalInstanceFactories,
          constructorHandle);
      } else {
        // construct the parameter types array & insert the static 4 types
        var pTypeCount = 4 + additionalInstanceFactories.length;
        var pTypesArray = new Class<?>[pTypeCount];
        pTypesArray[0] = Supplier.class;
        pTypesArray[1] = RPCSender.class;
        pTypesArray[2] = ChainableRPC.class;
        pTypesArray[pTypeCount - 1] = Object[].class;

        // insert the static instance factories parameters N times
        for (var index = 0; index < pTypeCount; index++) {
          pTypesArray[3 + index] = RPCInternalInstanceFactory.class;
        }

        // resolve the constructor handle for the
        var methodType = MethodType.methodType(void.class, pTypesArray);
        var directConstructorHandle = lookup.findConstructor(lookup.lookupClass(), methodType);
        var spreadingConstructorHandle = directConstructorHandle.asSpreader(
          3,
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
      if (this.additionalInstanceFactories.length == 0) {
        // no additional instance factories present, skip that parameter completely
        return this.constructorMethodHandle.invoke(
          channelSupplier,
          this.classRPCSender,
          baseRPC,
          additionalConstructorArgs);
      } else {
        // additional instance factories present, insert the parameter
        return this.constructorMethodHandle.invoke(
          channelSupplier,
          this.classRPCSender,
          baseRPC,
          this.additionalInstanceFactories,
          additionalConstructorArgs);
      }
    } catch (Throwable throwable) {
      throw new IllegalStateException("unable to construct instance of generated rpc class", throwable);
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
      var delegate = this.delegate;
      Preconditions.checkState(delegate != null, "delegate not yet set");
      return delegate.constructInstance(baseRPC, channelSupplier, additionalConstructorArgs);
    }
  }
}
