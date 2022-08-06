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

package eu.cloudnetservice.driver.util.define;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.time.Duration;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A class definer which defines classes using a class loader. This is a fallback method which will not work as expected
 * on modern jvm implementations which have higher access check requirements. In normal cases this definer should never
 * get used as the {@link LookupClassDefiner} should define classes.
 *
 * @since 4.0
 */
@ApiStatus.Internal
final class FallbackClassDefiner implements ClassDefiner {

  /**
   * The cached defining class loaders for each class loader of the parent classes to define the class in.
   */
  private final LoadingCache<ClassLoader, DefiningClassLoader> cache = Caffeine.newBuilder()
    .expireAfterAccess(Duration.ofMinutes(30))
    .build(DefiningClassLoader::new);

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Class<?> defineClass(@NonNull String name, @NonNull Class<?> parent, byte[] bytecode) {
    return this.cache.get(parent.getClassLoader()).defineClass(bytecode);
  }

  /**
   * A class loader which gives access to the normally protected defineClass method.
   *
   * @since 1.0
   */
  private static final class DefiningClassLoader extends ClassLoader {

    /**
     * Creates a new defining class loader for the parent class loader of the holding class.
     *
     * @param parent the parent class loader for delegation.
     */
    public DefiningClassLoader(@Nullable ClassLoader parent) {
      super(parent);
    }

    /**
     * An exposed method which allows converting the given bytecode into an instance of class delegating the call to
     * {@link ClassLoader#defineClass(String, byte[], int, int)}.
     *
     * @param byteCode the bytecode of the class to define.
     * @return the constructed class object from the given bytecode.
     */
    public @NonNull Class<?> defineClass(byte[] byteCode) {
      return super.defineClass(null, byteCode, 0, byteCode.length, null);
    }
  }
}
