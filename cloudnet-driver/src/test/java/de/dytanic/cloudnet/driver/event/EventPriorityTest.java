package de.dytanic.cloudnet.driver.event;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class EventPriorityTest {

  @Test
  public void testEventPriorityComparator() {
    List<EventPriority> eventPriorities = Arrays.asList(
      EventPriority.HIGH,
      EventPriority.LOWEST,
      EventPriority.LOW,
      EventPriority.NORMAL,
      EventPriority.HIGHEST
    );

    Collections.sort(eventPriorities);

    Assert.assertEquals(EventPriority.HIGHEST, eventPriorities.get(0));
    Assert.assertEquals(EventPriority.HIGH, eventPriorities.get(1));
    Assert.assertEquals(EventPriority.NORMAL, eventPriorities.get(2));
    Assert.assertEquals(EventPriority.LOW, eventPriorities.get(3));
    Assert.assertEquals(EventPriority.LOWEST, eventPriorities.get(4));
  }
}
