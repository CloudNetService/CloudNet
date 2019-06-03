package de.dytanic.cloudnet.driver.event;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

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
        Assert.assertEquals(46, publisherCounter.get());

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

    /*= -------------------------------------------------------------------------------------- =*/

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

    private final class TestEvent extends Event {

        public String value;

        public TestEvent(String value) {
            this.value = value;
        }
    }

    /*= ----------------------------------------------------------------------------------- =*/

    private final class ListenerTest {

        @EventListener(channel = "set")
        public void onTestExecute(TestEvent testEvent) {
            Assert.assertEquals("Test_value1234", testEvent.value);

            testEvent.value = "test_result";
            publisherCounter.incrementAndGet();
        }

        @EventListener(channel = "test_channel")
        public void onTestExecute2(TestEvent testEvent) {
            testEvent.value = "test_channel_result";
        }
    }

    private final class ListenerTest2 {

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