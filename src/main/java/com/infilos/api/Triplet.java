package com.infilos.api;

public class Triplet<A,B,C> {
    public final A first;
    public final B second;
    public final C third;

    Triplet(A first, B second, C third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public A item1() {
        return first;
    }

    public B item2() {
        return second;
    }

    public C item3() {
        return third;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Triplet triplet = (Triplet) o;

        return first.equals(triplet.first) &&
            second.equals(triplet.second) &&
            third.equals(triplet.third);
    }

    @Override
    public int hashCode() {
        int result = first.hashCode();
        result = 31 * result + second.hashCode();
        result = 31 * result + third.hashCode();

        return result;
    }

    @Override
    public String toString() {
        return "Triplet(" + first + ", " + second + ", " + third + ")";
    }

    public static <A, B, C> Triplet <A, B, C> of(A a, B b, C c) {
        return new Triplet <>(a, b, c);
    }
}
