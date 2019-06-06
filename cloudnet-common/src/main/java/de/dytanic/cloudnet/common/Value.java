package de.dytanic.cloudnet.common;

/**
 * A class that provides the reference of an object.
 * It is interesting for asynchronous operations or anonymous classes.
 *
 * @param <T> the type of value, that you want to set
 */
public class Value<T> {

    /**
     * The reference of an object, which is held and retrievable by an instance of this class.
     * It is protected to allow inherit
     */
    protected volatile T value;

    public Value(T value) {
        this.value = value;
    }

    public Value() {
    }

    public T getValue() {
        return this.value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Value)) return false;
        final Value<?> other = (Value<?>) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$value = this.getValue();
        final Object other$value = other.getValue();
        if (this$value == null ? other$value != null : !this$value.equals(other$value)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Value;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $value = this.getValue();
        result = result * PRIME + ($value == null ? 43 : $value.hashCode());
        return result;
    }

    public String toString() {
        return "Value(value=" + this.getValue() + ")";
    }
}