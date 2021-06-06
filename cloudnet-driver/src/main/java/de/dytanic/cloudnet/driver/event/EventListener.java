package de.dytanic.cloudnet.driver.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventListener {

  String channel() default "*";

  EventPriority priority() default EventPriority.NORMAL;
}
