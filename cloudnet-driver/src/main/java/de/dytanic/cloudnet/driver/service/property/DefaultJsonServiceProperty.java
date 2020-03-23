package de.dytanic.cloudnet.driver.service.property;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Optional;

public class DefaultJsonServiceProperty<T> implements ServiceProperty<T> {

    private String key;
    private Type type;
    private Class<T> tClass;

    private boolean allowModifications = true;

    private DefaultJsonServiceProperty(String key, Type type, Class<T> tClass) {
        this.key = key;
        this.type = type;
        this.tClass = tClass;
    }

    @NotNull
    public static <T> DefaultJsonServiceProperty<T> createFromClass(@NotNull String key, @NotNull Class<T> tClass) {
        return new DefaultJsonServiceProperty<>(key, null, tClass);
    }

    @NotNull
    public static <T> DefaultJsonServiceProperty<T> createFromType(@NotNull String key, @NotNull Type type) {
        return createFromType(key, type, false);
    }

    @NotNull
    public static <T> DefaultJsonServiceProperty<T> createFromType(@NotNull String key, @NotNull Type type, boolean forbidModifications) {
        DefaultJsonServiceProperty<T> property = new DefaultJsonServiceProperty<>(key, type, null);
        if (forbidModifications) {
            property.forbidModification();
        }
        return property;
    }

    public DefaultJsonServiceProperty<T> forbidModification() {
        this.allowModifications = false;
        return this;
    }

    @NotNull
    @Override
    public Optional<T> get(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
        if (!serviceInfoSnapshot.getProperties().contains(this.key)) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.type != null ? serviceInfoSnapshot.getProperties().get(this.key, this.type) : serviceInfoSnapshot.getProperties().get(this.key, this.tClass));
    }

    @Override
    public void set(@NotNull ServiceInfoSnapshot serviceInfoSnapshot, T value) {
        Preconditions.checkArgument(this.allowModifications, "This property doesn't support modifying the value");
        serviceInfoSnapshot.getProperties().append(this.key, value);
    }
}
