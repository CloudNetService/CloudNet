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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import eu.cloudnetservice.driver.impl.network.rpc.DefaultRPCFactory;
import eu.cloudnetservice.driver.impl.network.rpc.introspec.RPCClassMetadata;
import eu.cloudnetservice.driver.impl.network.rpc.sender.DefaultRPCSender;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import java.time.Duration;
import lombok.NonNull;

/**
 * The cache for rpc implementation generations to prevent multiple generations for the same class.
 *
 * @since 4.0
 */
public final class RPCGenerationCache {

  private final Cache<CacheKey, RPCInternalInstanceFactory> cachedGeneratedImplementation = Caffeine.newBuilder()
    .expireAfterAccess(Duration.ofHours(8))
    .build();

  private final DefaultRPCFactory rpcFactory;

  /**
   * Constructs a new rpc generation cache.
   *
   * @param rpcFactory the rpc factory that is associated with this generation cache.
   * @throws NullPointerException if the given rpc factory is null.
   */
  public RPCGenerationCache(@NonNull DefaultRPCFactory rpcFactory) {
    this.rpcFactory = rpcFactory;
  }

  /**
   * Gets an existing instance factory for the target class wrapped in the given target class metadata. The generation
   * process is triggered again if different generation flags are supplied for the same type.
   *
   * @param generationFlags the options to apply during class generation.
   * @param baseClassMeta   the class meta for the class that is being generated.
   * @return an instance factory for the generated rpc api implementation.
   * @throws NullPointerException if the given base class meta is null.
   */
  @NonNull
  RPCInternalInstanceFactory getOrGenerateImplementation(
    int generationFlags,
    @NonNull Class<?> senderTargetType,
    @NonNull RPCClassMetadata baseClassMeta
  ) {
    var sender = new DefaultRPCSender(
      senderTargetType,
      this.rpcFactory,
      this.rpcFactory.defaultObjectMapper(),
      this.rpcFactory.defaultDataBufFactory(),
      baseClassMeta,
      () -> null);
    return this.getOrGenerateImplementation(generationFlags, sender, baseClassMeta);
  }

  /**
   * Gets an existing instance factory for the target class wrapped in the given target class metadata. The generation
   * process is triggered again if different generation flags are supplied for the same type.
   *
   * @param generationFlags the options to apply during class generation.
   * @param rpcSender       the base rpc sender for the target class.
   * @param baseClassMeta   the class meta for the class that is being generated.
   * @return an instance factory for the generated rpc api implementation.
   * @throws NullPointerException if the given rpc sender or base class meta is null.
   */
  public @NonNull RPCInternalInstanceFactory getOrGenerateImplementation(
    int generationFlags,
    @NonNull RPCSender rpcSender,
    @NonNull RPCClassMetadata baseClassMeta
  ) {
    var generationTargetClass = baseClassMeta.targetClass();
    var generationTargetCacheKey = new CacheKey(generationFlags, generationTargetClass);
    var existingImplementation = this.cachedGeneratedImplementation.getIfPresent(generationTargetCacheKey);
    if (existingImplementation != null) {
      // implementation was already generated or generation is on the fly
      return existingImplementation;
    }

    // register a temp instance factory for the type being generated
    var tempInstanceFactory = new RPCInternalInstanceFactory.TempRPCInternalInstanceFactory();
    this.cachedGeneratedImplementation.put(generationTargetCacheKey, tempInstanceFactory);

    // generate the class
    var context = new RPCGenerationContext(this);
    var rpcGenerator = new RPCImplementationGenerator(context, baseClassMeta, generationFlags);
    var generatedClassLookup = rpcGenerator.generateImplementation();

    // construct the final instance factory
    var userArgCount = context.superclassConstructorDesc.parameterCount();
    var additionalInstanceFactories = context.additionalInstanceFactories().toArray(RPCInternalInstanceFactory[]::new);
    var instanceFactory = RPCInternalInstanceFactory.make(
      userArgCount,
      generatedClassLookup,
      () -> this.cachedGeneratedImplementation.getIfPresent(generationTargetCacheKey),
      rpcSender,
      additionalInstanceFactories);

    // associate the actual instance factory & delegate the temp factory
    this.cachedGeneratedImplementation.put(generationTargetCacheKey, instanceFactory);
    tempInstanceFactory.setDelegate(instanceFactory);

    return instanceFactory;
  }

  /**
   * A key for cache entries to distinct different ways of generating the same implementation.
   *
   * @param generationFlags the flags used during class generation.
   * @param targetClass     the target class that was implemented.
   * @since 4.0
   */
  private record CacheKey(int generationFlags, @NonNull Class<?> targetClass) {

  }
}
