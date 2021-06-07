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

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;

public final class DefaultEventManagerTest {

  private final AtomicInteger publisherCounter = new AtomicInteger(45);

  @Test
  public void testListenerCall() throws Throwable {
    IEventManager eventManager = new DefaultEventManager();

    Assert.assertNotNull(eventManager.registerListener(new ListenerTest()));

    TestEvent testEvent = new TestEvent("Test_value1234");

    Assert.assertNotNull(eventManager.callEvent("set", testEvent));
    Assert.assertEquals("test_result", testEvent.value);

    testEvent.value = "Test_value1234";

    Assert.assertNotNull(eventManager.unregisterListener(ListenerTest.class));
    Assert.assertNotNull(eventManager.callEvent("set", testEvent));
    Assert.assertEquals(46, this.publisherCounter.get());

    Assert.assertEquals("Test_value1234", testEvent.value);

    ListenerTest listenerTest = new ListenerTest();
    Assert.assertNotNull(eventManager.registerListener(listenerTest));
    Assert.assertNotNull(eventManager.unregisterListener(listenerTest));
    Assert.assertNotNull(eventManager.callEvent("set", testEvent));

    Assert.assertEquals("Test_value1234", testEvent.value);
    Assert.assertNotNull(eventManager.registerListener(listenerTest));
    Assert.assertNotNull(eventManager.callEvent("test_channel", testEvent));

    Assert.assertEquals("test_channel_result", testEvent.value);
    eventManager.unregisterAll();
  }

  @Test
  public void testEventPriority() {
    IEventManager eventManager = new DefaultEventManager();

    Assert.assertNotNull(eventManager.registerListener(new ListenerTest2()));

    TestEvent testEvent = new TestEvent("value_123");

    Assert.assertNotNull(eventManager.callEvent("test_channel_2", testEvent));
    Assert.assertEquals("value_456", testEvent.value);

    Assert.assertNotNull(eventManager.callEvent("test_channel_3", testEvent));
    Assert.assertEquals("value_789", testEvent.value);

    testEvent.value = "value_123";
    Assert.assertNotNull(eventManager.callEvent(testEvent));
    Assert.assertEquals("value_789", testEvent.value);
  }

  public static final class TestEvent extends Event {

    public String value;

    public TestEvent(String value) {
      this.value = value;
    }
  }


  public final class ListenerTest {

    @EventListener(channel = "set")
    public void onTestExecute(TestEvent testEvent) {
      Assert.assertEquals("Test_value1234", testEvent.value);

      testEvent.value = "test_result";
      DefaultEventManagerTest.this.publisherCounter.incrementAndGet();
    }

    @EventListener(channel = "test_channel")
    public void onTestExecute2(TestEvent testEvent) {
      testEvent.value = "test_channel_result";
    }
  }

  public final class ListenerTest2 {

    @EventListener(channel = "test_channel_2", priority = EventPriority.HIGHEST)
    public void onTestExecute(TestEvent testEvent) {
      Assert.assertEquals("value_123", testEvent.value);

      testEvent.value = "value_456";
    }

    @EventListener(channel = "test_channel_3", priority = EventPriority.LOWEST)
    public void onTestExecute2(TestEvent testEvent) {
      Assert.assertEquals("value_456", testEvent.value);

      testEvent.value = "value_789";
    }
  }
}
