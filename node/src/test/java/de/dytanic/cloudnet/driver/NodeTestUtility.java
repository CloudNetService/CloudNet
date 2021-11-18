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

package de.dytanic.cloudnet.driver;

import de.dytanic.cloudnet.CloudNet;
import org.mockito.Mockito;

public final class NodeTestUtility {

  /**
   * Mocks the {@link CloudNet} class and sets the instance of the CloudNet driver to the mocked one.
   *
   * @return the mocked node instance.
   */
  public static CloudNet mockAndSetDriverInstance() {
    CloudNet driver = Mockito.mock(CloudNet.class);
    CloudNetDriver.setInstance(driver);

    return driver;
  }

}
