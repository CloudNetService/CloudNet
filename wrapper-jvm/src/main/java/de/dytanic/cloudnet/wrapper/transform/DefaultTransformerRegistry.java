/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.wrapper.transform;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.function.Predicate;
import lombok.NonNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public final class DefaultTransformerRegistry implements TransformerRegistry {

  private final Instrumentation instrumentation;

  public DefaultTransformerRegistry(@NonNull Instrumentation instrumentation) {
    this.instrumentation = instrumentation;
  }

  @Override
  public void registerTransformer(
    @NonNull String packagePrefix,
    @NonNull String classname,
    @NonNull Transformer transformer
  ) {
    this.registerTransformer(name -> {
      // check for the package
      if (!name.startsWith(packagePrefix)) {
        return false;
      }
      // check the class name
      var lastSlash = name.lastIndexOf('/');
      if (lastSlash != -1 && name.length() > lastSlash) {
        var simpleName = name.substring(lastSlash + 1);
        return classname.equals(simpleName);
      }
      // nope
      return false;
    }, transformer);
  }

  @Override
  public void registerTransformer(@NonNull Predicate<String> filter, @NonNull Transformer transformer) {
    this.instrumentation.addTransformer(new FilteringTransformer(filter, transformer, this.instrumentation));
  }

  private record FilteringTransformer(
    @NonNull Predicate<String> predicate,
    @NonNull Transformer transformer,
    @NonNull Instrumentation inst
  ) implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader $, String className, Class<?> clazz, ProtectionDomain $1, byte[] file) {
      // do not handle re-transformations
      if (clazz == null && this.predicate.test(className)) {
        // read the class
        var node = new ClassNode();
        var reader = new ClassReader(file);
        reader.accept(node, 0);

        // call the transformer
        this.transformer.transform(className, node);
        // remove this transformer
        this.inst.removeTransformer(this);
        // re-write the class
        var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        node.accept(writer);
        return writer.toByteArray();
      }
      // no transformation
      return null;
    }
  }
}
