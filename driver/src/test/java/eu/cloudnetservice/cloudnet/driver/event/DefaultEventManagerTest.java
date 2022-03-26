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

package eu.cloudnetservice.cloudnet.driver.event;

import eu.cloudnetservice.cloudnet.driver.DriverEnvironment;
import eu.cloudnetservice.cloudnet.driver.DriverTestUtil;
import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessage;
import eu.cloudnetservice.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.cloudnet.driver.event.events.service.CloudServiceLifecycleChangeEvent;
import eu.cloudnetservice.cloudnet.driver.network.NetworkChannel;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;

@TestMethodOrder(OrderAnnotation.class)
public class DefaultEventManagerTest {

  @BeforeAll
  public static void initDriver() {
    var driver = DriverTestUtil.mockAndSetDriverInstance();
    Mockito.when(driver.componentName()).thenReturn("Node-1");
    Mockito.when(driver.environment()).thenReturn(DriverEnvironment.NODE);
  }

  @Test
  void testNullListenerRegistration() {
    EventManager eventManager = new DefaultEventManager();
    Assertions.assertThrows(NullPointerException.class, () -> eventManager.registerListener(null));
  }

  @Test
  @Order(0)
  void testListenerRegistration() {
    var eventManager = new DefaultEventManager();
    eventManager.registerListeners(new TestListener());

    Assertions.assertEquals(2, eventManager.listeners.size());

    Assertions.assertNotNull(eventManager.listeners.get(ChannelMessageReceiveEvent.class));
    Assertions.assertEquals(1, eventManager.listeners.get(ChannelMessageReceiveEvent.class).size());

    Assertions.assertEquals(
      EventPriority.HIGH,
      eventManager.listeners.get(ChannelMessageReceiveEvent.class).iterator().next().priority());
    Assertions.assertEquals(
      EventPriority.NORMAL,
      eventManager.listeners.get(CloudServiceLifecycleChangeEvent.class).iterator().next().priority());

    Assertions.assertEquals("*",
      eventManager.listeners.get(ChannelMessageReceiveEvent.class).iterator().next().channel());
    Assertions.assertEquals("123",
      eventManager.listeners.get(CloudServiceLifecycleChangeEvent.class).iterator().next().channel());
  }

  @Test
  @Order(10)
  @Disabled("https://github.com/raphw/byte-buddy/issues/1175 & https://gitlab.ow2.org/asm/asm/-/issues/317959")
  void testEventCall() {
    EventManager eventManager = new DefaultEventManager();
    eventManager.registerListener(new TestListener());

    var channelMessage = Mockito.mock(ChannelMessage.class);
    Mockito.when(channelMessage.channel()).thenReturn("passed");

    var event = new ChannelMessageReceiveEvent(
      channelMessage,
      Mockito.mock(NetworkChannel.class),
      true);

    Assertions.assertSame(event, eventManager.callEvent(event));

    Assertions.assertNotNull(event.queryResponse());
    Assertions.assertEquals("abc", event.queryResponse().join().channel());

    Assertions.assertNotNull(event.queryResponse().join().content());
    Assertions.assertEquals("passed", event.queryResponse().join().content().readString());
  }

  @Test
  @Order(20)
  void testUnregisterListenerByInstance() {
    var eventManager = this.newEventManagerWithListener();
    eventManager.unregisterListener(new TestListener());

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

    eventManager.registerListener(new TestListener());
    Assertions.assertEquals(2, eventManager.listeners.size());

    return eventManager;
  }

  private static final class TestListener {

    @EventListener(priority = EventPriority.HIGH)
    public void listenerA(ChannelMessageReceiveEvent event) {
      event.queryResponse(ChannelMessage.builder()
        .channel("abc")
        .targetAll()
        .buffer(DataBuf.empty().writeString(event.channel()))
        .build());
    }

    @EventListener(channel = "123")
    private void listenerB(CloudServiceLifecycleChangeEvent event) {
    }

    @Override
    public boolean equals(Object obj) {
      return super.equals(obj) || (obj != null && obj.getClass() == this.getClass());
    }
  }
}
