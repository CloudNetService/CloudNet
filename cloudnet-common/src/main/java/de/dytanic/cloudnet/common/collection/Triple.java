package de.dytanic.cloudnet.common.collection;

/**
 * This class can capture 3 references of 3 types and set or
 * clear the data using setFirst() / getFirst() and setSecond() / getSecond().
 * It can be used to return multiple objects of a method, or to
 * easily capture multiple objects without creating their own class.
 *
 * @param <F> the first type, which you want to defined
 * @param <S> the second type which you want to defined
 * @param <T> the third type which you want to defined
 */
public class Triple<F, S, T> {

    /**
     * The reference of the first value and the type of F
     *
     * @see F
     */
    protected F first;

    /**
     * The reference of the second value and the type of S
     *
     * @see S
     */
    protected S second;

    /**
     * The reference of the third value and the type of T
     *
     * @see T
     */
    protected T third;

    public Triple(F first, S second, T third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public Triple() {
    }

    public F getFirst() {
        return this.first;
    }

    public S getSecond() {
        return this.second;
    }

    public T getThird() {
        return this.third;
    }

    public void setFirst(F first) {
        this.first = first;
    }

    public void setSecond(S second) {
        this.second = second;
    }

    public void setThird(T third) {
        this.third = third;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Triple)) return false;
        final Triple<?, ?, ?> other = (Triple<?, ?, ?>) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$first = this.getFirst();
        final Object other$first = other.getFirst();
        if (this$first == null ? other$first != null : !this$first.equals(other$first)) return false;
        final Object this$second = this.getSecond();
        final Object other$second = other.getSecond();
        if (this$second == null ? other$second != null : !this$second.equals(other$second)) return false;
        final Object this$third = this.getThird();
        final Object other$third = other.getThird();
        if (this$third == null ? other$third != null : !this$third.equals(other$third)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Triple;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $first = this.getFirst();
        result = result * PRIME + ($first == null ? 43 : $first.hashCode());
        final Object $second = this.getSecond();
        result = result * PRIME + ($second == null ? 43 : $second.hashCode());
        final Object $third = this.getThird();
        result = result * PRIME + ($third == null ? 43 : $third.hashCode());
        return result;
    }

    public String toString() {
        return "Triple(first=" + this.getFirst() + ", second=" + this.getSecond() + ", third=" + this.getThird() + ")";
    }
}