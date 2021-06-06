package de.dytanic.cloudnet.driver.service.property;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class helps getting/modifying properties in {@link ServiceInfoSnapshot}s.
 *
 * @param <T> the type of the value for this property
 */
public interface ServiceProperty<T> {

  /**
   * Gets a property out of the properties of the given {@link ServiceInfoSnapshot}.
   *
   * @param serviceInfoSnapshot the serviceInfoSnapshot to get the property
   * @return an optional with the value, might be empty
   */
  @NotNull
  Optional<T> get(@NotNull ServiceInfoSnapshot serviceInfoSnapshot);

  /**
   * Sets a property into the properties of the given {@link ServiceInfoSnapshot}. Some properties might not support
   * this. An update of the service might be necessary.
   *
   * @param serviceInfoSnapshot the serviceInfoSnapshot to modify the property in
   * @param value               the value to set or null to remove the property
   */
  void set(@NotNull ServiceInfoSnapshot serviceInfoSnapshot, @Nullable T value);

}
