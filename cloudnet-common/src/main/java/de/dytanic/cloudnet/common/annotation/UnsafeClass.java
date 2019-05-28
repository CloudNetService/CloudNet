package de.dytanic.cloudnet.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a class, its methods, and members can change for each version.
 * The methods and fields are mostly for internal use only and can influence
 * other elements if they change
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface UnsafeClass {

}