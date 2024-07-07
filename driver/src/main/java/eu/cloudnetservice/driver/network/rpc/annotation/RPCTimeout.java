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

package eu.cloudnetservice.driver.network.rpc.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import org.jetbrains.annotations.Range;

/**
 * Defines the timeout to apply to an RPC execution, either for an entire class or a specific RPC method. If the
 * annotation is not explicitly provided, a default timeout is applied to all RPCs in a class. Note: if a method is
 * inherited, the class-level rpc timeout is inherited from the root class that was introspected.
 *
 * @since 4.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RPCTimeout {

  /**
   * Get the timeout to apply to the RPCs executed in a class or to the specific RPC executed by a method. The unit of
   * the timeout can be configured using {@link #unit()} which defaults to seconds. If an RPC method is annotated with
   * this annotation it has more relevance than the annotation placed at the top-level class.
   * <p>
   * The provided timeout must be positive. If the timeout is {@code 0} then no timeout is applied to the method, and it
   * can execute without any constraints time wise.
   *
   * @return the timeout to apply to the RPCs in a class or to the specific RPC executed by a method.
   */
  @Range(from = 0, to = Long.MAX_VALUE)
  long timeout();

  /**
   * Get the time unit in which the RPC timeout provided by {@link #timeout()} is measured. Defaults to seconds.
   *
   * @return the unit in which the given timeout is given.
   */
  @NonNull
  TimeUnit unit() default TimeUnit.SECONDS;
}
