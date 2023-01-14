/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.common.log;

import java.util.logging.Handler;

/**
 * An abstract handler implementation which only requires the extending class to overwrite {@code publish} instead of
 * implementing the (mostly) unused methods flush and close as well.
 *
 * @since 4.0
 */
public abstract class AbstractHandler extends Handler {

  /**
   * {@inheritDoc}
   */
  @Override
  public void flush() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws SecurityException {
  }
}
