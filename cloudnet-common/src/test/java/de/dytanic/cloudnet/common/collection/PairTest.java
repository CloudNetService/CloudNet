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

package de.dytanic.cloudnet.common.collection;

import org.junit.Assert;
import org.junit.Test;

public final class PairTest {

  @Test
  public void testPair() {
    Pair<String, Integer> pair = new Pair<>();

    Assert.assertNull(pair.getFirst());
    Assert.assertNull(pair.getSecond());

    pair.setFirst("Test");
    pair.setSecond(5);

    Assert.assertNotNull(pair.getFirst());
    Assert.assertNotNull(pair.getSecond());

    Assert.assertEquals("Test", pair.getFirst());
    Assert.assertEquals(5, pair.getSecond().intValue());

    pair = new Pair<>("foobar", 44);

    Assert.assertNotNull(pair.getFirst());
    Assert.assertNotNull(pair.getSecond());

    Assert.assertEquals("foobar", pair.getFirst());
    Assert.assertEquals(44, pair.getSecond().intValue());
  }
}
