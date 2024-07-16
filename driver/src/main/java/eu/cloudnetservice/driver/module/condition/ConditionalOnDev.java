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

package eu.cloudnetservice.driver.module.condition;

import eu.cloudnetservice.driver.module.condition.processors.ConditionalOnDevProcessor;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that, when applied to a module task or injectable method inside a module, will check that the current
 * environment is a development environment (as per system property {@code cloudnet.dev}). When that is the case, the
 * method will be left in the class without changes. However, if the check fails either the complete method is erased
 * from the class or the complete method body, depending on the presence of {@link KeepOnConditionFailure}.
 * <p>
 * This can for example be used to print additional information that is usually irrelevant to the user, but might come
 * very handy during the module development.
 * <p>
 * Note: while this annotation is retained at runtime the actual annotation will be dropped from the class during
 * introspection and cannot be resolved using, for example, {@code Method.getAnnotation}.
 *
 * @since 4.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@TargetConditionProcessor(ConditionalOnDevProcessor.class)
public @interface ConditionalOnDev {

}
