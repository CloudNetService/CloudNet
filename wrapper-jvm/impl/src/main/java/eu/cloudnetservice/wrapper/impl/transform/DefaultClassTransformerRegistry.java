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

package eu.cloudnetservice.wrapper.impl.transform;

import eu.cloudnetservice.wrapper.transform.ClassTransformer;
import eu.cloudnetservice.wrapper.transform.ClassTransformerRegistry;
import jakarta.inject.Singleton;
import java.lang.classfile.ClassFile;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of a class transformer registry.
 *
 * @since 4.0
 */
// Note: this class (or rather the superinterface) is available for injection, but isn't instantiated with
// injection to prevent leaking the instrumentation instance (Premain holds the instance but is hidden as well)
@Singleton
public final class DefaultClassTransformerRegistry implements ClassTransformerRegistry {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultClassTransformerRegistry.class);

  private final Instrumentation instrumentation;

  /**
   * Constructs a new default transformer registry using the given instrumentation.
   *
   * @param instrumentation the instrumentation to use to register class transformers.
   * @throws NullPointerException if the given instrumentation instance is null.
   */
  public DefaultClassTransformerRegistry(@NonNull Instrumentation instrumentation) {
    this.instrumentation = instrumentation;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerTransformer(@NonNull ClassTransformer transformer) {
    var transformerToRegister = new RegisteredClassTransformer(transformer, this.instrumentation);
    this.instrumentation.addTransformer(transformerToRegister, false);
  }

  /**
   * An implementation of a class file transformer that parses and transforms class files using the given CloudNet
   * transformer. The transform call is ignored if the given transformer is rejecting the class, and this transformer
   * will be unregistered from the given instrumentation if the acceptance check returns
   * {@link ClassTransformer.TransformWillingness#ACCEPT_ONCE}.
   *
   * @param transformer     the transformer to obtain the class file transform instance from for accepted classes.
   * @param instrumentation the instrumentation to which this transformer is registered.
   * @since 4.0
   */
  private record RegisteredClassTransformer(
    @NonNull ClassTransformer transformer,
    @NonNull Instrumentation instrumentation
  ) implements ClassFileTransformer {

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] transform(
      @Nullable ClassLoader loader,
      @NonNull String className,
      @Nullable Class<?> classBeingRedefined,
      @Nullable ProtectionDomain protectionDomain,
      byte[] classfileBuffer
    ) {
      // ignore classes that are being redefined or re-transformed, usually
      // these calls should not happen as this transformer is not registered
      // to support re-transformation of classes anyway
      if (classBeingRedefined != null) {
        return null;
      }

      // check if the managed transformer has the intention to change the given class,
      // do nothing if that is not the case
      var transformWillingness = this.transformer.classTransformWillingness(className);
      if (transformWillingness == ClassTransformer.TransformWillingness.REJECT) {
        return null;
      }

      // unregister this transformer from the instrumentation to prevent calling it
      // again before actually starting the transformation process
      if (transformWillingness == ClassTransformer.TransformWillingness.ACCEPT_ONCE) {
        this.instrumentation.removeTransformer(this);
      }

      var transformerClassName = this.transformer.getClass().getName();
      LOGGER.debug("Transforming class {} using transformer {}", className, transformerClassName);

      try {
        // apply the transformation to the provided class file
        var classFile = ClassFile.of();
        var classModel = classFile.parse(classfileBuffer);
        var classTransform = this.transformer.provideClassTransform();
        return classFile.transform(classModel, classTransform);
      } catch (Exception exception) {
        LOGGER.error("Failed to transform class {} using transformer {}", className, transformerClassName, exception);
        return null;
      }
    }
  }
}
