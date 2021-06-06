package de.dytanic.cloudnet.template;

import de.dytanic.cloudnet.driver.template.TemplateStorage;
import org.jetbrains.annotations.ApiStatus;

/**
 * @deprecated moved to the driver api, use {@link TemplateStorage} instead
 */
@Deprecated
@ApiStatus.ScheduledForRemoval(inVersion = "3.5")
public interface ITemplateStorage extends TemplateStorage {

}
