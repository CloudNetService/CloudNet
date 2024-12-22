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

package eu.cloudnetservice.node.impl.util;

import eu.cloudnetservice.driver.base.Named;
import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class WildcardUtilTest {

  private static final String VALID_PATTERN = "Lobby-56*";
  private static final Collection<? extends Named> VALID_NAMEABLES = Arrays.asList(
    new NamedThing("Lobby-1"),
    new NamedThing("Lobby-56"),
    new NamedThing("Lobby-567"),
    new NamedThing("lOBBY-5636"),
    new NamedThing("Lobby-789")
  );

  private static final String INVALID_PATTERN = "Lobby-)5{6*([}";
  private static final Collection<? extends Named> INVALID_NAMEABLES = Arrays.asList(
    new NamedThing("Lobby-1"),
    new NamedThing("Lobby-)56("),
    new NamedThing("Lobby-(567"),
    new NamedThing("Lobby-(789"),
    new NamedThing("lOBBY-)5636("),
    new NamedThing("Lobby-)5{6*([}"),
    new NamedThing("Lobby-)5{6Hello([}"),
    new NamedThing("lobby-)5{6World([}"),
    new NamedThing("lobBY-)5{66789]([}"),
    new NamedThing("loBBy-)5{6World([}")
  );

  @Test
  public void testValidSuppliedPattern() {
    Assertions.assertNotNull(WildcardUtil.fixPattern(VALID_PATTERN, true));

    Assertions.assertTrue(WildcardUtil.anyMatch(VALID_NAMEABLES, VALID_PATTERN));
    Assertions.assertTrue(WildcardUtil.anyMatch(VALID_NAMEABLES, VALID_PATTERN, false));

    Assertions.assertEquals(2, WildcardUtil.filterWildcard(VALID_NAMEABLES, VALID_PATTERN).size());
    Assertions.assertEquals(3, WildcardUtil.filterWildcard(VALID_NAMEABLES, VALID_PATTERN, false).size());
  }

  @Test
  public void testInvalidSuppliedPattern() {
    Assertions.assertNotNull(WildcardUtil.fixPattern(INVALID_PATTERN, true));

    Assertions.assertTrue(WildcardUtil.anyMatch(INVALID_NAMEABLES, INVALID_PATTERN));
    Assertions.assertTrue(WildcardUtil.anyMatch(INVALID_NAMEABLES, INVALID_PATTERN, false));

    Assertions.assertEquals(2, WildcardUtil.filterWildcard(INVALID_NAMEABLES, INVALID_PATTERN).size());
    Assertions.assertEquals(5, WildcardUtil.filterWildcard(INVALID_NAMEABLES, INVALID_PATTERN, false).size());
  }

  private record NamedThing(String name) implements Named {

  }
}
