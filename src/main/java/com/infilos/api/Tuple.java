package com.infilos.api;

public class Tuple<A, B> {
    public final A first;
    public final B second;

    Tuple(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public A item1() {
        return first;
    }

    public B item2() {
        return second;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean equals(Object o) {
        if (this==o) {
            return true;
        }
        if (o==null || getClass()!=o.getClass()) {
            return false;
        }

        Tuple triplet = (Tuple) o;

        return first.equals(triplet.first) &&
            second.equals(triplet.second);
    }

    @Override
    public int hashCode() {
        int result = first.hashCode();
        result = 31 * result + second.hashCode();

        return result;
    }

    @Override
    public String toString() {
        return "Tuple(" + first + ", " + second + ")";
    }

    public static <A, B> Tuple<A, B> of(A a, B b) {
        return new Tuple<>(a, b);
    }
}
