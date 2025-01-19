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

package eu.cloudnetservice.driver.impl.event;

import com.google.common.collect.Iterables;
import eu.cloudnetservice.driver.event.Event;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.InvocationOrder;
import eu.cloudnetservice.driver.event.events.service.CloudServiceLifecycleChangeEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DefaultEventManagerTest {

  @Test
  void testNullListenerRegistration() {
    EventManager eventManager = new DefaultEventManager();
    Assertions.assertThrows(NullPointerException.class, () -> eventManager.registerListener(null));
  }

  @Test
  @Order(0)
  void testListenerRegistration() {
    var eventManager = new DefaultEventManager();
    eventManager.registerListeners(TestListener.INSTANCE);

    Assertions.assertEquals(2, eventManager.listeners.size());

    Assertions.assertNotNull(eventManager.listeners.get(TestEvent.class));
    Assertions.assertEquals(3, eventManager.listeners.get(TestEvent.class).size());

    var iterator = eventManager.listeners.get(TestEvent.class).iterator();

    Assertions.assertEquals(InvocationOrder.EARLY, iterator.next().order());
    Assertions.assertEquals(InvocationOrder.EARLY, iterator.next().order());
    Assertions.assertEquals(InvocationOrder.LATE, iterator.next().order());

    Assertions.assertEquals(
      InvocationOrder.NORMAL,
      Iterables.getOnlyElement(eventManager.listeners.get(CloudServiceLifecycleChangeEvent.class)).order());

    Assertions.assertEquals("*",
      eventManager.listeners.get(TestEvent.class).iterator().next().channel());
    Assertions.assertEquals("123",
      eventManager.listeners.get(CloudServiceLifecycleChangeEvent.class).iterator().next().channel());
  }

  @Test
  @Order(10)
  void testEventCall() {
    EventManager eventManager = new DefaultEventManager();
    eventManager.registerListener(TestListener.INSTANCE);

    var event = new TestEvent(123);
    Assertions.assertSame(event, eventManager.callEvent(event));
    Assertions.assertEquals(5678, event.number);
  }

  @Test
  @Order(20)
  void testUnregisterListenerByInstance() {
    var eventManager = this.newEventManagerWithListener();
    eventManager.unregisterListener(TestListener.INSTANCE);

    Assertions.assertEquals(0, eventManager.listeners.size());
  }

  @Test
  @Order(30)
  void testUnregisterListenerByClassLoader() {
    var eventManager = this.newEventManagerWithListener();
    eventManager.unregisterListeners(TestListener.class.getClassLoader());

    Assertions.assertEquals(0, eventManager.listeners.size());
  }

  private DefaultEventManager newEventManagerWithListener() {
    var eventManager = new DefaultEventManager();

    eventManager.registerListener(TestListener.INSTANCE);
    Assertions.assertEquals(2, eventManager.listeners.size());

    return eventManager;
  }

  private static final class TestListener {

    private static final TestListener INSTANCE = new TestListener();

    @EventListener(order = InvocationOrder.LATE)
    public void listenerA(TestEvent event) {
      Assertions.assertEquals(2, event.counter);
      Assertions.assertEquals(4567, event.number);
      event.number = 5678;
    }

    @EventListener(order = InvocationOrder.EARLY)
    public void listenerB(TestEvent event) {
      Assertions.assertTrue(event.counter == 0 || event.counter == 1);
      event.counter++;
      event.number = 4567;
    }

    @EventListener(order = InvocationOrder.EARLY)
    public void listenerC(TestEvent event) {
      Assertions.assertTrue(event.counter == 0 || event.counter == 1);
      event.counter++;
    }

    @EventListener(channel = "123")
    private void listenerD(CloudServiceLifecycleChangeEvent event) {
    }

    @Override
    public boolean equals(Object obj) {
      return super.equals(obj) || (obj != null && obj.getClass() == this.getClass());
    }
  }

  private static final class TestEvent extends Event {

    private int number;
    private int counter;

    private TestEvent(int number) {
      this.number = number;
    }
  }
}
