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

import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStartEvent;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;

@TestMethodOrder(OrderAnnotation.class)
public class DefaultEventManagerTest {

  @Test
  void testNullListenerRegistration() {
    IEventManager eventManager = new DefaultEventManager();
    Assertions.assertThrows(NullPointerException.class, () -> eventManager.registerListener(null));
  }

  @Test
  @Order(0)
  void testListenerRegistration() {
    DefaultEventManager eventManager = new DefaultEventManager();
    eventManager.registerListeners(new TestListener());

    Assertions.assertEquals(2, eventManager.registeredListeners.size());

    Assertions.assertNotNull(eventManager.registeredListeners.get("123"));
    Assertions.assertEquals(1, eventManager.registeredListeners.get("123").size());

    Assertions.assertNotNull(eventManager.registeredListeners.get("*"));
    Assertions.assertEquals(1, eventManager.registeredListeners.get("*").size());

    Assertions.assertEquals(EventPriority.HIGH, eventManager.registeredListeners.get("*").get(0).getPriority());
    Assertions.assertEquals(EventPriority.NORMAL, eventManager.registeredListeners.get("123").get(0).getPriority());

    Assertions.assertEquals("listenerA", eventManager.registeredListeners.get("*").get(0).getMethodName());
    Assertions.assertEquals("listenerB", eventManager.registeredListeners.get("123").get(0).getMethodName());

    Assertions.assertEquals(ChannelMessageReceiveEvent.class,
      eventManager.registeredListeners.get("*").get(0).getEventClass());
    Assertions.assertEquals(CloudServiceStartEvent.class,
      eventManager.registeredListeners.get("123").get(0).getEventClass());
  }

  @Test
  @Order(10)
  void testEventCall() {
    IEventManager eventManager = new DefaultEventManager();
    eventManager.registerListener(new TestListener());

    ChannelMessage channelMessage = Mockito.mock(ChannelMessage.class);
    Mockito.when(channelMessage.getChannel()).thenReturn("passed");

    ChannelMessageReceiveEvent event = new ChannelMessageReceiveEvent(channelMessage, true);

    Assertions.assertSame(event, eventManager.callEvent(event));

    Assertions.assertNotNull(event.getQueryResponse());
    Assertions.assertEquals("abc", event.getQueryResponse().getChannel());

    Assertions.assertNotNull(event.getQueryResponse().getContent());
    Assertions.assertEquals("passed", event.getQueryResponse().getContent().readString());
  }

  @Test
  @Order(20)
  void testUnregisterListenerByInstance() {
    DefaultEventManager eventManager = this.newEventManagerWithListener();
    eventManager.unregisterListener(new TestListener());

    Assertions.assertEquals(0, eventManager.registeredListeners.size());
  }

  @Test
  @Order(30)
  void testUnregisterListenerByClass() {
    DefaultEventManager eventManager = this.newEventManagerWithListener();
    eventManager.unregisterListener(TestListener.class);

    Assertions.assertEquals(0, eventManager.registeredListeners.size());
  }

  @Test
  @Order(40)
  void testUnregisterListenerByClassLoader() {
    DefaultEventManager eventManager = this.newEventManagerWithListener();
    eventManager.unregisterListeners(TestListener.class.getClassLoader());

    Assertions.assertEquals(0, eventManager.registeredListeners.size());
  }

  private DefaultEventManager newEventManagerWithListener() {
    DefaultEventManager eventManager = new DefaultEventManager();

    eventManager.registerListener(new TestListener());
    Assertions.assertEquals(2, eventManager.registeredListeners.size());

    return eventManager;
  }

  public static final class TestListener {

    @EventListener(priority = EventPriority.HIGH)
    public void listenerA(ChannelMessageReceiveEvent event) {
      event.setQueryResponse(ChannelMessage.builder(null)
        .channel("abc")
        .buffer(DataBuf.empty().writeString(event.getChannel()))
        .build());
    }

    @EventListener(channel = "123")
    public void listenerB(CloudServiceStartEvent event) {
    }

    @Override
    public boolean equals(Object obj) {
      return super.equals(obj) || (obj != null && obj.getClass() == this.getClass());
    }
  }
}
