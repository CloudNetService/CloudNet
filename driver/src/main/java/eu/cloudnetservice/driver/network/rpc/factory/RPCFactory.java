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

package eu.cloudnetservice.driver.network.rpc.factory;

import eu.cloudnetservice.driver.network.rpc.handler.RPCHandler;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.exception.ClassCreationException;
import eu.cloudnetservice.driver.network.rpc.generation.GenerationContext;
import lombok.NonNull;

/**
 * A factory which can provide anything which is related to rpc.
 *
 * @since 4.0
 */
public interface RPCFactory {

  /**
   * Constructs a new rpc sender builder which should be capable of executing methods in the given target class.
   *
   * @param target the target class in which the sender should be able to execute methods.
   * @return a new builder for an RPC sender to execute methods in the given target class.
   * @throws NullPointerException if the given target class is null.
   */
  @NonNull
  RPCSender.Builder newRPCSenderBuilder(@NonNull Class<?> target);

  @NonNull
  <T> RPCHandler.Builder<T> newRPCHandlerBuilder(@NonNull Class<T> target);

  /**
   * Generates an api implementation for the given base class, invoking all of its method using rpc. This method only
   * overrides methods which are abstract in the given class tree. In other words, if you're passing an implementation
   * which has all methods which need to be processed locally already done, no rpc based method implementation will be
   * generated for the class.
   * <p>
   * The defined base class in the context must define one of the following constructors:
   * <ol>
   *   <li>A constructor with no arguments, or
   *   <li>A constructor taking exactly one argument, a rpc sender instance.
   * </ol>
   * Note: the constructor taking the rpc sender instance is preferred by the generator over the no args constructor.
   * <p>
   * This method will cache the result of the generation, calling this method twice will only result in two different
   * instances in the same class, but only if the given generation context did not change between the calls.
   *
   * @param baseClass the base class to generate the class methods based on.
   * @param context   the context of the class generation, holding the options for it.
   * @param <T>       the type which gets generated.
   * @return an implementation of the given class which has all abstract methods rpc based implemented.
   * @throws NullPointerException   if the given base class or generation context is null.
   * @throws ClassCreationException if the generator is unable to generate an implementation of the class.
   */
  @NonNull
  <T> RPCImplementationBuilder.ForBasic<T> newBasicRPCBasedImplementationBuilder(@NonNull Class<T> baseClass);

  /**
   * Generates an api implementation for the given base class, invoking all of its method using a chained rpc call. The
   * base rpc is discovered using the given rpc sender and the name of the method which called the method. The invoker
   * of the method must be direct (1 up in the local stack trace). If the method name cannot be determined by the stack,
   * use {@link #generateRPCChainBasedApi(RPCSender, String, Class, GenerationContext)} to supply the name of the method
   * to use instead. This method only overrides methods which are abstract in the given class tree. In other words, if
   * you're passing an implementation which has all methods which need to be processed locally already done, no rpc
   * based method implementation will be generated for the class.
   * <p>
   * If the base class defined in the invocation context needs arguments to be supplied, they must get passed to the
   * created instance factory. Note: the constructor parameters are not required to match the parameters supplied to the
   * base rpc call.
   * <p>
   * This method will cache the result of the generation, calling this method twice will only result in two different
   * instances in the same class, but only if the given generation context and the calling method name did not change
   * between the calls.
   *
   * @param baseSender     the rpc sender for the base class from which the chain should start.
   * @param chainBaseClass the base class from which methods one step into the chain should get called.
   * @param context        the context of the class generation, holding the options for it.
   * @param <T>            the type which gets generated.
   * @return a factory which is capable to create new instances of the given chain base class, for rpc chain calls.
   * @throws NullPointerException   if the given base sender, chain base class or generation context is null.
   * @throws ClassCreationException if the generator is unable to generate an implementation of the class.
   */
  @NonNull
  <T> RPCImplementationBuilder.ForChained<T> newChainedRPCBasedImplementationBuilder(@NonNull Class<T> baseClass);
}
