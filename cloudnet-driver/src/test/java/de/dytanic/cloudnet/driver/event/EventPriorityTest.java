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

package de.dytanic.cloudnet.driver.event;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

public class EventPriorityTest {

  @RepeatedTest(15)
  public void testEventPriorityComparator() {
    List<EventPriority> eventPriorities = Arrays.asList(EventPriority.values());

    Collections.shuffle(eventPriorities);
    Collections.sort(eventPriorities);

    for (int i = 0; i < EventPriority.values().length; i++) {
      Assertions.assertSame(EventPriority.values()[i], eventPriorities.get(i));
    }
  }
}
