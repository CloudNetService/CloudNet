package de.dytanic.cloudnet.driver.service.property;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface ServiceProperty<T> {

    @NotNull
    Optional<T> get(@NotNull ServiceInfoSnapshot serviceInfoSnapshot);

    void set(@NotNull ServiceInfoSnapshot serviceInfoSnapshot, @Nullable T value);

}
