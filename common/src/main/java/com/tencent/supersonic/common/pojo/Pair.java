package com.tencent.supersonic.common.pojo;

public final class Pair<T, U> {

    public final T first;
    public final U second;

    public Pair(T first, U second) {
        this.second = second;
        this.first = first;
    }

    // Because 'pair()' is shorter than 'new Pair<>()'.
    // Sometimes this difference might be very significant (especially in a
    // 80-ish characters boundary). Sorry diamond operator.
    public static <T, U> Pair<T, U> pair(T first, U second) {
        return new Pair<>(first, second);
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }
}
