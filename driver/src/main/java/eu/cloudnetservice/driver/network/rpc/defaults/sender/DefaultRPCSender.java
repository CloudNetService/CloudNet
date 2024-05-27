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

package eu.cloudnetservice.driver.network.rpc.defaults.sender;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import eu.cloudnetservice.driver.network.NetworkComponent;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.rpc.RPC;
import eu.cloudnetservice.driver.network.rpc.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.defaults.DefaultRPCProvider;
import eu.cloudnetservice.driver.network.rpc.defaults.MethodInformation;
import eu.cloudnetservice.driver.network.rpc.defaults.rpc.DefaultRPC;
import eu.cloudnetservice.driver.network.rpc.object.ObjectMapper;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default implementation of a rpc sender.
 *
 * @since 4.0
 */
public class DefaultRPCSender extends DefaultRPCProvider implements RPCSender {

  protected static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

  protected final Class<?> targetClass;
  protected final RPCFactory factory;
  protected final NetworkComponent networkComponent;
  protected final Cache<String, MethodInformation> cachedMethodInformation;

  /**
   * Constructs a new default rpc sender instance.
   *
   * @param factory        the factory used to create this instance.
   * @param component      the network component which is associated with this component, might be null.
   * @param targetClass    the target call for method invocations sent by this sender.
   * @param objectMapper   the object mapper to use to write and read data from the buffers.
   * @param dataBufFactory the buffer factory used for buffer allocations.
   * @throws NullPointerException if one of the required constructor parameters is null.
   */
  public DefaultRPCSender(
    @NonNull RPCFactory factory,
    @Nullable NetworkComponent component,
    @NonNull Class<?> targetClass,
    @NonNull ObjectMapper objectMapper,
    @NonNull DataBufFactory dataBufFactory
  ) {
    super(targetClass, objectMapper, dataBufFactory);

    this.factory = factory;
    this.targetClass = targetClass;
    this.networkComponent = component;
    this.cachedMethodInformation = Caffeine.newBuilder().build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCFactory factory() {
    return this.factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull NetworkComponent associatedComponent() {
    // possible to create without an associated component - throw an exception if so
    if (this.networkComponent == null) {
      throw new UnsupportedOperationException("Sender has no associated component");
    }
    return this.networkComponent;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPC invokeMethod(@NonNull String methodName) {
    return this.invokeMethod(methodName, EMPTY_OBJECT_ARRAY);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPC invokeMethod(@NonNull String methodName, Object... args) {
    // find the method information of the method we want to invoke
    var information = this.cachedMethodInformation.get(
      String.format("%s@%d", methodName, args.length),
      $ -> MethodInformation.find(null, this.targetClass, methodName, null, args.length));
    // generate the rpc from this information
    return new DefaultRPC(
      this,
      this.targetClass,
      methodName,
      args,
      this.objectMapper,
      information.returnType(),
      this.dataBufFactory);
  }
}
