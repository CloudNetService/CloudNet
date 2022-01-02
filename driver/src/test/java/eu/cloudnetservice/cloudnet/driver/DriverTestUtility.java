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

package eu.cloudnetservice.cloudnet.driver;

import org.mockito.Mockito;

/**
 * A utility class for testing code that is driver method dependant.
 */
public final class DriverTestUtility {

  /**
   * Mocks the {@link CloudNetDriver} class and sets the instance of the CloudNet driver to the mocked one.
   *
   * @return the mocked driver instance.
   */
  public static CloudNetDriver mockAndSetDriverInstance() {
    var driver = Mockito.mock(CloudNetDriver.class);
    CloudNetDriver.instance(driver);

    return driver;
  }
}
