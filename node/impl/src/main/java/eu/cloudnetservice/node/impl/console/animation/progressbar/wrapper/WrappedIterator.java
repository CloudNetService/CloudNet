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

package eu.cloudnetservice.node.impl.console.animation.progressbar.wrapper;

import eu.cloudnetservice.node.impl.console.Console;
import eu.cloudnetservice.node.impl.console.animation.progressbar.ConsoleProgressAnimation;
import java.util.Iterator;
import lombok.NonNull;

public final class WrappedIterator<T> implements Iterator<T> {

  private final Iterator<T> wrapped;
  private final ConsoleProgressAnimation animation;

  public WrappedIterator(
    @NonNull Iterator<T> wrapped,
    @NonNull Console console,
    @NonNull ConsoleProgressAnimation animation
  ) {
    this.wrapped = wrapped;
    this.animation = animation;

    console.startAnimation(animation);
  }

  @Override
  public boolean hasNext() {
    var hasNext = this.wrapped.hasNext();
    // close the animation if there are no more elements
    if (!hasNext) {
      this.animation.stepToEnd();
    }
    return hasNext;
  }

  @Override
  public T next() {
    var next = this.wrapped.next();
    this.animation.step();
    return next;
  }

  @Override
  public void remove() {
    this.wrapped.remove();
  }
}
