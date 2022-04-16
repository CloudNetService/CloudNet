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

package eu.cloudnetservice.driver.network.rpc.generation;

import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.network.rpc.defaults.generation.ChainedApiImplementationGenerator;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ChainTest {

  @Test
  public void t() {
    ChainedApiImplementationGenerator.generateApiImplementation(
      Function.class,
      Mockito.mock(RPCSender.class),
      GenerationContext.forClass(Test123.class).build(),
      Mockito.mock(RPCSender.class));
  }

  public abstract static class Test123 {

    private final String name;
    private final int google;
    private final String kek;

    public Test123(String name, int google, String kek) {
      this.name = name;
      this.google = google;
      this.kek = kek;
    }

    public abstract void test();

    public abstract String bla();

    public abstract Task<String> foo();
  }
}
