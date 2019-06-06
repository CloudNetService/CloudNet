package de.dytanic.cloudnet.common.collection;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * This class can capture 2 references of 2 types and set or
 * clear the data using setFirst() / getFirst() and setSecond() / getSecond().
 * It can be used to return multiple objects of a method, or to
 * easily capture multiple objects without creating their own class.
 *
 * @param <F> the first type, which you want to defined
 * @param <S> the second type which you want to defined
 */
@ToString
@EqualsAndHashCode
public class Pair<F, S> {

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

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public Pair() {
    }

    public F getFirst() {
        return this.first;
    }

    public S getSecond() {
        return this.second;
    }

    public void setFirst(F first) {
        this.first = first;
    }

    public void setSecond(S second) {
        this.second = second;
    }

}