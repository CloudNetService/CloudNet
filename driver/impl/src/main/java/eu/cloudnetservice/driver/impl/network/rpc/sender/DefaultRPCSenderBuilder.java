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

package eu.cloudnetservice.driver.impl.network.rpc.sender;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.impl.network.rpc.introspec.RPCClassMetadata;
import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.NetworkComponent;
import eu.cloudnetservice.driver.network.buffer.DataBufFactory;
import eu.cloudnetservice.driver.network.object.ObjectMapper;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.factory.RPCFactory;
import java.lang.invoke.TypeDescriptor;
import java.util.function.Supplier;
import lombok.NonNull;

/**
 * The default implementation of an RPC sender builder.
 *
 * @since 4.0
 */
public final class DefaultRPCSenderBuilder implements RPCSender.Builder {

  private final RPCFactory sourceFactory;
  private final RPCClassMetadata classMetadata;

  private ObjectMapper objectMapper;
  private DataBufFactory dataBufFactory;
  private Supplier<NetworkChannel> channelSupplier;

  /**
   * Constructs a new builder instance for the target class (given by the class metadata) and the default data buf
   * factory / object mapper from the source RPC factory.
   *
   * @param sourceFactory  the rpc factory that constructed this object.
   * @param classMetadata  the metadata for the target class.
   * @param dataBufFactory the default data buf factory of the source RPC factory.
   * @param objectMapper   the default object mapper of the source RPC factory.
   * @throws NullPointerException if the given class meta, data buf factory or object mapper is null.
   */
  public DefaultRPCSenderBuilder(
    @NonNull RPCFactory sourceFactory,
    @NonNull RPCClassMetadata classMetadata,
    @NonNull DataBufFactory dataBufFactory,
    @NonNull ObjectMapper objectMapper
  ) {
    this.sourceFactory = sourceFactory;
    this.classMetadata = classMetadata;
    this.dataBufFactory = dataBufFactory;
    this.objectMapper = objectMapper;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCSender.Builder targetComponent(@NonNull NetworkComponent networkComponent) {
    return this.targetChannel(networkComponent::firstChannel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCSender.Builder targetChannel(@NonNull NetworkChannel channel) {
    return this.targetChannel(() -> channel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCSender.Builder targetChannel(@NonNull Supplier<NetworkChannel> channelSupplier) {
    this.channelSupplier = channelSupplier;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCSender.Builder excludeMethod(@NonNull String name, @NonNull TypeDescriptor methodDescriptor) {
    this.classMetadata.unregisterMethod(name, methodDescriptor);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCSender.Builder objectMapper(@NonNull ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCSender.Builder dataBufFactory(@NonNull DataBufFactory dataBufFactory) {
    this.dataBufFactory = dataBufFactory;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull RPCSender build() {
    Preconditions.checkArgument(this.channelSupplier != null, "channel supplier must be given");

    var classMetadata = this.classMetadata.freeze(); // immutable & copied - changes no longer reflect into it
    return new DefaultRPCSender(
      classMetadata.targetClass(),
      this.sourceFactory,
      this.objectMapper,
      this.dataBufFactory,
      classMetadata,
      this.channelSupplier);
  }
}
